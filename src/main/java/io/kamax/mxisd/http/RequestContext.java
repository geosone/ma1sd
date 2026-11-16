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

package io.kamax.mxisd.http;

import java.util.Optional;

/**
 * Who made the current request, for code that runs on the request's worker thread
 * (e.g. a phone connector): the Matrix ID of an authenticated (v2) request.
 * Unauthenticated (v1) requests, as sent by a homeserver for registration, have none.
 */
public final class RequestContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    /**
     * Sets the user of the current request and returns the previous one, to restore it afterwards.
     */
    public static String setUserId(String userId) {
        String prev = USER_ID.get();
        if (userId == null) {
            USER_ID.remove();
        } else {
            USER_ID.set(userId);
        }
        return prev;
    }

    public static Optional<String> userId() {
        return Optional.ofNullable(USER_ID.get());
    }

}
