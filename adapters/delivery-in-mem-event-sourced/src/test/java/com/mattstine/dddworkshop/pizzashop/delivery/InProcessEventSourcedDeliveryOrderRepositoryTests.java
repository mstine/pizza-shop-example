package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Matt Stine
 */
public class InProcessEventSourcedDeliveryOrderRepositoryTests {

    private DeliveryOrderRepository repository;
    private EventLog eventLog;
    private DeliveryOrderRef ref;
    private DeliveryOrder deliveryOrder;

    @Before
    public void setUp() {
        eventLog = mock(EventLog.class);
        repository = new InProcessEventSourcedDeliveryOrderRepository(eventLog,
                new Topic("delivery_orders"));
        ref = repository.nextIdentity();
        deliveryOrder = DeliveryOrder.builder()
                .ref(ref)
                .kitchenOrderRef(new KitchenOrderRef())
                .onlineOrderRef(new OnlineOrderRef())
                .pizza(DeliveryOrder.Pizza.builder().size(DeliveryOrder.Pizza.Size.MEDIUM).build())
                .eventLog(eventLog)
                .build();
    }

    @Test
    public void provides_next_identity() {
        assertThat(ref).isNotNull();
    }

    @Test
    public void add_fires_event() {
        repository.add(deliveryOrder);
        DeliveryOrderAddedEvent event = new DeliveryOrderAddedEvent(ref, deliveryOrder.state());
        verify(eventLog).publish(eq(new Topic("delivery_orders")), eq(event));
    }

    @Test
    public void find_by_ref_hydrates_added_order() {
        repository.add(deliveryOrder);

        when(eventLog.eventsBy(new Topic("delivery_orders")))
                .thenReturn(Collections.singletonList(new DeliveryOrderAddedEvent(ref, deliveryOrder.state())));

        assertThat(repository.findByRef(ref)).isEqualTo(deliveryOrder);
    }
}
