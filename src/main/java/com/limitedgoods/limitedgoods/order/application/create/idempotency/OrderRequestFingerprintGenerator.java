package com.limitedgoods.limitedgoods.order.application.create.idempotency;

import com.limitedgoods.limitedgoods.order.dto.request.OrderItemsListDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
public class OrderRequestFingerprintGenerator {

    private static final String VERSION = "v1";

    public String generate(List<OrderItemsListDto> items) {
        List<OrderItemsListDto> sortedItems = new ArrayList<>(items);

        sortedItems.sort(Comparator.comparing(OrderItemsListDto::getProductId));

        String canonicalRequest = VERSION;

        for (OrderItemsListDto item : sortedItems) {
            canonicalRequest += "|" + item.getProductId() + ":" + item.getQuantity();
        }

        return sha256(canonicalRequest);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e );
        }
    }
}