package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.RepositoryAddEvent;
import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
final class OnlineOrderAddedEvent implements OnlineOrderEvent, RepositoryAddEvent {
    OnlineOrderRef ref;
    OnlineOrder.OrderState orderState;
}
