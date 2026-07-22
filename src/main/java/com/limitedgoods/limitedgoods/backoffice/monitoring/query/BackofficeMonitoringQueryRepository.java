package com.limitedgoods.limitedgoods.backoffice.monitoring.query;

import com.limitedgoods.limitedgoods.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackofficeMonitoringQueryRepository extends JpaRepository<Order, Long> {
}
