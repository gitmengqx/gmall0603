package com.atguigu.gmall0603.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0603.bean.SkuLsInfo;
import com.atguigu.gmall0603.bean.SkuLsParams;
import com.atguigu.gmall0603.bean.SkuLsResult;
import com.atguigu.gmall0603.config.RedisUtil;
import com.atguigu.gmall0603.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {
    // 调用操作es的客户端对象
    @Autowired
    private JestClient jestClient;
    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {

        /*
        1.  保存的对象 PUT /Index/Type/Id {}
        2.  定义动作，并执行动作
         */

        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        /*
        1.  定义dsl 语句
        2.  定义动作
        3.  执行动作
        4.  获取返回的结果集
         */
        String query = makeQueryStringForSearch(skuLsParams);

        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult=null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 返回数据
        SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);

        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();

        // 定义key
        String hotKey = "hotScore";

        // 保存数据
        Double hotScore = jedis.zincrby(hotKey, 1, "skuId:" + skuId);

        if (hotScore%10==0){
            // 更新es
            updateHotScore(skuId,  Math.round(hotScore));
        }

    }

    /**
     * 更新es
     * @param skuId
     * @param hotScore
     */
    private void updateHotScore(String skuId, long hotScore) {
        /*
        1.  定义dsl 语句
        POST  gmall/SkuInfo/40/_update
            {
              "doc": {
               "hotScore": 20
              }
            }
           2.   定义动作并执行
         */

        String upd = "{\n" +
                "  \"doc\": {\n" +
                "   \"hotScore\": "+hotScore+"\n" +
                "  }\n" +
                "}";
        Update update = new Update.Builder(upd).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 制作返回结果集
     * @param searchResult
     * @param skuLsParams
     * @return
     */
    private SkuLsResult makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        SkuLsResult skuLsResult = new SkuLsResult();
        //        List<SkuLsInfo> skuLsInfoList;
        //      声明一个集合来存储SkuLsInfo
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        // 很简单：
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);

        // 判断集合
        if (hits!=null && hits.size()>0){
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                // 查询出来的skuName 并不是高亮对象！
                // 获取高亮
                if(hit.highlight!=null && hit.highlight.size()>0){
                    List<String> list = hit.highlight.get("skuName");
                    // 集合中只取第一个值
                    String skuNameHI = list.get(0);
                    skuLsInfo.setSkuName(skuNameHI);
                }
                // 添加到集合
                skuLsInfoArrayList.add(skuLsInfo);
            }
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
        //        long total;
        skuLsResult.setTotal(searchResult.getTotal());
        //        long totalPages;
        //  工作中：10 3 4 |
//        long totalPages = searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():searchResult.getTotal()/skuLsParams.getPageSize()+1;
        long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);
        //        List<String> attrValueIdList;
        // 声明一个集合来存储平台属性值Id
        ArrayList<String> stringArrayList = new ArrayList<>();

        // 通过聚合方式获取平台属性值Id
        MetricAggregation aggregations = searchResult.getAggregations();
        List<TermsAggregation.Entry> groupby_attr = aggregations.getTermsAggregation("groupby_attr").getBuckets();
        // 从桶中获取平台属性值Id，放入集合列表
        if (groupby_attr!=null && groupby_attr.size()>0){
            for (TermsAggregation.Entry entry : groupby_attr) {
                String valueId = entry.getKey();
                stringArrayList.add(valueId);
            }
        }
        // 找出平台属性值Id 放入该集合stringArrayList
        skuLsResult.setAttrValueIdList(stringArrayList);
        return skuLsResult;
    }

    /**
     * 定义dsl 语句
     * @param skuLsParams
     * @return
     */

    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // {} 查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // { bool } 查询器
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // { bool - filter} 过滤器
        // boolQueryBuilder.filter();

        // 判断三级分类Id
        // { bool - filter -- term } 过滤器
        // {"term": {"catalog3Id": "61"}}
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        // { bool - filter -- term } 过滤器
        //  {"term": {"skuAttrValueList.valueId": "83"}}
        // 平台属性值Id
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环遍历
            for (String valueId : skuLsParams.getValueId()) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // 判断商品名称 skuName
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // { bool - must -- match } 查询
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);

            // 设置高亮 highlight
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.field("skuName");
            highlightBuilder.postTags("</span>");
            // 将设置好的高亮对象放入到查询器中
            searchSourceBuilder.highlight(highlightBuilder);

        }

        // { query bool } 查询器
        searchSourceBuilder.query(boolQueryBuilder);

        // 排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        // 分页
        // 从第几条开始查询
        /*
           // 默认第一页
            int pageNo=1;
            // 每页显示的条数
            int pageSize=20;
         */
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        // 设置每页大小
        searchSourceBuilder.size(skuLsParams.getPageSize());

        // 聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        // 将类转换为字符串
        String query = searchSourceBuilder.toString();

        System.out.println("query:"+query);

        return query;
    }
}
