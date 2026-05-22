package com.kirandev.sharedlogginglib;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Snapshot MDC from the submitting thread (could be virtual or platform)
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (callerMdc != null && !callerMdc.isEmpty()) {
                    MDC.setContextMap(callerMdc);
                }
                runnable.run();
            } finally {
                MDC.clear();  // virtual threads are discarded but child scopes need this
            }
        };
    }
}
