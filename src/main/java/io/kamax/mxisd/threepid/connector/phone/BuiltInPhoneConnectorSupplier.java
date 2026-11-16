/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
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
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.config.threepid.connector.PhoneGotifyConfig;
import io.kamax.mxisd.config.threepid.connector.PhonePhonelinkConfig;
import io.kamax.mxisd.config.threepid.connector.PhoneSmstoolsConfig;
import io.kamax.mxisd.config.threepid.connector.PhoneTwilioConfig;
import io.kamax.mxisd.config.threepid.connector.PhoneWebsmsConfig;
import io.kamax.mxisd.config.threepid.medium.PhoneConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class BuiltInPhoneConnectorSupplier implements PhoneConnectorSupplier {

    @Override
    public Optional<PhoneConnector> apply(PhoneConfig cfg, Mxisd mxisd) {
        return create(cfg.getConnector(), cfg);
    }

    private Optional<PhoneConnector> create(String id, PhoneConfig cfg) {
        if (StringUtils.equals(PhonePhonelinkConnector.ID, id)) {
            PhonePhonelinkConfig cCfg = GsonUtil.get().fromJson(cfg.getConnectors().getOrDefault(PhonePhonelinkConnector.ID, new JsonObject()), PhonePhonelinkConfig.class).build();
            PhoneConnector fallback = null;
            if (StringUtils.isNotBlank(cCfg.getFallback())) {
                if (StringUtils.equals(PhonePhonelinkConnector.ID, cCfg.getFallback())) {
                    throw new IllegalArgumentException("The phonelink connector cannot be its own fallback");
                }
                fallback = create(cCfg.getFallback(), cfg).orElseThrow(() ->
                    new IllegalArgumentException("Unknown fallback phone connector: " + cCfg.getFallback()));
            }
            return Optional.of(new PhonePhonelinkConnector(cCfg, fallback));
        }

        if (StringUtils.equals(BlackholePhoneConnector.ID, id)) {
            return Optional.of(new BlackholePhoneConnector());
        }

        if (StringUtils.equals(PhoneSmsTwilioConnector.ID, id)) {
            PhoneTwilioConfig cCfg = GsonUtil.get().fromJson(cfg.getConnectors().getOrDefault(PhoneSmsTwilioConnector.ID, new JsonObject()), PhoneTwilioConfig.class);
            return Optional.of(new PhoneSmsTwilioConnector(cCfg));
        }

        if (StringUtils.equals(PhoneSmsWebsmsConnector.ID, id)) {
            PhoneWebsmsConfig cCfg = GsonUtil.get().fromJson(cfg.getConnectors().getOrDefault(PhoneSmsWebsmsConnector.ID, new JsonObject()), PhoneWebsmsConfig.class);
            return Optional.of(new PhoneSmsWebsmsConnector(cCfg));
        }

        if (StringUtils.equals(PhoneSmsSmstoolsConnector.ID, id)) {
            PhoneSmstoolsConfig cCfg = GsonUtil.get().fromJson(cfg.getConnectors().getOrDefault(PhoneSmsSmstoolsConnector.ID, new JsonObject()), PhoneSmstoolsConfig.class);
            return Optional.of(new PhoneSmsSmstoolsConnector(cCfg));
        }

        if (StringUtils.equals(PhoneGotifyConnector.ID, id)) {
            PhoneGotifyConfig cCfg = GsonUtil.get().fromJson(cfg.getConnectors().getOrDefault(PhoneGotifyConnector.ID, new JsonObject()), PhoneGotifyConfig.class);
            return Optional.of(new PhoneGotifyConnector(cCfg));
        }

        return Optional.empty();
    }

}
