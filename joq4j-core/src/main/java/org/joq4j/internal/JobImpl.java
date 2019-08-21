package org.joq4j.internal;

import org.joq4j.AsyncTask;
import org.joq4j.Broker;
import org.joq4j.Job;
import org.joq4j.JobOptions;
import org.joq4j.JobQueue;
import org.joq4j.JobStatus;
import org.joq4j.RemoteExecutionException;
import org.joq4j.common.utils.DateTimes;
import org.joq4j.common.utils.Threads;
import org.joq4j.serde.Deserializer;
import org.joq4j.serde.JavaSerdeFactory;
import org.joq4j.serde.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class JobImpl implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobImpl.class);
    private static final String JOB_KEY_PREFIX = "jq:job:";

    static final String FIELD_STATUS = "status";
    static final String FIELD_QUEUED_AT = "queued_at";
    static final String FIELD_STARTED_AT = "started_at";
    static final String FIELD_FINISHED_AT = "finished_at";
    static final String FIELD_RESULT = "result";
    static final String FIELD_ERROR = "error";

    static final String FIELD_TASK = "task";
    static final String FIELD_WORKER_ID = "worker";

    static final String FIELD_DESCRIPTION = "description";
    static final String FIELD_TIMEOUT = "timeout";
    static final String FIELD_TTL = "ttl";
    static final String FIELD_RESULT_TTL = "result_ttl";
    static final String FIELD_FAILURE_TTL = "fail_ttl";

    private final String jobId;
    private final String jobKey;
    private AsyncTask task;

    private final JobOptions options;
    private final JobQueueImpl queue;
    private final Broker broker;
    private final Serializer serializer;
    private final Deserializer deserializer;

    JobImpl(JobQueue queue, JobOptions options) {
        this(queue, null, options);
    }

    JobImpl(JobQueue queue, AsyncTask task, JobOptions options) {
        this.task = task;
        this.options = options;

        if (!(queue instanceof JobQueueImpl)) {
            throw new IllegalArgumentException(
                    "Queue argument must be an instance of org.joq4j.core.JobQueueImpl");
        }
        this.queue = (JobQueueImpl) queue;
        this.broker = this.queue.getBroker();
        this.serializer = this.queue.getSerializer();
        this.deserializer = this.queue.getDeserializer();

        final String jobId = options.getJobId();
        if (jobId != null) {
            if (jobId.length() < 4 || jobId.length() > 128) {
                throw new IllegalArgumentException("Job ID length must be between 4 to 128 characters");
            }
            this.jobId = jobId;
        } else {
            this.jobId = generateId();
        }
        this.jobKey = generateJobKey(this.jobId);
        LOGGER.debug("New job has created: " + this.jobId);
    }

    public boolean cancel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return this.jobId;
    }

    @Override
    public JobOptions getOptions() {
        return this.options;
    }

    @Override
    public JobStatus getStatus() {
        try {
            String status = getField(FIELD_STATUS);
            return JobStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return JobStatus.UNKNOWN;
        }
    }

    @Override
    public String getOrigin() {
        return this.queue.getName();
    }

    @Override
    public Date getEnqueuedAt() {
        String fd = getField(FIELD_QUEUED_AT);
        return DateTimes.parseFromIsoFormat(fd);
    }

    @Override
    public Date getStartedAt() {
        String fd = getField(FIELD_STARTED_AT);
        return DateTimes.parseFromIsoFormat(fd);
    }

    @Override
    public Date getFinishedAt() {
        String fd = getField(FIELD_FINISHED_AT);
        return DateTimes.parseFromIsoFormat(fd);
    }

    @Override
    public String getWorkerId() {
        return getField(FIELD_WORKER_ID);
    }

    @Override
    public Throwable getError() throws IllegalStateException {
        if (!isDone()) {
            throw new IllegalStateException("Job is not finished");
        }
        return new RemoteExecutionException(getField(FIELD_ERROR));
    }

    @Override
    public Object getResult() throws IllegalStateException {
        if (!isDone()) {
            throw new IllegalStateException("Job is not finished");
        }
        String res = getField(FIELD_RESULT);
        return deserializer.deserializeFromBase64(res);
    }

    public void initNewJob() {
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put(FIELD_DESCRIPTION, options.getDescription());
        fieldMap.put(FIELD_TIMEOUT, String.valueOf(options.getJobTimeout()));
        fieldMap.put(FIELD_TTL, String.valueOf(options.getTtl()));
        fieldMap.put(FIELD_RESULT_TTL, String.valueOf(options.getResultTtl()));
        fieldMap.put(FIELD_FAILURE_TTL, String.valueOf(options.getFailureTtl()));
        fieldMap.put(FIELD_TASK, new JavaSerdeFactory().createSerializer().serializeAsBase64(task));

        setBatchFields(fieldMap);
        setStatus(JobStatus.QUEUED);
    }

    public void restoreFromBroker() {
        try {
            Map<String, String> fieldMap = getBatchFields();
            options.setDescription(fieldMap.get(FIELD_DESCRIPTION));
            options.setJobTimeout(Long.parseLong(fieldMap.get(FIELD_TIMEOUT)));
            options.setTtl(Long.parseLong(fieldMap.get(FIELD_TTL)));
            options.setResultTtl(Long.parseLong(fieldMap.get(FIELD_RESULT_TTL)));
            options.setFailureTtl(Long.parseLong(fieldMap.get(FIELD_FAILURE_TTL)));

            String serialized = fieldMap.get(FIELD_TASK);
            task = (AsyncTask) new JavaSerdeFactory().createDeserializer().deserializeFromBase64(serialized);
        } catch (NumberFormatException ignored) {}
    }

    public void setStatus(JobStatus status) {
        String dt = DateTimes.currentDateTimeAsIsoString();
        switch (status) {
            case QUEUED:
                setField(FIELD_QUEUED_AT, dt);
                break;
            case STARTED:
                setField(FIELD_STARTED_AT, dt);
                break;
            case FAILURE:
            case SUCCESS:
                setField(FIELD_FINISHED_AT, dt);
                break;
            default:
                break;
        }
        setField(FIELD_STATUS, status.getName());
    }

    public Object perform() {
        try {
            Object result = task.call();
            setField(FIELD_RESULT, serializer.serializeAsBase64(result));
            setStatus(JobStatus.SUCCESS);
            return result;
        } catch (Throwable t) {
            setField(FIELD_ERROR, t.getMessage());
            setStatus(JobStatus.FAILURE);
        }
        return null;
    }

    public Object waitForJobResult(long timeout) throws TimeoutException {
        long now;
        do {
            String value = getField(FIELD_RESULT);
            if (value != null) {
                return deserializer.deserializeFromBase64(value);
            }
            Threads.sleep(1);
            now = System.currentTimeMillis();
        } while (System.currentTimeMillis() - now > timeout || timeout == 0);
        throw new TimeoutException();
    }

    public void delete() {
        broker.removeMap(jobKey);
        setStatus(JobStatus.DELETED);
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    private String getField(String field) {
        return broker.getFromMap(jobKey, field);
    }

    private void setField(String field, String value) {
        broker.putToMap(jobKey, field, value);
    }

    private Map<String, String> getBatchFields() {
        return broker.getMap(jobKey);
    }

    private void setBatchFields(Map<String, String> fieldMap) {
        broker.putMap(jobKey, fieldMap);
    }

    static String generateJobKey(String jobId) {
        return JOB_KEY_PREFIX + jobId;
    }

    static JobImpl fetch(JobQueueImpl queue, String jobId) {
        JobImpl job = new JobImpl(queue, new JobOptions().setJobId(jobId));
        job.restoreFromBroker();
        return job;
    }

    static boolean checkExists(JobQueueImpl queue, String jobId) {
        String status = queue.getBroker().getFromMap(generateJobKey(jobId), FIELD_STATUS);
        return status != null && JobStatus.valueOf(status) != JobStatus.UNKNOWN;
    }

    static boolean checkAlive(JobQueueImpl queue, String jobId) {
        String status = queue.getBroker().getFromMap(generateJobKey(jobId), FIELD_STATUS);
        if (status != null) {
            JobStatus jobStatus = JobStatus.valueOf(status);
            return jobStatus != JobStatus.UNKNOWN && jobStatus != JobStatus.DELETED;
        }
        return false;
    }
}
