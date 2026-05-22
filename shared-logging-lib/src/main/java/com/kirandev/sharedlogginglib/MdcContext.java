package com.kirandev.sharedlogginglib;

import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Utilities for propagating correlation context in non-web workloads
 * (Kafka consumers, scheduled tasks, async executors).
 */
public final class MdcContext {

    private MdcContext() {
    }

    public static void put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    public static void putAll(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            return;
        }
        context.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                MDC.put(key, value);
            }
        });
    }

    public static Runnable wrap(Runnable runnable) {
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        return () -> runWithContext(callerMdc, runnable);
    }

    public static <T> Supplier<T> wrap(Supplier<T> supplier) {
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        return () -> callWithContext(callerMdc, supplier);
    }

    public static void runWithContext(Map<String, String> context, Runnable runnable) {
        callWithContext(context, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T callWithContext(Map<String, String> context, Supplier<T> supplier) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            if (context != null && !context.isEmpty()) {
                MDC.setContextMap(context);
            }
            return supplier.get();
        } finally {
            restore(previous);
        }
    }

    private static void restore(Map<String, String> previous) {
        if (previous == null || previous.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(previous);
        }
    }

    public static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    public static String requireNonBlank(String value, String name) {
        return Objects.requireNonNull(value, name + " must not be null");
    }
}
