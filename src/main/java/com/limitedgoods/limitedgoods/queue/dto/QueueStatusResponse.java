package com.limitedgoods.limitedgoods.queue.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatusResponse {

    private boolean admitted;
    private Integer position;          // 대기 중일 때만 (admitted=false)
    private String admissionToken;     // 입장 가능할 때만 (admitted=true)
    private Integer estimatedWaitSec;  // 대기 중일 때만

    public static QueueStatusResponse admitted(String token) {
        return QueueStatusResponse.builder()
                .admitted(true)
                .admissionToken(token)
                .build();
    }

    public static QueueStatusResponse waiting(int position) {
        return QueueStatusResponse.builder()
                .admitted(false)
                .position(position)
                .estimatedWaitSec(position * 2)  // 1명당 약 2초 예상
                .build();
    }
}
