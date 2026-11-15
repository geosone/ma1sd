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


import at.sms.business.sdk.client.SmsClient;
import at.sms.business.sdk.client.impl.DefaultSmsClient;
import at.sms.business.sdk.domain.TextMessage;
import at.sms.business.sdk.exception.ApiException;
import at.sms.business.sdk.exception.AuthorizationFailedException;
import at.sms.business.sdk.exception.HttpConnectionException;
import at.sms.business.sdk.exception.ParameterValidationException;
import io.kamax.mxisd.config.threepid.connector.PhoneWebsmsConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.exception.NotImplementedException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
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
 * Sends SMS through websms.com. With username and password it uses the official websms
 * Java SDK (https://github.com/websms-com/websmscom-java, MIT, built from the
 * third_party/websmscom-java submodule); the SDK has no token support, so with an API
 * access token the same request is sent here with "Authorization: Bearer".
 */
public class PhoneSmsWebsmsConnector implements PhoneConnector {

    public static final String ID = "websms";

    // 2000 = OK, 2001 = OK (queued)
    private static final int STATUS_OK = 2000;
    private static final int STATUS_QUEUED = 2001;

    private static final int READ_TIMEOUT_MS = 30_000;

    private transient final Logger log = LoggerFactory.getLogger(PhoneSmsWebsmsConnector.class);

    private final PhoneWebsmsConfig cfg;

    public PhoneSmsWebsmsConnector(PhoneWebsmsConfig cfg) {
        this.cfg = cfg.build();
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
        if (msisdn.isEmpty() || !StringUtils.isNumeric(msisdn)) {
            throw new InternalServerError("Invalid phone number for websms: " + recipient);
        }

        log.info("Sending SMS notification to +{} with {} characters", msisdn, content.length());
        if (cfg.hasAccessToken()) {
            sendWithToken(Long.parseLong(msisdn), content);
            return;
        }
        try {
            SmsClient client = new DefaultSmsClient(cfg.getUsername(), cfg.getPassword(), cfg.getUrl(), READ_TIMEOUT_MS);
            TextMessage message = new TextMessage(new long[]{Long.parseLong(msisdn)}, content);
            int status = client.send(message, cfg.getMaxSmsPerMessage(), cfg.isTest());
            if (status != STATUS_OK && status != STATUS_QUEUED) {
                throw new InternalServerError("websms returned status " + status);
            }
            log.info("SMS accepted by websms (status {})", status);
        } catch (AuthorizationFailedException e) {
            log.error("websms rejected the credentials of user {}", cfg.getUsername());
            throw new InternalServerError(e);
        } catch (ApiException e) {
            log.error("websms error {}: {}", e.statuscode, e.getMessage());
            throw new InternalServerError(e);
        } catch (ParameterValidationException e) {
            log.error("websms request was invalid: {}", e.getMessage());
            throw new InternalServerError(e);
        } catch (HttpConnectionException e) {
            log.error("Unable to reach websms at {}: {}", cfg.getUrl(), e.getMessage());
            throw new InternalServerError(e);
        }
    }

    private static final String TEXT_PATH = "/json/smsmessaging/text";

    private void sendWithToken(long msisdn, String content) {
        JsonArray recipients = new JsonArray();
        recipients.add(msisdn);
        JsonObject body = new JsonObject();
        body.add("recipientAddressList", recipients);
        body.addProperty("messageContent", content);
        body.addProperty("maxSmsPerMessage", cfg.getMaxSmsPerMessage());
        body.addProperty("test", cfg.isTest());

        HttpPost req = new HttpPost(cfg.getUrl() + TEXT_PATH);
        req.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.getAccessToken());
        req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        req.setEntity(new StringEntity(GsonUtil.get().toJson(body), ContentType.APPLICATION_JSON));
        RequestConfig timeouts = RequestConfig.custom().setConnectTimeout(10_000).setSocketTimeout(READ_TIMEOUT_MS).build();

        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(timeouts).build();
             CloseableHttpResponse res = client.execute(req)) {
            int httpStatus = res.getStatusLine().getStatusCode();
            String resBody = res.getEntity() == null ? "" : EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8);
            if (httpStatus == 401 || httpStatus == 403) {
                // websms echoes the token in its error text: never log the body here
                log.error("websms rejected the access token (HTTP {})", httpStatus);
                throw new InternalServerError("websms rejected the access token");
            }
            if (httpStatus < 200 || httpStatus >= 300) {
                log.error("websms returned HTTP {}", httpStatus);
                throw new InternalServerError("websms returned HTTP " + httpStatus);
            }
            JsonObject result = GsonUtil.parseObj(resBody);
            int status = GsonUtil.findPrimitive(result, "statusCode").map(p -> p.getAsInt()).orElse(-1);
            if (status != STATUS_OK && status != STATUS_QUEUED) {
                log.error("websms error {}: {}", status, GsonUtil.findString(result, "statusMessage").orElse(""));
                throw new InternalServerError("websms returned status " + status);
            }
            log.info("SMS accepted by websms (status {})", status);
        } catch (IOException e) {
            log.error("Unable to reach websms at {}: {}", cfg.getUrl(), e.getMessage());
            throw new InternalServerError(e);
        }
    }

}
