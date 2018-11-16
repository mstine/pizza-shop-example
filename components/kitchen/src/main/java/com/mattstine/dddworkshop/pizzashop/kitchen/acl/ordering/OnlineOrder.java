package com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matt Stine
 */
@Value
@EqualsAndHashCode
public class OnlineOrder {
	Type type;
	EventLog $eventLog;
	OnlineOrderRef ref;
	List<Pizza> pizzas;
	@NonFinal
	State state;

	@Builder
	private OnlineOrder(@NonNull Type type, @NonNull EventLog eventLog, @NonNull OnlineOrderRef ref) {
		this.type = type;
		this.$eventLog = eventLog;
		this.ref = ref;
		this.pizzas = new ArrayList<>();

		this.state = State.NEW;
	}

	boolean isPickupOrder() {
		return this.type == Type.PICKUP;
	}

	boolean isDeliveryOrder() {
		return this.type == Type.DELIVERY;
	}

	public boolean isNew() {
		return state == State.NEW;
	}

	boolean isSubmitted() {
		return this.state == State.SUBMITTED;
	}

	boolean isPaid() {
		return state == State.PAID;
	}

	public void addPizza(Pizza pizza) {
		this.pizzas.add(pizza);
	}

	enum State {
		NEW, SUBMITTED, PAID
	}

	public enum Type {
		IDENTITY, DELIVERY, PICKUP
	}
}
