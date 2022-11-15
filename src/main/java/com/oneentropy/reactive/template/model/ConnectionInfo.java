package com.oneentropy.reactive.template.model;

import lombok.*;

import java.sql.Connection;
import java.sql.Savepoint;

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
    private Savepoint savePoint;

}
