package com.oneentropy.reactive.template.services;

import com.oneentropy.reactive.template.model.ExecutionData;
import com.oneentropy.reactive.template.model.ResponseResult;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TransactionalQueryService {

    Mono<ResponseResult> executeUpdate(ExecutionData executionData);

    Mono<List<ResponseResult>> executeUpdate(List<ExecutionData> executionDataList);

    Mono<ResponseResult> execute(ExecutionData executionData);

    Mono<List<ResponseResult>> execute(List<ExecutionData> executionDataList);

}
