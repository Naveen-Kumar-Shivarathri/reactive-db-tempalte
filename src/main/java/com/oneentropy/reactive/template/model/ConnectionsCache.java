package com.oneentropy.reactive.template.model;

import com.zaxxer.hikari.HikariDataSource;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionsCache {

    private Map<String, HikariDataSource> connections;

    public boolean put(String connName, HikariDataSource dataSource){
        if(this.connections==null)
            connections = new HashMap<>();
        if(connections.get(connName)==null){
            connections.put(connName,dataSource);
            return true;
        }else{
            connections.replace(connName,dataSource);
            return false;
        }
    }

}
