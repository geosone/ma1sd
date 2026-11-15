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
 * Configuration of the smstools connector: SMS Server Tools 3 behind smstools-http-api
 * (https://github.com/lvasiliev/smstools-http-api), which can run on another host.
 */
public class PhoneSmstoolsConfig {

    private transient final Logger log = LoggerFactory.getLogger(PhoneSmstoolsConfig.class);

    private String url = "";
    private String username = "";
    private String password = "";
    private String queue = "";
    private int timeout = 30;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isConfigured() {
        return StringUtils.isNotBlank(url);
    }

    public PhoneSmstoolsConfig build() {
        url = StringUtils.removeEnd(StringUtils.trimToEmpty(url), "/");
        if (timeout < 1) {
            timeout = 30;
        }

        log.info("--- Phone SMS smstools connector config ---");
        log.info("URL: {}", getUrl());
        log.info("Username: {}", StringUtils.defaultIfBlank(getUsername(), "(no authentication)"));
        log.info("Queue: {}", StringUtils.defaultIfBlank(getQueue(), "(default)"));
        log.info("Timeout: {}s", getTimeout());

        return this;
    }

}
