package com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
public class KitchenOrderAssemblyFinishedEvent implements Event {
    KitchenOrderRef ref;
}
