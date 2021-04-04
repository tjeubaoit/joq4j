package org.joq4j.examples;

import org.joq4j.*;
import org.joq4j.backend.StorageBackend;
import org.joq4j.broker.Broker;
import org.joq4j.redis.backend.RedisBackend;
import org.joq4j.redis.broker.RedisBroker;

public class RedisPlatformApp {

    public static void main(String[] args) throws Exception {
        Broker broker = new RedisBroker("redis://localhost:6379/0");
        StorageBackend backend = new RedisBackend("redis://localhost:6379/0");
        JobQueue queue = JobQueue.builder()
                .broker(broker)
                .backend(backend)
                .build();
        Task task = (Task) () -> "Hello, World!";
        AsyncResult result = queue.enqueue(task);
        while (!result.isDone()) {
            Thread.sleep(1000);
            System.out.println("Job State:"+ backend.getState(result.getJobId()));
        }
        System.out.println(result.get());
    }
}
