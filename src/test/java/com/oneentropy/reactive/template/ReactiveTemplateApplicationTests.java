package com.oneentropy.reactive.template;

import com.oneentropy.reactive.template.conf.ConnectionsConfig;
import com.oneentropy.reactive.template.conf.DatabaseProperties;
import com.oneentropy.reactive.template.model.ConnectionsCache;
import com.oneentropy.reactive.template.model.ExecutionData;
import com.oneentropy.reactive.template.model.ResponseResult;
import com.oneentropy.reactive.template.services.TransactionalQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = {ConnectionsConfig.class,DatabaseProperties.class})
@ActiveProfiles("test")
class ReactiveTemplateApplicationTests {

	@Autowired
	private DatabaseProperties databaseProperties;

	@Autowired
	private ConnectionsCache connectionsCache;

	@Autowired
	private TransactionalQueryService transactionalQueryService;

	@Test
	void testUpdateQueryCall() {

		String sql = "insert into tableA (columnA,columnB) values(?,?)";
		String deleteSql = "delete from tableA where columnA=?";
		String select = "select * from tableA where columnA=?";
		List<ExecutionData> executionDataList = new ArrayList<>();
		for(int iterator=0;iterator<100;iterator++){
			executionDataList.add(ExecutionData.builder().sql(select).params(new Object[]{iterator}).connName("mysql").build());
		}
		Mono<List<ResponseResult>> responseResultMono = transactionalQueryService.execute(executionDataList);
		StepVerifier.create(responseResultMono)
				.expectNextMatches(responseResult -> {
					return responseResult.stream().allMatch(responseResult1 -> {
						return responseResult1.getStatus().equals("SUCCESS");
					});
				})
				.verifyComplete();

	}

}
