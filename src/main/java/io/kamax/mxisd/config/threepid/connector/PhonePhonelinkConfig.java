/*
 * ma1sd - Matrix Identity Server Daemon
 * Copyright (C) 2026 Mario Fetka
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.config.threepid.connector;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration of the phonelink connector: codes go to the send endpoint of the
 * gotify-phonelink plugin (https://github.com/geosone/gotify-phonelink), which pushes
 * them to the Gotify user who linked the number and falls back to SMS itself.
 */
public class PhonePhonelinkConfig {

    private transient final Logger log = LoggerFactory.getLogger(PhonePhonelinkConfig.class);

    // full URL of the plugin's send endpoint: https://gotify.example.org/plugin/<id>/custom/<token>/send
    private String url = "";
    // sender token from the plugin configuration
    private String token = "";
    private String title = "Matrix";
    private int priority = 8;
    // time until an unopened push is also sent by SMS ("2m"); empty = the sender's default
    private String failover = "";
    private int timeout = 30;
    // when Gotify cannot be reached: also try this connector (e.g. "smstools"); empty = none
    private String fallback = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getFailover() {
        return failover;
    }

    public void setFailover(String failover) {
        this.failover = failover;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getFallback() {
        return fallback;
    }

    public void setFallback(String fallback) {
        this.fallback = fallback;
    }

    public boolean isConfigured() {
        return StringUtils.isNoneBlank(url, token);
    }

    public PhonePhonelinkConfig build() {
        url = StringUtils.trimToEmpty(url);
        failover = StringUtils.trimToEmpty(failover);
        fallback = StringUtils.trimToEmpty(fallback);
        if (timeout < 1) {
            timeout = 30;
        }

        log.info("--- Phone phonelink connector config ---");
        log.info("Send endpoint: {}", StringUtils.isNotBlank(url) ? "set" : "missing");
        log.info("Sender token: {}", StringUtils.isNotBlank(token) ? "set" : "missing");
        log.info("Title: {}", getTitle());
        log.info("Priority: {}", getPriority());
        log.info("Failover: {}", StringUtils.defaultIfBlank(failover, "sender default"));
        log.info("Fallback connector: {}", StringUtils.defaultIfBlank(fallback, "none"));

        return this;
    }

}
