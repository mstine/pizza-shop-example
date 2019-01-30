package com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen;

/**
 * @author Matt Stine
 */
public interface KitchenService {
    KitchenOrder findKitchenOrderByRef(KitchenOrderRef kitchenOrderRef);
}
