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

package io.kamax.mxisd.threepid.connector.phone;


import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.threepid.connector.PhonePhonelinkConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.exception.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sends the code through the gotify-phonelink plugin (POST {url}, Bearer sender token):
 * a push to the Gotify user who linked the number, with SMS failover if it is not
 * opened in time, or an SMS at once for unlinked numbers (sender option sms_unlinked).
 * If the plugin cannot deliver (Gotify down, rejected token, ...), the optional
 * fallback connector sends the SMS directly.
 */
public class PhonePhonelinkConnector implements PhoneConnector {

    public static final String ID = "phonelink";

    private transient final Logger log = LoggerFactory.getLogger(PhonePhonelinkConnector.class);

    private final PhonePhonelinkConfig cfg;
    private final PhoneConnector fallback;

    public PhonePhonelinkConnector(PhonePhonelinkConfig cfg, PhoneConnector fallback) {
        this.cfg = cfg;
        this.fallback = fallback;
        log.info("phonelink connector has been initiated{}", fallback == null ? "" : " with fallback " + fallback.getId());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void send(String recipient, String content) {
        if (!cfg.isConfigured()) {
            log.error("phonelink connector is not configured: url and token are required.");
            throw new NotImplementedException("Phone numbers cannot be validated at this time. Contact your administrator.");
        }

        String msisdn = StringUtils.removeStart(StringUtils.trimToEmpty(recipient), "+");
        if (msisdn.isEmpty() || !StringUtils.isNumeric(msisdn)) {
            throw new InternalServerError("Invalid phone number for phonelink: " + recipient);
        }

        JsonObject body = new JsonObject();
        body.addProperty("to", "+" + msisdn);
        body.addProperty("title", cfg.getTitle());
        body.addProperty("message", content);
        body.addProperty("priority", cfg.getPriority());
        if (StringUtils.isNotBlank(cfg.getFailover())) {
            body.addProperty("failover", cfg.getFailover());
        }

        HttpPost req = new HttpPost(cfg.getUrl());
        req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.getToken());
        req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        req.setEntity(new StringEntity(GsonUtil.get().toJson(body), ContentType.APPLICATION_JSON));
        int timeoutMs = cfg.getTimeout() * 1000;
        RequestConfig timeouts = RequestConfig.custom().setConnectTimeout(timeoutMs).setSocketTimeout(timeoutMs).build();

        log.info("Sending code to +{} with {} characters via phonelink", msisdn, content.length());
        String failure;
        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(timeouts).build();
             CloseableHttpResponse res = client.execute(req)) {
            int httpStatus = res.getStatusLine().getStatusCode();
            String resBody = res.getEntity() == null ? "" : EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8);
            if (httpStatus >= 200 && httpStatus < 300) {
                JsonObject r = GsonUtil.parseObj(resBody);
                log.info("phonelink delivered the code by {}{}",
                    GsonUtil.findString(r, "status").orElse("?"),
                    GsonUtil.findString(r, "failover_in").map(f -> " (SMS failover in " + f + ")").orElse(""));
                return;
            }
            String error = StringUtils.abbreviate(GsonUtil.findString(GsonUtil.parseObj(resBody), "error").orElse(resBody), 300);
            if (httpStatus == 400) {
                log.error("phonelink refused the request: {}", error);
                throw new InternalServerError("phonelink refused the request: " + error);
            }
            failure = "HTTP " + httpStatus + ": " + error;
            if (httpStatus == 401 || httpStatus == 403) {
                log.error("phonelink rejected the sender token (HTTP {})", httpStatus);
            } else {
                log.warn("phonelink could not deliver the code: {}", failure);
            }
        } catch (IOException e) {
            failure = e.getMessage();
            log.warn("Unable to reach phonelink: {}", failure);
        } catch (RuntimeException e) {
            if (e instanceof InternalServerError) {
                throw e;
            }
            failure = e.getMessage();
            log.warn("Unexpected answer from phonelink: {}", failure);
        }

        if (fallback == null) {
            throw new InternalServerError("phonelink could not deliver the code: " + failure);
        }
        log.info("Sending the code to +{} via the fallback connector {}", msisdn, fallback.getId());
        fallback.send(recipient, content);
    }

}
