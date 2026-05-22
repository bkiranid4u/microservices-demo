package com.kirandev.sharedlogginglib;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kirandev.logging")
public class LoggingProperties {

    /**
     * When true, rolling file appenders are attached (useful on VMs; disable in
     * Docker/Kubernetes where stdout JSON is collected by the platform).
     */
    private boolean fileEnabled = false;

    /**
     * Emit JSON to stdout (recommended for Docker, Kubernetes, and log agents).
     */
    private boolean jsonConsole = true;

    /**
     * Populate service identity in MDC at startup for non-web applications.
     */
    private boolean initializeServiceContext = true;

    public boolean isFileEnabled() {
        return fileEnabled;
    }

    public void setFileEnabled(boolean fileEnabled) {
        this.fileEnabled = fileEnabled;
    }

    public boolean isJsonConsole() {
        return jsonConsole;
    }

    public void setJsonConsole(boolean jsonConsole) {
        this.jsonConsole = jsonConsole;
    }

    public boolean isInitializeServiceContext() {
        return initializeServiceContext;
    }

    public void setInitializeServiceContext(boolean initializeServiceContext) {
        this.initializeServiceContext = initializeServiceContext;
    }

    private Kafka kafka = new Kafka();

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public static class Kafka {
        /**
         * Enable Kafka MDC propagation (producer header injection and consumer header extraction).
         * Disable if you need to supply your own RecordInterceptor or ProducerInterceptor.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
