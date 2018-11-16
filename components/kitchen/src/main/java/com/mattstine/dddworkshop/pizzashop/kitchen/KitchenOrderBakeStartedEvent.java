package com.mattstine.dddworkshop.pizzashop.kitchen;

import lombok.Value;

@Value
final class KitchenOrderBakeStartedEvent implements KitchenOrderEvent {
    KitchenOrderRef ref;
}
