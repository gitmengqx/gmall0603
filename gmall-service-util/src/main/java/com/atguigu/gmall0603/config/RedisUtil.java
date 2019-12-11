package com.atguigu.gmall0603.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
    // 创建一个连接池工厂
    private JedisPool jedisPool ;

    // 初始化连接池工厂
    public void initJedisPool(String host,int port,int timeOut){
        // 配置连接池工厂参数
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 设置参数 表示获取到连接之后启动检查
        jedisPoolConfig.setTestOnBorrow(true);
        // 当连接到达最大时，等待
        jedisPoolConfig.setBlockWhenExhausted(true);
        // 设置最大核心数
        jedisPoolConfig.setMaxTotal(200);
        // 最小剩余数
        jedisPoolConfig.setMinIdle(10);
        // 设置等待实际
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        jedisPool = new JedisPool(jedisPoolConfig,host,port,timeOut);
    }
    // 获取Jedis
    public Jedis getJedis(){
//        Jedis jedis = new Jedis("192.168.67.224",6379);
        return jedisPool.getResource();
    }


}
