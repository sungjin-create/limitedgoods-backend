package com.limitedgoods.limitedgoods.backoffice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class PrometheusClient {

    private final RestClient restClient;

    public PrometheusClient(
            @Value("${monitoring.prometheus.base-url}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Map query(String promql) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/query")
                        .queryParam("query", "{query}")
                        .build(promql))
                .retrieve()
                .body(Map.class);
    }

    public Map queryRange(String promql, long start, long end, int stepSeconds) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/query_range")
                        .queryParam("query", "{query}")
                        .queryParam("start", start)
                        .queryParam("end", end)
                        .queryParam("step", stepSeconds)
                        .build(promql))
                .retrieve()
                .body(Map.class);
    }
}
