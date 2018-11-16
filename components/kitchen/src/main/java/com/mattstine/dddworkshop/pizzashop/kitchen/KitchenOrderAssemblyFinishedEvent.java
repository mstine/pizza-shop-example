package com.mattstine.dddworkshop.pizzashop.kitchen;

import lombok.Value;

@Value
public final class KitchenOrderAssemblyFinishedEvent implements KitchenOrderEvent {
    KitchenOrderRef ref;
}
