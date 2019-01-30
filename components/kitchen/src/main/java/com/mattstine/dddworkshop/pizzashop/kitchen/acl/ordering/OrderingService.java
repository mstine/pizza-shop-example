package com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering;

/**
 * @author Matt Stine
 */
public interface OrderingService {
    OnlineOrder findByRef(OnlineOrderRef ref);
}
