package org.joq4j.redis.connection;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

public class RedisConnectionProviders {
    private static Map<String, JedisPool> clients = new TreeMap<>();

    public static synchronized Jedis getOrCreate(String name, RedisConf redisConf) {
        JedisPool jedisPool = clients.computeIfAbsent(name, k -> init(redisConf));

        return jedisPool.getResource();
    }

    public static synchronized Jedis getDefault(RedisConf redisConf) {
        JedisPool jedisPool = clients.computeIfAbsent("default", k -> init(redisConf));

        return jedisPool.getResource();
    }

    private static JedisPool init(RedisConf redisConfig) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(redisConfig.poolSize());
        jedisPoolConfig.setMaxIdle(redisConfig.maxIdle());
        jedisPoolConfig.setMinIdle(redisConfig.minIdle());
        jedisPoolConfig.setTestOnBorrow(redisConfig.testOnBorrow());
        jedisPoolConfig.setMinEvictableIdleTimeMillis(redisConfig.minEvictableIdleMillis());
        jedisPoolConfig.setBlockWhenExhausted(redisConfig.blockWhenExhausted());

        return new JedisPool(jedisPoolConfig, URI.create(redisConfig.url()));
    }

    public static void shutdownPools() {
        clients.forEach((key, client) -> client.close());
    }
}
