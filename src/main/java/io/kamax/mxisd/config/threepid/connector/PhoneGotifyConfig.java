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
 * Configuration of the Gotify connector: the SMS is pushed as a Gotify message
 * (https://gotify.net), for a forwarder (e.g. an SMS gateway app subscribed to the
 * Gotify application) to deliver it. The recipient is in the title and in the extras.
 */
public class PhoneGotifyConfig {

    private transient final Logger log = LoggerFactory.getLogger(PhoneGotifyConfig.class);

    private String url = "";
    private String token = "";
    private int priority = 8;
    private String title = "SMS to +%RECIPIENT%";
    private int timeout = 30;

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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isConfigured() {
        return StringUtils.isNoneBlank(url, token);
    }

    public PhoneGotifyConfig build() {
        url = StringUtils.removeEnd(StringUtils.trimToEmpty(url), "/");
        if (timeout < 1) {
            timeout = 30;
        }

        log.info("--- Phone SMS Gotify connector config ---");
        log.info("URL: {}", getUrl());
        log.info("Application token: {}", StringUtils.isNotBlank(token) ? "set" : "missing");
        log.info("Priority: {}", getPriority());
        log.info("Title: {}", getTitle());

        return this;
    }

}
