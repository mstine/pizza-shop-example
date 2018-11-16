package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.payments.PaymentRef;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Matt Stine
 */
public class OnlineOrderTests {

    private EventLog eventLog;
    private OnlineOrder onlineOrder;
    private Pizza pizza;
    private OnlineOrderRef ref;

    @Before
    public void setUp() {
        ref = new OnlineOrderRef();
        eventLog = mock(EventLog.class);
        onlineOrder = OnlineOrder.builder()
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .ref(ref)
                .build();
        pizza = Pizza.builder().size(Pizza.Size.MEDIUM).build();
    }

    @Test
    public void new_order_is_new() {
        assertThat(onlineOrder.isNew()).isTrue();
    }

    @Test
    public void should_create_pickup_order() {
        assertThat(onlineOrder.isPickupOrder()).isTrue();
    }

    @Test
    public void should_create_delivery_order() {
        onlineOrder = OnlineOrder.builder()
                .type(OnlineOrder.Type.DELIVERY)
                .eventLog(eventLog)
                .ref(new OnlineOrderRef())
                .build();
        assertThat(onlineOrder.isDeliveryOrder()).isTrue();
    }

    @Test
    public void should_add_pizza() {
        onlineOrder.addPizza(pizza);
        assertThat(onlineOrder.getPizzas()).contains(pizza);
    }

    @Test
    public void adding_pizza_fires_event() {
        onlineOrder.addPizza(pizza);
        verify(eventLog)
                .publish(eq(new Topic("ordering")),
                        eq(new PizzaAddedEvent(ref, pizza)));
    }

    @Test
    public void can_only_add_pizza_to_new_order() {
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();
        assertThatIllegalStateException().isThrownBy(() -> onlineOrder.addPizza(pizza));
    }

    @Test
    public void submit_order_updates_state() {
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();
        assertThat(onlineOrder.isSubmitted()).isTrue();
    }

    @Test
    public void submit_order_fires_event() {
        onlineOrder.addPizza(Pizza.builder().size(Pizza.Size.MEDIUM).build());
        onlineOrder.submit();
        verify(eventLog)
                .publish(eq(new Topic("ordering")),
                        isA(OnlineOrderSubmittedEvent.class));
    }

    @Test
    public void submit_requires_at_least_one_pizza() {
        assertThatIllegalStateException()
                .isThrownBy(() -> onlineOrder.submit());
    }

    @Test
    public void can_only_submit_new_order() {
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();
        assertThatIllegalStateException().isThrownBy(onlineOrder::submit);
    }

    @Test
    public void calculates_price() {
        onlineOrder.addPizza(pizza);
        assertThat(onlineOrder.calculatePrice()).isEqualTo(Pizza.Size.MEDIUM.getPrice());
    }

    @Test
    public void mark_paid_updates_state() {
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();
        onlineOrder.markPaid();
        assertThat(onlineOrder.isPaid()).isTrue();
    }

    @Test
    public void mark_paid_fires_event() {
        onlineOrder.addPizza(pizza);
        verify(eventLog).publish(eq(new Topic("ordering")), isA(PizzaAddedEvent.class));
        onlineOrder.submit();
        verify(eventLog).publish(eq(new Topic("ordering")), isA(OnlineOrderSubmittedEvent.class));
        onlineOrder.markPaid();
        verify(eventLog).publish(eq(new Topic("ordering")), isA(OnlineOrderPaidEvent.class));
    }

    @Test
    public void can_only_mark_submitted_order_paid() {
        assertThatIllegalStateException().isThrownBy(onlineOrder::markPaid);
    }

    @Test
    public void setting_payment_ref_fires_event() {
        PaymentRef paymentRef = new PaymentRef();
        onlineOrder.assignPaymentRef(paymentRef);

        verify(eventLog).publish(eq(new Topic("ordering")), isA(PaymentRefAssignedEvent.class));
    }

    @Test
    public void accumulator_apply_with_orderAddedEvent_returns_order() {
        OnlineOrderAddedEvent orderAddedEvent = new OnlineOrderAddedEvent(ref, onlineOrder.state());
        assertThat(onlineOrder.accumulatorFunction().apply(onlineOrder.identity(), orderAddedEvent)).isEqualTo(onlineOrder);
    }

    @Test
    public void accumulator_apply_with_pizzaAddedEvent_updates_state() {
        OnlineOrder expectedOnlineOrder = OnlineOrder.builder()
                .ref(ref)
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .build();
        expectedOnlineOrder.addPizza(pizza);

        PizzaAddedEvent pae = new PizzaAddedEvent(ref, pizza);

        assertThat(onlineOrder.accumulatorFunction().apply(onlineOrder, pae)).isEqualTo(expectedOnlineOrder);
    }

    @Test
    public void accumulator_apply_with_orderSubmittedEvent_updates_state() {
        OnlineOrder expectedOnlineOrder = OnlineOrder.builder()
                .ref(ref)
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .build();
        expectedOnlineOrder.addPizza(pizza);
        expectedOnlineOrder.submit();

        PizzaAddedEvent pae = new PizzaAddedEvent(ref, pizza);
        onlineOrder.accumulatorFunction().apply(onlineOrder, pae);

        OnlineOrderSubmittedEvent ose = new OnlineOrderSubmittedEvent(ref);

        assertThat(onlineOrder.accumulatorFunction().apply(onlineOrder, ose)).isEqualTo(expectedOnlineOrder);
    }

    @Test
    public void accumulator_apply_with_paymentRefAssignedEvent_updates_state() {
        OnlineOrder expectedOnlineOrder = OnlineOrder.builder()
                .ref(ref)
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .build();
        expectedOnlineOrder.addPizza(pizza);
        expectedOnlineOrder.submit();

        PaymentRef paymentRef = new PaymentRef();
        expectedOnlineOrder.assignPaymentRef(paymentRef);

        PizzaAddedEvent pae = new PizzaAddedEvent(ref, pizza);
        onlineOrder.accumulatorFunction().apply(onlineOrder, pae);

        OnlineOrderSubmittedEvent ose = new OnlineOrderSubmittedEvent(ref);
        onlineOrder.accumulatorFunction().apply(onlineOrder, ose);

        @SuppressWarnings("SpellCheckingInspection")
        PaymentRefAssignedEvent prae = new PaymentRefAssignedEvent(ref, paymentRef);
        assertThat(onlineOrder.accumulatorFunction().apply(onlineOrder, prae)).isEqualTo(expectedOnlineOrder);
    }

    @Test
    public void accumulator_apply_with_orderPaidEvent_updates_state() {
        OnlineOrder expectedOnlineOrder = OnlineOrder.builder()
                .ref(ref)
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .build();
        expectedOnlineOrder.addPizza(pizza);
        expectedOnlineOrder.submit();

        PaymentRef paymentRef = new PaymentRef();
        expectedOnlineOrder.assignPaymentRef(paymentRef);

        expectedOnlineOrder.markPaid();

        PizzaAddedEvent pae = new PizzaAddedEvent(ref, pizza);
        onlineOrder.accumulatorFunction().apply(onlineOrder, pae);

        OnlineOrderSubmittedEvent ose = new OnlineOrderSubmittedEvent(ref);
        onlineOrder.accumulatorFunction().apply(onlineOrder, ose);

        @SuppressWarnings("SpellCheckingInspection")
        PaymentRefAssignedEvent prae = new PaymentRefAssignedEvent(ref, paymentRef);
        onlineOrder.accumulatorFunction().apply(onlineOrder, prae);

        OnlineOrderPaidEvent ope = new OnlineOrderPaidEvent(ref);

        assertThat(onlineOrder.accumulatorFunction().apply(onlineOrder, ope)).isEqualTo(expectedOnlineOrder);
    }

}
