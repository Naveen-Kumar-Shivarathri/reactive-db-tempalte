package com.oneentropy.reactive.template.conf;

import com.oneentropy.reactive.template.model.ConnectionsCache;
import com.oneentropy.reactive.template.util.TemplateUtil;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;
import java.util.Map;

@Configuration
@ComponentScan(basePackages = {"com.oneentropy.reactive.template"})
@Slf4j
public class ConnectionsConfig {

    @Autowired
    DatabaseProperties databaseProperties;

    @Bean
    public ConnectionsCache createConnectionsCache() {
        ConnectionsCache connectionsCache = new ConnectionsCache();
        if (!TemplateUtil.hasContent(databaseProperties.getConnections())) {
            log.info("No Connections are specified.");
            return connectionsCache;
        }

        for (Map.Entry<String, DatabaseProperties.ConnectionDetails> entry : databaseProperties.getConnections().entrySet()) {
            HikariDataSource hikariDataSource = new HikariDataSource();
            hikariDataSource.setJdbcUrl(entry.getValue().getUrl());
            hikariDataSource.setUsername(entry.getValue().getUsername());
            hikariDataSource.setPassword(entry.getValue().getPassword());
            hikariDataSource.setDriverClassName(entry.getValue().getDriver());
            hikariDataSource.setPoolName(entry.getKey());
            hikariDataSource.setRegisterMbeans(true);
            hikariDataSource.setAutoCommit(false);
            //Warming up hikari CP
            try {
                hikariDataSource.getConnection().close();
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
            connectionsCache.put(entry.getKey(), hikariDataSource);
        }

        return connectionsCache;
    }


}
