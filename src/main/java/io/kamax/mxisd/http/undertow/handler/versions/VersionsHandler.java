package io.kamax.mxisd.http.undertow.handler.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.undertow.server.HttpServerExchange;


public class VersionsHandler extends BasicHttpHandler {

    public static final String Path = "/_matrix/identity/versions";

    // Identity service API versions served: r0.3.0 and the same /v2 API since spec v1.1
    private static final String[] VERSIONS = {"r0.3.0", "v1.1"};

    public void handleRequest(HttpServerExchange exchange) {
        JsonArray versions = new JsonArray();
        for (String version : VERSIONS) {
            versions.add(version);
        }

        // {"versions": ["r0.3.0", "v1.1"]}
        JsonObject obj = new JsonObject();
        obj.add("versions", versions);

        respond(exchange, obj);
    }
}
