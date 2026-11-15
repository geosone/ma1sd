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
import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.threepid.connector.PhoneWebsmsConfig;
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
 * Sends SMS through the websms.com REST API (POST /json/smsmessaging/text),
 * the same call the MIT-licensed websms Java SDK
 * (https://github.com/websms-com/websmscom-java) makes.
 */
public class PhoneSmsWebsmsConnector implements PhoneConnector {

    public static final String ID = "websms";

    private static final String TEXT_PATH = "/json/smsmessaging/text";
    // 2000 = OK, 2001 = OK (queued)
    private static final int STATUS_OK = 2000;
    private static final int STATUS_QUEUED = 2001;

    private transient final Logger log = LoggerFactory.getLogger(PhoneSmsWebsmsConnector.class);

    private final PhoneWebsmsConfig cfg;
    private final CloseableHttpClient client;

    public PhoneSmsWebsmsConnector(PhoneWebsmsConfig cfg) {
        this.cfg = cfg.build();
        RequestConfig timeouts = RequestConfig.custom()
            .setConnectTimeout(10_000)
            .setSocketTimeout(30_000)
            .build();
        this.client = HttpClients.custom().setDefaultRequestConfig(timeouts).build();
        log.info("websms API has been initiated");
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void send(String recipient, String content) {
        if (!cfg.isConfigured()) {
            log.error("websms connector is not fully configured: an access token or username and password are required.");
            throw new NotImplementedException("Phone numbers cannot be validated at this time. Contact your administrator.");
        }

        // ma1sd passes the MSISDN without "+"; websms expects the digits only
        String msisdn = StringUtils.removeStart(StringUtils.trimToEmpty(recipient), "+");
        if (!StringUtils.isNumeric(msisdn) || msisdn.isEmpty()) {
            throw new InternalServerError("Invalid phone number for websms: " + recipient);
        }

        JsonArray recipients = new JsonArray();
        recipients.add(Long.parseLong(msisdn));
        JsonObject body = new JsonObject();
        body.add("recipientAddressList", recipients);
        body.addProperty("messageContent", content);
        body.addProperty("maxSmsPerMessage", cfg.getMaxSmsPerMessage());
        body.addProperty("test", cfg.isTest());

        HttpPost req = new HttpPost(cfg.getUrl() + TEXT_PATH);
        req.setHeader(HttpHeaders.AUTHORIZATION, getAuthorization());
        req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        req.setEntity(new StringEntity(GsonUtil.get().toJson(body), ContentType.APPLICATION_JSON));

        log.info("Sending SMS notification to +{} with {} characters", msisdn, content.length());
        try (CloseableHttpResponse res = client.execute(req)) {
            int httpStatus = res.getStatusLine().getStatusCode();
            String resBody = res.getEntity() == null ? "" : EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8);
            if (httpStatus == 401 || httpStatus == 403) {
                throw new InternalServerError("websms rejected the credentials (HTTP " + httpStatus + ")");
            }
            if (httpStatus < 200 || httpStatus >= 300) {
                throw new InternalServerError("websms returned HTTP " + httpStatus + ": " + resBody);
            }

            JsonObject result = GsonUtil.parseObj(resBody);
            int statusCode = GsonUtil.findPrimitive(result, "statusCode").map(p -> p.getAsInt()).orElse(-1);
            String statusMessage = GsonUtil.findString(result, "statusMessage").orElse("");
            if (statusCode != STATUS_OK && statusCode != STATUS_QUEUED) {
                throw new InternalServerError("websms error " + statusCode + ": " + statusMessage);
            }
            log.info("SMS accepted by websms (status {}, transfer ID {})", statusCode,
                GsonUtil.findString(result, "transferId").orElse("-"));
        } catch (IOException e) {
            log.error("Unable to reach websms at {}", cfg.getUrl(), e);
            throw new InternalServerError(e);
        }
    }

    private String getAuthorization() {
        if (StringUtils.isNotBlank(cfg.getAccessToken())) {
            return "Bearer " + cfg.getAccessToken();
        }
        String credentials = cfg.getUsername() + ":" + cfg.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

}
