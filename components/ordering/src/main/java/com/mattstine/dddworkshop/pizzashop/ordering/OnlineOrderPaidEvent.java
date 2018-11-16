package com.mattstine.dddworkshop.pizzashop.ordering;

import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
final class OnlineOrderPaidEvent implements OnlineOrderEvent {
    OnlineOrderRef ref;
}
