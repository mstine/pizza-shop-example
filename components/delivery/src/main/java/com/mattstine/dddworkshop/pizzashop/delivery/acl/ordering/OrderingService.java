package com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering;

/**
 * @author Matt Stine
 */
public interface OrderingService {
    OnlineOrder findByRef(OnlineOrderRef ref);
}
