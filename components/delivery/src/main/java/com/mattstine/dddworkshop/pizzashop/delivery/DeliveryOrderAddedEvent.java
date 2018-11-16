package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.RepositoryAddEvent;
import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
final class DeliveryOrderAddedEvent implements DeliveryOrderEvent, RepositoryAddEvent {
	DeliveryOrderRef ref;
	DeliveryOrder.OrderState state;
}
