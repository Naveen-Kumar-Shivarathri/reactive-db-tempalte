package com.oneentropy.reactive.template.services.impl;

import com.oneentropy.reactive.template.model.ConnectionsCache;
import com.oneentropy.reactive.template.model.ExecutionData;
import com.oneentropy.reactive.template.model.ResponseResult;
import com.oneentropy.reactive.template.services.TransactionalQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class TransactionalQueryServiceImpl implements TransactionalQueryService {

    @Autowired
    private ConnectionsCache connectionsCache;

    @Override
    public Mono<ResponseResult> executeUpdate(ExecutionData executionData) {
        QueryExecutor queryExecutor = new QueryExecutor(connectionsCache);
        return queryExecutor.executeUpdate(executionData);
    }

    @Override
    public Mono<List<ResponseResult>> executeUpdate(List<ExecutionData> executionDataList,boolean sync) {
        QueryExecutor queryExecutor = new QueryExecutor(connectionsCache);
        return queryExecutor.executeUpdate(executionDataList,sync);
    }

    @Override
    public Mono<ResponseResult> execute(ExecutionData executionData) {
        QueryExecutor queryExecutor = new QueryExecutor(connectionsCache);
        return queryExecutor.execute(executionData);
    }

    @Override
    public Mono<List<ResponseResult>> execute(List<ExecutionData> executionDataList,boolean sync) {
        QueryExecutor queryExecutor = new QueryExecutor(connectionsCache);
        return queryExecutor.execute(executionDataList,sync);
    }



}
