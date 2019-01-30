package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Repository;

/**
 * @author Matt Stine
 */
interface DeliveryOrderRepository extends Repository<DeliveryOrderRef, DeliveryOrder, DeliveryOrder.OrderState, DeliveryOrderEvent, DeliveryOrderAddedEvent> {
    DeliveryOrder findByKitchenOrderRef(KitchenOrderRef kitchenOrderRef);
}
