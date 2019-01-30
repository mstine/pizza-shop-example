package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventHandler;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.ordering.acl.payments.PaymentRef;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Matt Stine
 */
public class InProcessEventSourcedOnlineOnlineOrderRepositoryTests {

    private OnlineOrderRepository repository;
    private EventLog eventLog;
    private OnlineOrderRef ref;
    private OnlineOrder onlineOrder;
    private Pizza pizza;

    @Before
    public void setUp() {
        eventLog = mock(EventLog.class);
        repository = new InProcessEventSourcedOnlineOrderRepository(eventLog,
                new Topic("ordering"));
        ref = repository.nextIdentity();
        onlineOrder = OnlineOrder.builder()
                .ref(ref)
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .build();
        pizza = Pizza.builder().size(Pizza.Size.MEDIUM).build();
    }

    @Test
    public void provides_next_identity() {
        assertThat(ref).isNotNull();
    }

    @Test
    public void add_fires_event() {
        repository.add(onlineOrder);
        OnlineOrderAddedEvent event = new OnlineOrderAddedEvent(onlineOrder.getRef(), onlineOrder.state());
        verify(eventLog).publish(eq(new Topic("ordering")), eq(event));
    }

    @Test
    public void find_by_ref_hydrates_added_order() {
        repository.add(onlineOrder);

        when(eventLog.eventsBy(new Topic("ordering")))
                .thenReturn(Collections.singletonList(new OnlineOrderAddedEvent(ref, onlineOrder.state())));

        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void find_by_ref_hydrates_order_with_added_pizza() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);

        when(eventLog.eventsBy(new Topic("ordering")))
                .thenReturn(Arrays.asList(new OnlineOrderAddedEvent(ref, onlineOrder.state()),
                        new PizzaAddedEvent(ref, pizza)));

        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void find_by_ref_hydrates_submitted_order() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();

        when(eventLog.eventsBy(new Topic("ordering")))
                .thenReturn(Arrays.asList(new OnlineOrderAddedEvent(ref, onlineOrder.state()),
                        new PizzaAddedEvent(ref, pizza),
                        new OnlineOrderSubmittedEvent(ref)));

        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void find_by_ref_hydrates_order_with_paymentRef_assigned() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();

        PaymentRef paymentRef = new PaymentRef();
        onlineOrder.assignPaymentRef(paymentRef);

        when(eventLog.eventsBy(new Topic("ordering")))
                .thenReturn(Arrays.asList(new OnlineOrderAddedEvent(ref, onlineOrder.state()),
                        new PizzaAddedEvent(ref, pizza),
                        new OnlineOrderSubmittedEvent(ref),
                        new PaymentRefAssignedEvent(ref, paymentRef)));

        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void find_by_ref_hydrates_order_marked_paid() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();

        PaymentRef paymentRef = new PaymentRef();
        onlineOrder.assignPaymentRef(paymentRef);

        onlineOrder.markPaid();

        when(eventLog.eventsBy(new Topic("ordering")))
                .thenReturn(Arrays.asList(new OnlineOrderAddedEvent(ref, onlineOrder.state()),
                        new PizzaAddedEvent(ref, pizza),
                        new OnlineOrderSubmittedEvent(ref),
                        new PaymentRefAssignedEvent(ref, paymentRef),
                        new OnlineOrderPaidEvent(ref)));

        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void subscribes_to_ordering_topic() {
        verify(eventLog).subscribe(eq(new Topic("ordering")), isA(EventHandler.class));
    }

}
