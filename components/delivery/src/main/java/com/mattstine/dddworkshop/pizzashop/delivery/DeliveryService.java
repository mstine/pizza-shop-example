package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrder;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrderAssemblyFinishedEvent;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenService;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrder;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.ordering.OrderingService;

/**
 * @author Matt Stine
 */
final class DeliveryService {
	private final EventLog eventLog;
	private final DeliveryOrderRepository deliveryOrderRepository;
	private final OrderingService orderingService;
	private final KitchenService kitchenService;

	DeliveryService(EventLog eventLog, DeliveryOrderRepository deliveryOrderRepository, OrderingService orderingService, KitchenService kitchenService) {
		this.eventLog = eventLog;
		this.deliveryOrderRepository = deliveryOrderRepository;
		this.orderingService = orderingService;
		this.kitchenService = kitchenService;

		this.eventLog.subscribe(new Topic("kitchen_orders"), e -> {
			KitchenOrderAssemblyFinishedEvent kitchenOrderAssemblyFinishedEvent = (KitchenOrderAssemblyFinishedEvent) e;
			addDeliveryOrderToRepository(kitchenOrderAssemblyFinishedEvent);
		});
	}

	private void addDeliveryOrderToRepository(KitchenOrderAssemblyFinishedEvent e) {
		KitchenOrderRef kitchenOrderRef = e.getRef();
		KitchenOrder kitchenOrder = kitchenService.findKitchenOrderByRef(kitchenOrderRef);
		OnlineOrderRef onlineOrderRef = kitchenOrder.getOnlineOrderRef();
		OnlineOrder onlineOrder = orderingService.findByRef(onlineOrderRef);

		if (onlineOrder.getType().equals(OnlineOrder.Type.DELIVERY)) {
			DeliveryOrder deliveryOrder = kitchenOrderToDeliveryOrder(kitchenOrder);
			deliveryOrderRepository.add(deliveryOrder);
		}
	}

	private DeliveryOrder kitchenOrderToDeliveryOrder(KitchenOrder kitchenOrder) {
		DeliveryOrder.DeliveryOrderBuilder deliveryOrderBuilder = DeliveryOrder.builder()
				.ref(deliveryOrderRepository.nextIdentity())
				.kitchenOrderRef(kitchenOrder.getRef())
				.onlineOrderRef(kitchenOrder.getOnlineOrderRef())
				.eventLog(eventLog);

		kitchenOrder.getPizzas().forEach(
				pizza -> deliveryOrderBuilder.pizza(DeliveryOrder.Pizza.builder()
						.size(kitchenOrderPizzaSizeToDeliveryOrderPizzaSize(pizza.getSize()))
						.build()));

		return deliveryOrderBuilder.build();
	}

	private DeliveryOrder.Pizza.Size kitchenOrderPizzaSizeToDeliveryOrderPizzaSize(KitchenOrder.Pizza.Size size) {
		switch (size) {
			case SMALL:
				return DeliveryOrder.Pizza.Size.SMALL;
			case MEDIUM:
				return DeliveryOrder.Pizza.Size.MEDIUM;
			case LARGE:
				return DeliveryOrder.Pizza.Size.LARGE;
			default:
				throw new IllegalStateException("size must be member of KitchenOrder.Pizza.Size enum");
		}
	}

	public DeliveryOrder findDeliveryOrderByKitchenOrderRef(KitchenOrderRef kitchenOrderRef) {
		return deliveryOrderRepository.findByKitchenOrderRef(kitchenOrderRef);
	}
}
