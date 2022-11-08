package com.oneentropy.reactive.template.model;

import lombok.*;

import java.sql.Connection;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionInfo {

    public static final String RUNNING = "RUNNING";
    public static final String IDLE = "IDLE";

    private Connection connection;
    private String status;

}
