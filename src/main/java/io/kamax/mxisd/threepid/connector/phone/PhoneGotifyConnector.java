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
import io.kamax.mxisd.config.threepid.connector.PhoneGotifyConfig;
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
 * Pushes the SMS as a Gotify message (POST {url}/message, X-Gotify-Key application token).
 * Gotify itself sends no SMS: a forwarder subscribed to the application delivers it,
 * taking the recipient from the title or from extras "ma1sd::sms".recipient.
 */
public class PhoneGotifyConnector implements PhoneConnector {

    public static final String ID = "gotify";

    private transient final Logger log = LoggerFactory.getLogger(PhoneGotifyConnector.class);

    private final PhoneGotifyConfig cfg;

    public PhoneGotifyConnector(PhoneGotifyConfig cfg) {
        this.cfg = cfg.build();
        log.info("Gotify connector has been initiated");
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void send(String recipient, String content) {
        if (!cfg.isConfigured()) {
            log.error("Gotify connector is not configured: url and token are required.");
            throw new NotImplementedException("Phone numbers cannot be validated at this time. Contact your administrator.");
        }

        String msisdn = StringUtils.removeStart(StringUtils.trimToEmpty(recipient), "+");
        if (msisdn.isEmpty() || !StringUtils.isNumeric(msisdn)) {
            throw new InternalServerError("Invalid phone number for Gotify: " + recipient);
        }

        JsonObject sms = new JsonObject();
        sms.addProperty("recipient", "+" + msisdn);
        JsonObject extras = new JsonObject();
        extras.add("ma1sd::sms", sms);

        JsonObject body = new JsonObject();
        body.addProperty("title", cfg.getTitle().replace("%RECIPIENT%", msisdn));
        body.addProperty("message", content);
        body.addProperty("priority", cfg.getPriority());
        body.add("extras", extras);

        HttpPost req = new HttpPost(cfg.getUrl() + "/message");
        req.setHeader("X-Gotify-Key", cfg.getToken());
        req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        req.setEntity(new StringEntity(GsonUtil.get().toJson(body), ContentType.APPLICATION_JSON));
        int timeoutMs = cfg.getTimeout() * 1000;
        RequestConfig timeouts = RequestConfig.custom().setConnectTimeout(timeoutMs).setSocketTimeout(timeoutMs).build();

        log.info("Sending SMS notification to +{} with {} characters via Gotify", msisdn, content.length());
        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(timeouts).build();
             CloseableHttpResponse res = client.execute(req)) {
            int httpStatus = res.getStatusLine().getStatusCode();
            String resBody = res.getEntity() == null ? "" : EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8);
            if (httpStatus == 401 || httpStatus == 403) {
                log.error("Gotify rejected the application token (HTTP {})", httpStatus);
                throw new InternalServerError("Gotify rejected the application token");
            }
            if (httpStatus < 200 || httpStatus >= 300) {
                log.error("Gotify returned HTTP {}: {}", httpStatus, StringUtils.abbreviate(resBody, 300));
                throw new InternalServerError("Gotify returned HTTP " + httpStatus);
            }
            log.info("Message accepted by Gotify (ID {})",
                GsonUtil.findPrimitive(GsonUtil.parseObj(resBody), "id").map(p -> p.getAsString()).orElse("-"));
        } catch (IOException e) {
            log.error("Unable to reach Gotify at {}: {}", cfg.getUrl(), e.getMessage());
            throw new InternalServerError(e);
        }
    }

}
