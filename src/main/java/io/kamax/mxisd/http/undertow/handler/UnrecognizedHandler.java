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

package io.kamax.mxisd.http.undertow.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

/**
 * Answers requests no route matches: 404 for an unknown endpoint, 405 for a known
 * endpoint with the wrong method, both with M_UNRECOGNIZED as the spec expects.
 * OPTIONS (CORS pre-flight) goes to the options handler for any path.
 */
public class UnrecognizedHandler extends BasicHttpHandler {

    private final int status;
    private final HttpHandler options;

    public UnrecognizedHandler(int status, HttpHandler options) {
        this.status = status;
        this.options = options;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (Methods.OPTIONS.equals(exchange.getRequestMethod())) {
            options.handleRequest(exchange);
            return;
        }
        String error = status == 405 ? "Unsupported method" : "Unrecognized request";
        respond(exchange, status, "M_UNRECOGNIZED", error);
    }

}
