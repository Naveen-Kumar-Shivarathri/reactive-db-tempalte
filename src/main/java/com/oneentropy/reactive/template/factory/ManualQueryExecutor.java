package com.oneentropy.reactive.template.factory;

import com.oneentropy.reactive.template.exceptions.NoDBSessionFoundException;
import com.oneentropy.reactive.template.model.ConnectionInfo;
import com.oneentropy.reactive.template.model.ExecutionData;
import com.oneentropy.reactive.template.model.ResponseResult;
import com.oneentropy.reactive.template.model.Template;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface ManualQueryExecutor {

    void begin();

    void commit();

    void rollback();

    Map<String, Map<String, ConnectionInfo>> getConnectionsInfo();

    Template getConnection(String connName, String UUID, boolean namedParams) throws SQLException;

    void initConnectionCache();

    Mono<ResponseResult> executeUpdate(ExecutionData executionData) throws NoDBSessionFoundException;

    Mono<List<ResponseResult>> executeUpdate(List<ExecutionData> executionDataList, boolean sync) throws NoDBSessionFoundException;

    Mono<ResponseResult> execute(ExecutionData executionData) throws NoDBSessionFoundException;

    Mono<List<ResponseResult>> execute(List<ExecutionData> executionDataList, boolean sync) throws NoDBSessionFoundException;


}
