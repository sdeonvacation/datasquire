package io.datasquire.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the DataSquire starter.
 */
@ConfigurationProperties(prefix = "datasquire")
public class DataSquireProperties {

    private boolean enabled = true;
    private Session session = new Session();
    private Controller controller = new Controller();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Controller getController() {
        return controller;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public static class Session {
        private int maxSize = 1000;
        private int ttlMinutes = 30;
        private int maxHistorySize = 20;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }

        public int getMaxHistorySize() {
            return maxHistorySize;
        }

        public void setMaxHistorySize(int maxHistorySize) {
            this.maxHistorySize = maxHistorySize;
        }
    }

    public static class Controller {
        private boolean enabled = true;
        private String basePath = "/api";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }
}
