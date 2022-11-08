package com.oneentropy.reactive.template.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseResult {

    private String status;
    private Object payload;
    private String message;
    private int rowsAffected;

}
