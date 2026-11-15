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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends SMS through websms.com with the official websms Java SDK
 * (https://github.com/websms-com/websmscom-java, MIT, built from the
 * third_party/websmscom-java submodule).
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
            log.error("websms connector is not fully configured: username and password are required.");
            throw new NotImplementedException("Phone numbers cannot be validated at this time. Contact your administrator.");
        }

        // ma1sd passes the MSISDN without "+"; websms expects the digits only
        String msisdn = StringUtils.removeStart(StringUtils.trimToEmpty(recipient), "+");
        if (msisdn.isEmpty() || !StringUtils.isNumeric(msisdn)) {
            throw new InternalServerError("Invalid phone number for websms: " + recipient);
        }

        log.info("Sending SMS notification to +{} with {} characters", msisdn, content.length());
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

}
