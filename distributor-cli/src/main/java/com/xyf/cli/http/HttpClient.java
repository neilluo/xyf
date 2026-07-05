package com.xyf.cli.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 客户端工具 - 封装 OkHttp + Bearer Token 自动注入
 */
public class HttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;

    public HttpClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request.Builder builder = chain.request().newBuilder();
                    if (apiKey != null && !apiKey.isBlank()) {
                        builder.header("Authorization", "Bearer " + apiKey);
                    }
                    return chain.proceed(builder.build());
                })
                .build();
    }

    public String get(String path) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .build();
        return execute(request);
    }

    public String post(String path, Object body) throws IOException {
        String json = MAPPER.writeValueAsString(body);
        RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(reqBody)
                .build();
        return execute(request);
    }

    public String postEmpty(String path) throws IOException {
        RequestBody reqBody = RequestBody.create("", MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(reqBody)
                .build();
        return execute(request);
    }

    private String execute(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String bodyStr = responseBody != null ? responseBody.string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + bodyStr);
            }
            return bodyStr;
        }
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}
