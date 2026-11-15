/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

package io.kamax.mxisd.http.io.identity;

import io.kamax.mxisd.http.IsAPIv2;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Response of POST /_matrix/identity/v2/store-invite: the token, the display name and
 * the public keys [server long-term key, ephemeral key of this invite].
 */
public class ThreePidInviteReplyIO {

    private String token;
    private List<Key> public_keys;
    private String display_name;

    public ThreePidInviteReplyIO(IThreePidInviteReply reply, String pubKey, String publicUrl) {
        this.token = reply.getToken();
        this.display_name = reply.getDisplayName();

        String longTerm = pubKey;
        String ephemeral = reply.getInvite().getProperties().get("e_key_public");
        List<String> keys = reply.getPublicKeys();
        if (keys != null && !keys.isEmpty()) {
            longTerm = keys.get(0);
            if (keys.size() > 1) {
                ephemeral = keys.get(1);
            }
        }

        this.public_keys = new ArrayList<>();
        this.public_keys.add(new Key(longTerm, publicUrl + IsAPIv2.Base + "/pubkey/isvalid"));
        if (StringUtils.isNotBlank(ephemeral)) {
            this.public_keys.add(new Key(ephemeral, publicUrl + IsAPIv2.Base + "/pubkey/ephemeral/isvalid"));
        }
    }

    public String getToken() {
        return token;
    }

    public List<Key> getPublic_keys() {
        return public_keys;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public static class Key {
        private String key_validity_url;
        private String public_key;

        public Key(String key, String keyValidityUrl) {
            this.key_validity_url = keyValidityUrl;
            this.public_key = key;
        }

        public String getKey_validity_url() {
            return key_validity_url;
        }

        public String getPublic_key() {
            return public_key;
        }
    }

}
