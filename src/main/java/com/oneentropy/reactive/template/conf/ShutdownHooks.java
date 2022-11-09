package com.oneentropy.reactive.template.conf;

import com.oneentropy.reactive.template.model.ConnectionsCache;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;

@Component
public class ShutdownHooks {

    @Autowired
    private ConnectionsCache connectionsCache;

    @PreDestroy
    public void shutdownHikariCp(){
        for(Map.Entry<String, HikariDataSource> entry : connectionsCache.getConnections().entrySet()){
            entry.getValue().close();
        }
    }

}
