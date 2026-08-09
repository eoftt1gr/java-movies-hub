package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        String json = gson.toJson(body);
        ex.getResponseHeaders().set(
                "Content-Type",
                CT_JSON
        );

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set(
                "Content-Type",
                CT_JSON
        );

        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    protected <T> T readJson(HttpExchange ex, Class<T> clazz) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                ex.getRequestBody(), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, clazz);
        }
    }
}