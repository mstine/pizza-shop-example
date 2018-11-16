package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrderRef;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.List;
import java.util.function.BiFunction;

/**
 * @author Matt Stine
 */
@Value
public final class DeliveryOrder implements Aggregate {
	DeliveryOrderRef ref;
	KitchenOrderRef kitchenOrderRef;
	OnlineOrderRef onlineOrderRef;
	List<Pizza> pizzas;
	EventLog $eventLog;
	@NonFinal
	State state;

	@Builder
	private DeliveryOrder(@NonNull DeliveryOrderRef ref,
						  @NonNull KitchenOrderRef kitchenOrderRef,
						  @NonNull OnlineOrderRef onlineOrderRef,
						  @Singular List<Pizza> pizzas,
						  @NonNull EventLog eventLog) {
		this.ref = ref;
		this.kitchenOrderRef = kitchenOrderRef;
		this.onlineOrderRef = onlineOrderRef;
		this.pizzas = pizzas;
		this.$eventLog = eventLog;

		this.state = State.READY_FOR_DELIVERY;
	}

	/**
	 * Private no-args ctor to support reflection ONLY.
	 */
	@SuppressWarnings("unused")
	private DeliveryOrder() {
		this.ref = null;
		this.kitchenOrderRef = null;
		this.onlineOrderRef = null;
		this.pizzas = null;
		this.$eventLog = null;
	}

	@Override
	public DeliveryOrder identity() {
		return DeliveryOrder.builder()
				.ref(DeliveryOrderRef.IDENTITY)
				.kitchenOrderRef(KitchenOrderRef.IDENTITY)
				.onlineOrderRef(OnlineOrderRef.IDENTITY)
				.eventLog(EventLog.IDENTITY)
				.build();
	}

	@Override
	public BiFunction<DeliveryOrder, DeliveryOrderEvent, DeliveryOrder> accumulatorFunction() {
		return new Accumulator();
	}

	@Override
	public OrderState state() {
		return new OrderState(ref, kitchenOrderRef, onlineOrderRef, pizzas);
	}

	public boolean isReadyForDelivery() {
		return this.state == State.READY_FOR_DELIVERY;
	}

	enum State {
		READY_FOR_DELIVERY
	}

	private static class Accumulator implements BiFunction<DeliveryOrder, DeliveryOrderEvent, DeliveryOrder> {

		@SuppressWarnings("SpellCheckingInspection")
		@Override
		public DeliveryOrder apply(DeliveryOrder deliveryOrder, DeliveryOrderEvent deliveryOrderEvent) {
			if (deliveryOrderEvent instanceof DeliveryOrderAddedEvent) {
				DeliveryOrderAddedEvent doae = (DeliveryOrderAddedEvent) deliveryOrderEvent;
				OrderState orderState = doae.getState();
				return DeliveryOrder.builder()
						.eventLog(InProcessEventLog.instance())
						.ref(orderState.getRef())
						.kitchenOrderRef(orderState.getKitchenOrderRef())
						.onlineOrderRef(orderState.getOnlineOrderRef())
						.pizzas(orderState.getPizzas())
						.build();
			}
			throw new IllegalStateException("Unknown DeliveryOrderEvent");
		}
	}

	/*
	 * Pizza Value Object for KitchenOrder Details Only
	 */
	@Value
	public static final class Pizza {
		Size size;

		@Builder
		private Pizza(@NonNull Size size) {
			this.size = size;
		}

		public enum Size {
			SMALL, MEDIUM, LARGE
		}
	}

	@Value
	static class OrderState implements AggregateState {
		DeliveryOrderRef ref;
		KitchenOrderRef kitchenOrderRef;
		OnlineOrderRef onlineOrderRef;
		List<Pizza> pizzas;
	}
}
