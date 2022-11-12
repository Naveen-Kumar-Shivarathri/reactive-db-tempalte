package com.oneentropy.reactive.template.services.impl;

import com.oneentropy.reactive.template.model.*;
import com.oneentropy.reactive.template.util.TemplateUtil;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Slf4j
public class QueryExecutor {

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILURE = "FAILURE";
    private static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    private RunningConnections runningConnections;
    private ConnectionsCache connectionsCache;

    public QueryExecutor(ConnectionsCache connectionsCache) {
        initConnectionCache();
        this.connectionsCache = connectionsCache;
    }

    public Template getConnection(String connName, String UUID, boolean namedParams) throws SQLException {
        if (!TemplateUtil.hasContent(connName) || connectionsCache.getConnections().get(connName) == null) {
            log.error("No connections found with Name:{}", connName);
            return null;
        }
        Connection connection = null;
        ObjectName poolName = null;
        try {
            poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + connName + ")");
        } catch (MalformedObjectNameException e) {
            log.error(e.getMessage());
        }
        HikariPoolMXBean poolProxy = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);
        if (poolProxy.getIdleConnections() != 0) {
            connection = connectionsCache.getConnections().get(connName).getConnection();
        } else {
            while (connection == null) {
                connection = runningConnections.getIdleConnection(connName);
            }
        }
        runningConnections.put(connName, UUID, connection);
        SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(connection, false);
        if (namedParams)
            return Template.builder().namedParameterJdbcTemplate(new NamedParameterJdbcTemplate(singleConnectionDataSource)).build();
        else return Template.builder().jdbcTemplate(new JdbcTemplate(singleConnectionDataSource)).build();


    }

    public void initConnectionCache() {

        if (runningConnections == null)
            runningConnections = new RunningConnections();
    }

    public Mono<ResponseResult> executeUpdate(ExecutionData executionData) {
        boolean namedParams = executionData.isNamedParamsExecution();
        try {
            String uuid = UUID.randomUUID().toString();
            Template templateHolder = null;
            if (namedParams)
                templateHolder = getConnection(executionData.getConnName(), uuid, true);
            else
                templateHolder = getConnection(executionData.getConnName(), uuid, false);
            Template template = templateHolder;
            runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.RUNNING);
            return Mono.defer(() -> {
                long startTime = System.currentTimeMillis();
                return Mono.defer(() -> {
                    int rowsAffected = -1;
                    if (namedParams)
                        rowsAffected = template.getNamedParameterJdbcTemplate().update(executionData.getSql(), executionData.getNamedParams());
                    else
                        rowsAffected = template.getJdbcTemplate().update(executionData.getSql(), executionData.getParams());
                    long endTime = System.currentTimeMillis();
                    runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.IDLE);
                    logSuccess(startTime, endTime, executionData.getSql(), rowsAffected);
                    runningConnections.commit();
                    runningConnections.returnConnectionsToPool();
                    return Mono.just(ResponseResult.builder().status(SUCCESS).rowsAffected(rowsAffected).build());
                }).onErrorResume(exception -> {
                    logFailure(startTime, System.currentTimeMillis(), executionData.getSql(), exception);
                    runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.IDLE);
                    runningConnections.rollback();
                    runningConnections.returnConnectionsToPool();
                    return handleException(exception);
                });
            }).subscribeOn(Schedulers.boundedElastic());
        } catch (SQLException e) {
            return handleException(e);
        }
    }

    public Mono<List<ResponseResult>> executeUpdate(List<ExecutionData> executionDataList, boolean sync) {
        Scheduler schedulerHolder = null;
        if (sync) {
            log.debug("Executing Queries in synchronized mode on a single thread");
            schedulerHolder = Schedulers.single();
        } else
            schedulerHolder = Schedulers.boundedElastic();
        Scheduler scheduler = schedulerHolder;
        return Flux.fromIterable(executionDataList).flatMap(executionData -> {
            boolean namedParams = executionData.isNamedParamsExecution();

            try {
                String uuid = UUID.randomUUID().toString();
                Template templateHolder = null;
                if (namedParams)
                    templateHolder = getConnection(executionData.getConnName(), uuid, true);
                else
                    templateHolder = getConnection(executionData.getConnName(), uuid, false);
                Template template = templateHolder;
                runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.RUNNING);
                return Mono.defer(() -> {
                    long startTime = System.currentTimeMillis();
                    return Mono.defer(() -> {
                        int rowsAffected = -1;
                        if (namedParams)
                            rowsAffected = template.getNamedParameterJdbcTemplate().update(executionData.getSql(), executionData.getNamedParams());
                        else
                            rowsAffected = template.getJdbcTemplate().update(executionData.getSql(), executionData.getParams());
                        long endTime = System.currentTimeMillis();
                        runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.IDLE);
                        logSuccess(startTime, endTime, executionData.getSql(), rowsAffected);
                        return Mono.just(ResponseResult.builder().status(SUCCESS).rowsAffected(rowsAffected).build());
                    }).onErrorResume(exception -> {
                        logFailure(startTime, System.currentTimeMillis(), executionData.getSql(), exception);
                        runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.IDLE);
                        return handleException(exception);
                    }).subscribeOn(scheduler);
                });
            } catch (SQLException e) {
                return handleException(e);
            }
        }).collectList().flatMap(responseResults -> {
            boolean rollback = false;
            for (ResponseResult responseResult : responseResults)
                if (responseResult.getStatus().equals(FAILURE)) {
                    rollback = true;
                    break;
                }
            if (rollback) runningConnections.rollback();
            else runningConnections.commit();
            runningConnections.returnConnectionsToPool();
            return Mono.just(responseResults);
        }).subscribeOn(scheduler);


    }

    public Mono<ResponseResult> execute(ExecutionData executionData) {
        boolean namedParams = executionData.isNamedParamsExecution();
        try {
            String uuid = UUID.randomUUID().toString();
            Template templateHolder = null;
            if (namedParams)
                templateHolder = getConnection(executionData.getConnName(), uuid, true);
            else
                templateHolder = getConnection(executionData.getConnName(), uuid, false);
            Template template = templateHolder;
            runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.RUNNING);
            return Mono.defer(() -> {
                        long startTime = System.currentTimeMillis();
                        return Mono.defer(() -> {
                            Object payload = null;
                            if (namedParams)
                                payload = template.getNamedParameterJdbcTemplate().query(executionData.getSql(), executionData.getNamedParams(), (rs, rowNum) -> mapRow(rs, rowNum));
                            else
                                payload = template.getJdbcTemplate().query(executionData.getSql(), (rs, rowNum) -> mapRow(rs, rowNum), executionData.getParams());
                            long endTime = System.currentTimeMillis();
                            logSuccess(startTime, endTime, executionData.getSql(), payload.toString());
                            runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.IDLE);
                            runningConnections.commit();
                            runningConnections.returnConnectionsToPool();
                            return Mono.just(ResponseResult.builder().status(SUCCESS).payload(payload).build());
                        }).onErrorResume(exception -> {
                            logFailure(startTime, System.currentTimeMillis(), executionData.getSql(), exception);
                            runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.IDLE);
                            runningConnections.rollback();
                            runningConnections.returnConnectionsToPool();
                            return handleException(exception);
                        });
                    })
                    .subscribeOn(Schedulers.boundedElastic());
        } catch (SQLException e) {
            return handleException(e);
        }
    }

    public Mono<List<ResponseResult>> execute(List<ExecutionData> executionDataList, boolean sync) {
        Scheduler schedulerHolder = null;
        if (sync) {
            log.debug("Executing Queries in synchronized mode on a single thread");
            schedulerHolder = Schedulers.single();
        } else
            schedulerHolder = Schedulers.boundedElastic();
        Scheduler scheduler = schedulerHolder;
        return Flux.fromIterable(executionDataList).flatMap(executionData -> {
                    boolean namedParams = executionData.isNamedParamsExecution();
                    try {
                        String uuid = UUID.randomUUID().toString();
                        Template templateHolder = null;
                        if (namedParams)
                            templateHolder = getConnection(executionData.getConnName(), uuid, true);
                        else
                            templateHolder = getConnection(executionData.getConnName(), uuid, false);
                        Template template = templateHolder;
                        runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.RUNNING);
                        return Mono.defer(() -> {
                            long startTime = System.currentTimeMillis();
                            return Mono.defer(() -> {
                                Object payload = null;
                                if (namedParams)
                                    payload = template.getNamedParameterJdbcTemplate().query(executionData.getSql(), executionData.getNamedParams(), (rs, rowNum) -> mapRow(rs, rowNum));
                                else
                                    payload = template.getJdbcTemplate().query(executionData.getSql(), (rs, rowNum) -> mapRow(rs, rowNum), executionData.getParams());
                                long endTime = System.currentTimeMillis();
                                runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.IDLE);
                                logSuccess(startTime, endTime, executionData.getSql(), payload.toString());
                                return Mono.just(ResponseResult.builder().status(SUCCESS).payload(payload).build());
                            }).onErrorResume(exception -> {
                                logFailure(startTime, System.currentTimeMillis(), executionData.getSql(), exception);
                                runningConnections.updateStatus(executionData.getConnName(), uuid, ConnectionInfo.IDLE);
                                return handleException(exception);
                            }).subscribeOn(scheduler);
                        });
                    } catch (SQLException e) {
                        return handleException(e);
                    }
                }).collectList().flatMap(responseResults -> {
                    boolean rollback = false;
                    for (ResponseResult responseResult : responseResults)
                        if (responseResult.getStatus().equals(FAILURE)) {
                            rollback = true;
                            break;
                        }
                    if (rollback) runningConnections.rollback();
                    else runningConnections.commit();
                    runningConnections.returnConnectionsToPool();
                    return Mono.just(responseResults);
                })
                .subscribeOn(scheduler);


    }

    private static Mono<ResponseResult> handleException(Throwable exception) {
        return Mono.just(ResponseResult.builder().status(FAILURE).message(exception.getMessage()).build());
    }

    private static void logSuccess(long startTime, long endTime, String sql, String payload) {
        log.info("Query-Execution: Status-{}, TimeTaken-{}ms, SQL- [{}], payload-{}", SUCCESS, endTime - startTime, sql, payload);
    }

    private static void logSuccess(long startTime, long endTime, String sql, int rowsAffected) {
        log.info("Query-Execution: Status-{}, TimeTaken-{}ms, SQL- [{}], rowsAffected-{}", SUCCESS, endTime - startTime, sql, rowsAffected);
    }

    private static void logFailure(long startTime, long endTime, String sql, Throwable exception) {
        log.info("Query-Execution: Status-{}, TimeTaken-{}ms, SQL- [{}], payload-{}", FAILURE, endTime - startTime, sql, exception.getMessage());
    }

    private static Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        int totalRows = rs.getMetaData().getColumnCount();
        JSONObject obj = new JSONObject();
        for (int columnIterator = 0; columnIterator < totalRows; columnIterator++) {
            if (rs.getMetaData().getColumnType(columnIterator + 1) == 2005) {
                obj.put(rs.getMetaData().getColumnLabel(columnIterator + 1).toLowerCase(), handleClob(rs.getClob(columnIterator + 1)));
            } else {
                obj.put(rs.getMetaData().getColumnLabel(columnIterator + 1).toLowerCase(), rs.getObject(columnIterator + 1));
            }
        }

        return obj;
    }

    private static String handleClob(Clob clobData) {
        if (clobData == null)
            return null;

        try (Reader reader = clobData.getCharacterStream()) {
            if (reader == null)
                return null;
            char[] buffer = new char[(int) clobData.length()];
            if (buffer.length == 0)
                return null;
            reader.read(buffer);
            return new String(buffer);
        } catch (SQLException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }


}
