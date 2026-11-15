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
 * Configuration of the websms.com (sms.at / LINK Mobility) connector.
 * Authenticate with an API access token (sent as Bearer) or with username and password
 * (sent through the official websms Java SDK).
 */
public class PhoneWebsmsConfig {

    private transient final Logger log = LoggerFactory.getLogger(PhoneWebsmsConfig.class);

    private String url = "https://api.websms.com";
    private String accessToken = "";
    private String username = "";
    private String password = "";
    private int maxSmsPerMessage = 1;
    private boolean test = false;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean hasAccessToken() {
        return StringUtils.isNotBlank(accessToken);
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

    public int getMaxSmsPerMessage() {
        return maxSmsPerMessage;
    }

    public void setMaxSmsPerMessage(int maxSmsPerMessage) {
        this.maxSmsPerMessage = maxSmsPerMessage;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isConfigured() {
        return StringUtils.isNotBlank(url)
            && (hasAccessToken() || (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)));
    }

    public PhoneWebsmsConfig build() {
        url = StringUtils.removeEnd(StringUtils.trimToEmpty(url), "/");
        if (maxSmsPerMessage < 1) {
            maxSmsPerMessage = 1;
        }

        log.info("--- Phone SMS websms connector config ---");
        log.info("URL: {}", getUrl());
        log.info("Authentication: {}", hasAccessToken() ? "access token" : "username " + getUsername());
        log.info("Max SMS per message: {}", getMaxSmsPerMessage());
        log.info("Test mode: {}", isTest());

        return this;
    }

}
