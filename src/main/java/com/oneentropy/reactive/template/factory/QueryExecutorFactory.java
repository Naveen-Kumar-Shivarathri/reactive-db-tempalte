package com.oneentropy.reactive.template.factory;

import com.oneentropy.reactive.template.model.ConnectionsCache;
import com.oneentropy.reactive.template.services.impl.ManualQueryExecutorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QueryExecutorFactory {

    @Autowired
    private ConnectionsCache connectionsCache;

    public ManualQueryExecutor createManualQueryExecutor(){
        return new ManualQueryExecutorImpl(connectionsCache);
    }

}
