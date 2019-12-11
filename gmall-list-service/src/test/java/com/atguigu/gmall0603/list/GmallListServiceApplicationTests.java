package com.atguigu.gmall0603.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

	@Autowired
	private JestClient jestClient;
	@Test
	public 	void contextLoads() {
	}

	// 连接elasticsearch ，并从es 中获取到数据
	@Test
	public void testES() throws IOException {
		/*
		1.	定义要执行的dsl 语句
			GET movie_chn/movie/_search
			{
			  "query": {
				"match": {
				  "actorList.name": "张译"
				}
			  }
			}
		2.	定义执行的动作
			GET movie_chn/movie/_search
		3.	执行动作
		4.	获取到执行之后的数据结果集

		 */
		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"match\": {\n" +
				"      \"actorList.name\": \"张译\"\n" +
				"    }\n" +
				"  }\n" +
				"}";

		// 定义执行的动作
		Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();

		// 执行 相当于在执行
		SearchResult searchResult = jestClient.execute(search);
		// 获取执行的结果
		List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);
		if (hits!=null && hits.size()>0){
			for (SearchResult.Hit<Map, Void> hit : hits) {
				Map map = hit.source;
				System.out.println(map.get("name"));
			}
		}


	}

}
