package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Repository;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrderRef;

/**
 * @author Matt Stine
 */
interface DeliveryOrderRepository extends Repository<DeliveryOrderRef, DeliveryOrder, DeliveryOrder.OrderState, DeliveryOrderEvent, DeliveryOrderAddedEvent> {
	DeliveryOrder findByKitchenOrderRef(KitchenOrderRef kitchenOrderRef);
}
