package com.atguigu.gmall0603.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 表示配置文件 redisConfig.xml
public class RedisConfig {

    /*
    第一点：获取host,port,timeOut 的值 从配置文件中获取！
    第二点：初始化initJedisPool
    第三点：将RedisUtil 放入spring 容器中
     */
    // 表示如果没有host配置，则host给默认值 disable
    // 当前类必须在spring 容器中！
    @Value("${spring.redis.host:disable}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.timeOut:10000}")
    private int timeOut;
    /*
    <bean id="redisUtil" class="com.atguigu.gmall0603.config.RedisUtil">
    </bean>
     */
    // 将RedisUtil 封装到spring 容器中！
    @Bean
    public RedisUtil getRedisUtil(){
        if ("disable".equals(host)){
            return null;
        }
        RedisUtil redisUtil = new RedisUtil();
        // 初始化initJedisPool
        redisUtil.initJedisPool(host,port,timeOut);
        return redisUtil;
    }



}
