package com.oneentropy.reactive.template.model;

import com.oneentropy.reactive.template.util.TemplateUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class RunningConnections {

    private Map<String, Map<String, ConnectionInfo>> activeConnections;
    private String sessionId;

    public void put(String connName, String UUID, Connection connection){
        if(activeConnections==null)
            activeConnections = new ConcurrentHashMap<>();
        if(activeConnections.get(connName)==null)
            activeConnections.put(connName,new ConcurrentHashMap<>());
        ConnectionInfo connectionInfo = ConnectionInfo.builder().connection(connection).status(ConnectionInfo.IDLE).build();
        activeConnections.get(connName).put(UUID, connectionInfo);
    }

    public void updateStatus(String connName, String UUID, String status){
        if(activeConnections==null){
            log.error("Cannot update status, Active Connections are not yet populated");
            return;
        }
        if(activeConnections.get(connName)==null){
            log.error("Cannot update status, as no connections are pooled for connection name:{}",connName);
            return;
        }

        for(Map.Entry<String, ConnectionInfo> connectionInfoEntry: activeConnections.get(connName).entrySet()){
            if(connectionInfoEntry.getKey().equals(UUID)) {
                connectionInfoEntry.getValue().setStatus(status);
                break;
            }
        }
    }

    public Connection getIdleConnection(String connName){
        if(activeConnections==null){
            log.error("Cannot retrieve Connection, Active Connections are not yet populated");
            return null;
        }
        if(activeConnections.get(connName)==null){
            log.error("Cannot retrieve Connection, as no connections are pooled for connection name:{}",connName);
            return null;
        }
        for(Map.Entry<String, ConnectionInfo> connectionInfoEntry: activeConnections.get(connName).entrySet()){
            if(connectionInfoEntry.getValue().getStatus()==ConnectionInfo.IDLE) {
                log.debug("Reusing connection:{} on connName:{}",connectionInfoEntry.getKey(),connName);
                return connectionInfoEntry.getValue().getConnection();
            }
        }
        return null;
    }

    public void commit(){
        if(activeConnections==null){
            log.error("Cannot commit, Active Connections are not yet populated");
        }
        if(!TemplateUtil.hasContent(activeConnections)){
            log.error("Cannot commit, as no connections are pooled");
        }
        if(this.sessionId==null){
            log.error("Cannot commit, as there is no session defined");
            return;
        }
        for(Map.Entry<String, Map<String, ConnectionInfo>> connection: activeConnections.entrySet()){

            for(Map.Entry<String, ConnectionInfo> connectionInfoEntry: connection.getValue().entrySet()){
                try {
                    connectionInfoEntry.getValue().getConnection().commit();
                } catch (SQLException e) {
                    log.error("Encountered exception while committing the transaction on connection:{}, error:{}",connectionInfoEntry.getKey(), e.getMessage());
                }
            }

        }
    }

    public void rollback(){
        if(activeConnections==null){
            log.error("Cannot rollback, Active Connections are not yet populated");
        }
        if(!TemplateUtil.hasContent(activeConnections)){
            log.error("Cannot rollback, as no connections are pooled");
        }
        for(Map.Entry<String, Map<String, ConnectionInfo>> connection: activeConnections.entrySet()){

            for(Map.Entry<String, ConnectionInfo> connectionInfoEntry: connection.getValue().entrySet()){
                try {
                    connectionInfoEntry.getValue().getConnection().rollback();
                } catch (SQLException e) {
                    log.error("Encountered exception while rolling-back the transaction on connection:{}, error:{}",connectionInfoEntry.getKey(), e.getMessage());
                }
            }

        }
    }

    public void commit(String uuid){
        if(activeConnections==null){
            log.error("Cannot commit, Active Connections are not yet populated");
        }
        if(!TemplateUtil.hasContent(activeConnections)){
            log.error("Cannot commit, as no connections are pooled");
        }
        for(Map.Entry<String, Map<String, ConnectionInfo>> connection: activeConnections.entrySet()){

            for(Map.Entry<String, ConnectionInfo> connectionInfoEntry: connection.getValue().entrySet()){
                try {
                    connectionInfoEntry.getValue().getConnection().commit();
                } catch (SQLException e) {
                    log.error("Encountered exception while committing the transaction on connection:{}, error:{}",connectionInfoEntry.getKey(), e.getMessage());
                }
            }

        }
    }

    public void rollback(String uuid){
        if(activeConnections==null){
            log.error("Cannot rollback, Active Connections are not yet populated");
        }
        if(!TemplateUtil.hasContent(activeConnections)){
            log.error("Cannot rollback, as no connections are pooled");
        }
        for(Map.Entry<String, Map<String, ConnectionInfo>> connection: activeConnections.entrySet()){

            for(Map.Entry<String, ConnectionInfo> connectionInfoEntry: connection.getValue().entrySet()){
                try {
                    Savepoint savepoint = connectionInfoEntry.getValue().getSavePoint();
                    connectionInfoEntry.getValue().getConnection().rollback(savepoint);
                } catch (SQLException e) {
                    log.error("Encountered exception while rolling-back the transaction on connection:{}, error:{}",connectionInfoEntry.getKey(), e.getMessage());
                }
            }

        }
    }

    public void returnConnectionsToPool(){
        if(activeConnections==null){
            log.error("Cannot return connections to pool, Active Connections are not yet populated");
        }
        if(!TemplateUtil.hasContent(activeConnections)){
            log.error("Cannot return connections to pool, as no connections are pooled");
        }
        for(Map.Entry<String, Map<String, ConnectionInfo>> connection: activeConnections.entrySet()){

            for(Map.Entry<String, ConnectionInfo> connectionInfoEntry: connection.getValue().entrySet()){
                try {
                    connectionInfoEntry.getValue().getConnection().close();
                } catch (SQLException e) {
                    log.error("Encountered exception while returning connection to pool on connection:{}, error:{}",connectionInfoEntry.getKey(), e.getMessage());
                }
            }

        }
    }

    public void createSavePoints(String uuid){
        if(activeConnections==null){
            log.error("Cannot create a Savepoint, Active Connections are not yet populated");
        }
        if(!TemplateUtil.hasContent(activeConnections)){
            log.error("Cannot create a SavePoint, as no connections are pooled");
        }
        for(Map.Entry<String, Map<String, ConnectionInfo>> connection: activeConnections.entrySet()){

            for(Map.Entry<String, ConnectionInfo> connectionInfoEntry: connection.getValue().entrySet()){
                try {
                    Savepoint savepoint = connectionInfoEntry.getValue().getConnection().setSavepoint(uuid);
                    connectionInfoEntry.getValue().setSavePoint(savepoint);
                } catch (SQLException e) {
                    log.error("Encountered exception while committing the transaction on connection:{}, error:{}",connectionInfoEntry.getKey(), e.getMessage());
                }
            }

        }

    }


}
