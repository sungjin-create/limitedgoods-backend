package com.limitedgoods.limitedgoods.backoffice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class LokiClient {

    private final RestClient restClient;

    public LokiClient(@Value("${monitoring.loki.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Map queryRange(String logql, long startNanos, long endNanos, int limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/loki/api/v1/query_range")
                        .queryParam("query", "{query}")
                        .queryParam("start", startNanos)
                        .queryParam("end", endNanos)
                        .queryParam("limit", limit)
                        .build(logql))
                .retrieve()
                .body(Map.class);
    }
}
