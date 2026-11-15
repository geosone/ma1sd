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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.threepid.connector.PhoneSmstoolsConfig;
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
import java.util.Base64;

/**
 * Sends SMS through SMS Server Tools 3 with smstools-http-api
 * (POST {url}/api/v1.0/sms/outgoing, HTTP basic authentication).
 */
public class PhoneSmsSmstoolsConnector implements PhoneConnector {

    public static final String ID = "smstools";

    private static final String OUTGOING_PATH = "/api/v1.0/sms/outgoing";

    private transient final Logger log = LoggerFactory.getLogger(PhoneSmsSmstoolsConnector.class);

    private final PhoneSmstoolsConfig cfg;

    public PhoneSmsSmstoolsConnector(PhoneSmstoolsConfig cfg) {
        this.cfg = cfg.build();
        log.info("smstools HTTP API connector has been initiated");
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void send(String recipient, String content) {
        if (!cfg.isConfigured()) {
            log.error("smstools connector is not configured: url is required.");
            throw new NotImplementedException("Phone numbers cannot be validated at this time. Contact your administrator.");
        }

        // smstools expects the international number without "+"
        String msisdn = StringUtils.removeStart(StringUtils.trimToEmpty(recipient), "+");
        if (msisdn.isEmpty() || !StringUtils.isNumeric(msisdn)) {
            throw new InternalServerError("Invalid phone number for smstools: " + recipient);
        }

        JsonArray mobiles = new JsonArray();
        mobiles.add(msisdn);
        JsonObject body = new JsonObject();
        body.add("mobiles", mobiles);
        body.addProperty("text", content);
        if (StringUtils.isNotBlank(cfg.getQueue())) {
            body.addProperty("queue", cfg.getQueue());
        }

        HttpPost req = new HttpPost(cfg.getUrl() + OUTGOING_PATH);
        if (StringUtils.isNotBlank(cfg.getUsername())) {
            String credentials = cfg.getUsername() + ":" + cfg.getPassword();
            req.setHeader(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        req.setEntity(new StringEntity(GsonUtil.get().toJson(body), ContentType.APPLICATION_JSON));
        int timeoutMs = cfg.getTimeout() * 1000;
        RequestConfig timeouts = RequestConfig.custom().setConnectTimeout(timeoutMs).setSocketTimeout(timeoutMs).build();

        log.info("Sending SMS notification to +{} with {} characters via smstools", msisdn, content.length());
        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(timeouts).build();
             CloseableHttpResponse res = client.execute(req)) {
            int httpStatus = res.getStatusLine().getStatusCode();
            String resBody = res.getEntity() == null ? "" : EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8);
            if (httpStatus == 401 || httpStatus == 403) {
                log.error("smstools API rejected the credentials (HTTP {})", httpStatus);
                throw new InternalServerError("smstools API rejected the credentials");
            }
            if (httpStatus < 200 || httpStatus >= 300) {
                log.error("smstools API returned HTTP {}: {}", httpStatus, StringUtils.abbreviate(resBody, 300));
                throw new InternalServerError("smstools API returned HTTP " + httpStatus);
            }

            // {"mobiles": {"<number>": {"message_id": "...", "response": "Ok"}}, "parts_count": n, ...}
            JsonObject result = GsonUtil.parseObj(resBody);
            JsonObject perNumber = GsonUtil.findObj(result, "mobiles")
                .flatMap(m -> GsonUtil.findObj(m, msisdn))
                .orElseThrow(() -> new InternalServerError("smstools API answer has no result for the recipient"));
            String response = GsonUtil.findString(perNumber, "response").orElse("");
            if (!StringUtils.equalsIgnoreCase("Ok", response)) {
                log.error("smstools refused the SMS: {}", response);
                throw new InternalServerError("smstools refused the SMS: " + response);
            }
            log.info("SMS queued by smstools (message ID {}, {} part(s))",
                GsonUtil.findString(perNumber, "message_id").orElse("-"),
                GsonUtil.findPrimitive(result, "parts_count").map(JsonElement::getAsString).orElse("?"));
        } catch (IOException e) {
            log.error("Unable to reach the smstools API at {}: {}", cfg.getUrl(), e.getMessage());
            throw new InternalServerError(e);
        }
    }

}
