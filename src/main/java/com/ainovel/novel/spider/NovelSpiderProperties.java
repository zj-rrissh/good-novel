package com.ainovel.novel.spider;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ainovel.spider")
public class NovelSpiderProperties {

    private boolean enabled = false;
    private String specFile;
    private Long defaultAuthorId = 1002L;
    private Duration requestTimeout = Duration.ofSeconds(10);
    private String userAgent = "AInovelNovelSpider/1.0";
    private boolean continueOnError = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSpecFile() {
        return specFile;
    }

    public void setSpecFile(String specFile) {
        this.specFile = specFile;
    }

    public Long getDefaultAuthorId() {
        return defaultAuthorId;
    }

    public void setDefaultAuthorId(Long defaultAuthorId) {
        this.defaultAuthorId = defaultAuthorId;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }
}
