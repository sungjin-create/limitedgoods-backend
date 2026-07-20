package com.limitedgoods.limitedgoods.backoffice.product.scheduler;

import com.limitedgoods.limitedgoods.backoffice.product.service.BackofficeProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStatusScheduler {

    private final BackofficeProductService backofficeProductService;

    @Scheduled(fixedDelay = 1000)
    public void activeStatus(){
        int updateCount = backofficeProductService.updateScheduledToActive();
        if(updateCount != 0){
            log.debug("상품 상태 SCHEDULED -> ACTIVE 처리 건수 = {}", updateCount);
        }
    }

}
