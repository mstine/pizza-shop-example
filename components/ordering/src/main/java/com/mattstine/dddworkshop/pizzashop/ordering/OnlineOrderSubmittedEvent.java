package com.mattstine.dddworkshop.pizzashop.ordering;

import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
final class OnlineOrderSubmittedEvent implements OnlineOrderEvent {
    OnlineOrderRef ref;
}
