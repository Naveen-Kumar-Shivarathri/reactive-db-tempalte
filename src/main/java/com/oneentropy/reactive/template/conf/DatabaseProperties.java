package com.oneentropy.reactive.template.conf;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@ConfigurationProperties(prefix = "oneentropy")
@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
@Getter
@Setter
@NoArgsConstructor
public class DatabaseProperties {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConnectionDetails{
        private String url;
        private String username;
        private String password;
        private String driver;
    }

    private Map<String, ConnectionDetails> connections;

}
