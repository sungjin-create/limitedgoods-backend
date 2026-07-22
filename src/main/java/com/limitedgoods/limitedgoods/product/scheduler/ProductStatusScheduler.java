package com.limitedgoods.limitedgoods.product.scheduler;

import com.limitedgoods.limitedgoods.product.service.ProductStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStatusScheduler {

    private final ProductStatusService productStatusService;

    @Scheduled(fixedDelay = 1000)
    public void activeStatus(){
        int updateCount = productStatusService.activateScheduledProducts();
        if(updateCount != 0){
            log.debug("상품 상태 SCHEDULED -> ACTIVE 처리 건수 = {}", updateCount);
        }
    }

}
