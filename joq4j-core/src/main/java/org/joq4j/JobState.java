package org.joq4j;

import lombok.Getter;

import java.io.Serializable;

public enum JobState implements Serializable {

    CREATED("created"),
    QUEUED("queued"),
    DEQUEUE("dequeue"),
    PENDING("pending"),
    RUNNING("running"),
    SUCCESS("success"),
    FAILURE("failure"),
    RETRY("retry"),
    CANCELLED("cancelled");

    @Getter
    private String name;

    JobState(String name) {
        this.name = name.toUpperCase();
    }
}
