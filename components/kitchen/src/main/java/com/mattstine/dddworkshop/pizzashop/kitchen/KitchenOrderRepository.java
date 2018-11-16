package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Repository;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrderRef;

interface KitchenOrderRepository extends Repository<KitchenOrderRef, KitchenOrder, KitchenOrder.OrderState, KitchenOrderEvent, KitchenOrderAddedEvent> {
    KitchenOrder findByOnlineOrderRef(OnlineOrderRef onlineOrderRef);
}
