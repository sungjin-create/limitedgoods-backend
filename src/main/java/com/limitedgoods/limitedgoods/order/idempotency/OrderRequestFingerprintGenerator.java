package com.limitedgoods.limitedgoods.order.idempotency;

import com.limitedgoods.limitedgoods.order.dto.OrderItemsListDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
public class OrderRequestFingerprintGenerator {

    private static final String VERSION = "v1";

    public String generate(List<OrderItemsListDto> items) {
        String canonicalRequest = items.stream()
                .sorted(Comparator.comparing(
                        OrderItemsListDto::getProductId
                ))
                .map(item ->
                        item.getProductId()
                                + ":"
                                + item.getQuantity()
                )
                .reduce(
                        VERSION,
                        (result, item) -> result + "|" + item
                );

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
            throw new IllegalStateException(
                    "SHA-256 알고리즘을 사용할 수 없습니다.",
                    e
            );
        }
    }
}