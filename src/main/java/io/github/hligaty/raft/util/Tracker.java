package io.github.hligaty.raft.util;

import org.slf4j.MDC;

import java.util.UUID;

public final class Tracker {
    private static final String NAME = "traceId";
    
    public static void start() {
        MDC.put(NAME, UUID.randomUUID().toString());
    }

    public static void start(String traceId) {
        MDC.put(NAME, traceId);
    }
    
    public static void stop() {
        MDC.clear();
    }
    
    public static String getId() {
        return MDC.get(NAME);
    }
}
