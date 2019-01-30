package com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * @author Matt Stine
 */
@Value
public class OnlineOrderPaidEvent implements Event {
    @NonFinal
    OnlineOrderRef ref;
}
