package com.kirandev.sharedlogginglib;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;

/**
 * Seeds static MDC fields for batch and messaging services that do not pass
 * through {@link MdcFilter}. Trace/span IDs are still supplied by the OTel MDC
 * bridge or inbound message headers at processing time.
 */
public class ServiceMdcInitializer implements ApplicationRunner {

    @Value("${spring.application.name:service}")
    private String serviceName;

    private final Environment environment;

    public ServiceMdcInitializer(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        MDC.put(LoggingMdc.SERVICE_ID, serviceName);
        String deploymentEnv = MdcContext.firstNonBlank(
                environment.getProperty("DEPLOYMENT_ENVIRONMENT"),
                String.join(",", environment.getActiveProfiles()));
        if (deploymentEnv != null) {
            MDC.put("deployment.environment", deploymentEnv);
        }
    }
}
