package com.oneentropy.reactive.template.model;

import lombok.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public boolean isJdbcTemplate(){
        return jdbcTemplate==null;
    }
}
