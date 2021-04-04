package org.joq4j.redis.broker;

import org.joq4j.broker.Broker;
import org.joq4j.broker.Subscriber;
import org.joq4j.config.Config;
import org.joq4j.redis.connection.RedisConf;
import org.joq4j.redis.connection.RedisConnectionProviders;
import redis.clients.jedis.Jedis;

public class RedisBroker implements Broker {
    private Jedis jedis;

    public RedisBroker() {
        this.jedis = RedisConnectionProviders.getDefault(new RedisConf(new Config()));
    }

    public RedisBroker(String url) {
        this.jedis = RedisConnectionProviders.getDefault(new RedisConf(new Config(), url));
    }

    @Override
    public void subscribe(Subscriber subscriber, String... channels) {
    }

    @Override
    public void unsubscribe(Subscriber subscriber, String... channels) {

    }

    @Override
    public void publish(String channel, String message) {
        jedis.publish(channel, message);
    }

    @Override
    public void push(String queue, String... values) {
        jedis.lpush(queue, values);
    }

    @Override
    public String pop(String queue) {
        return jedis.rpop(queue);
    }

    @Override
    public void close() {
        jedis.close();
    }
}
