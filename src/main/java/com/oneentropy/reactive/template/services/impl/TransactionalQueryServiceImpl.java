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
        AutoQueryExecutor autoQueryExecutor = new AutoQueryExecutor(connectionsCache);
        return autoQueryExecutor.executeUpdate(executionData);
    }

    @Override
    public Mono<List<ResponseResult>> executeUpdate(List<ExecutionData> executionDataList,boolean sync) {
        AutoQueryExecutor autoQueryExecutor = new AutoQueryExecutor(connectionsCache);
        return autoQueryExecutor.executeUpdate(executionDataList,sync);
    }

    @Override
    public Mono<ResponseResult> execute(ExecutionData executionData) {
        AutoQueryExecutor autoQueryExecutor = new AutoQueryExecutor(connectionsCache);
        return autoQueryExecutor.execute(executionData);
    }

    @Override
    public Mono<List<ResponseResult>> execute(List<ExecutionData> executionDataList,boolean sync) {
        AutoQueryExecutor autoQueryExecutor = new AutoQueryExecutor(connectionsCache);
        return autoQueryExecutor.execute(executionDataList,sync);
    }



}
