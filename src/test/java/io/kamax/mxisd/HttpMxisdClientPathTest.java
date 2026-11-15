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

package io.kamax.mxisd;

import io.kamax.mxisd.http.undertow.handler.invite.v1.RoomInviteHandler;
import io.kamax.mxisd.http.undertow.handler.directory.v1.UserDirectorySearchHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpMxisdClientPathTest {

    @Test
    public void servesR0AndV3() {
        assertEquals("/_matrix/client/r0/rooms/{roomId}/invite", HttpMxisd.clientPath(RoomInviteHandler.Path, "r0"));
        assertEquals("/_matrix/client/v3/rooms/{roomId}/invite", HttpMxisd.clientPath(RoomInviteHandler.Path, "v3"));
        assertEquals("/_matrix/client/v3/user_directory/search", HttpMxisd.clientPath(UserDirectorySearchHandler.Path, "v3"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonR0Paths() {
        HttpMxisd.clientPath("/_matrix/identity/v2/lookup", "v3");
    }

}
