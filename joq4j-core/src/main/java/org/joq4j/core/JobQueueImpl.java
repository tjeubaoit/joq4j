package org.joq4j.core;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.joq4j.*;
import org.joq4j.backend.StorageBackend;
import org.joq4j.broker.Broker;
import org.joq4j.config.Config;
import org.joq4j.encoding.MessageEncoder;
import org.joq4j.encoding.TaskSerializer;

@Accessors(fluent = true)
@Getter
public class JobQueueImpl implements JobQueue {

    private static final String QUEUE_KEY_PREFIX = "jq:queue:";

    private final String name;
    private final String queueKey;
    private final Broker broker;
    private final StorageBackend backend;

    private final long defaultTimeout;
    private final MessageEncoder messageEncoder;
    private final TaskSerializer taskSerializer;

    public JobQueueImpl(String name, Broker broker, StorageBackend backend, Config conf) {
        this.name = name;
        this.broker = broker;
        this.backend = backend;
        this.queueKey = QUEUE_KEY_PREFIX + this.name;

        QueueOptions options = new QueueOptions();
        options.configure(conf);

        this.defaultTimeout = options.defaultTimeout();
        this.messageEncoder = options.messageEncoder();
        this.taskSerializer = options.taskSerializer();
    }

    @Override
    public AsyncResult enqueue(Task task) {
        return enqueue(task, new TaskOptions().timeout(this.defaultTimeout));
    }

    @Override
    public AsyncResult enqueue(Task task, TaskOptions options) {
        JobImpl job = new JobImpl(this.name, task, options);
        broker.push(queueKey, messageEncoder.writeAsBase64(job));

        backend.storeJob(job);
        backend.setState(job.id(), JobState.QUEUED);

        return new AsyncResultImpl(backend, job);
    }

    @Override
    public Job nextJob(String worker) {
        return messageEncoder.readFromBase64(broker.pop(queueKey));
    }
}
