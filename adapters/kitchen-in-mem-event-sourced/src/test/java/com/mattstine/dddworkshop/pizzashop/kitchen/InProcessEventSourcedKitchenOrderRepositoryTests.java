package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderRef;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class InProcessEventSourcedKitchenOrderRepositoryTests {

    private KitchenOrderRepository repository;
    private EventLog eventLog;
    private KitchenOrderRef ref;
    private KitchenOrder kitchenOrder;

    @Before
    public void setUp() {
        eventLog = mock(EventLog.class);
        repository = new InProcessEventSourcedKitchenOrderRepository(eventLog,
                new Topic("kitchen_orders"));
        ref = repository.nextIdentity();
        kitchenOrder = KitchenOrder.builder()
                .ref(ref)
                .onlineOrderRef(new OnlineOrderRef())
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .eventLog(eventLog)
                .build();
    }

    @Test
    public void provides_next_identity() {
        assertThat(ref).isNotNull();
    }

    @Test
    public void add_fires_event() {
        repository.add(kitchenOrder);
        KitchenOrderAddedEvent event = new KitchenOrderAddedEvent(ref, kitchenOrder.state());
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), eq(event));
    }


    @Test
    public void find_by_ref_hydrates_added_order() {
        repository.add(kitchenOrder);

        when(eventLog.eventsBy(new Topic("kitchen_orders")))
                .thenReturn(Collections.singletonList(new KitchenOrderAddedEvent(ref, kitchenOrder.state())));

        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }

    @Test
    public void find_by_ref_hydrates_prepping_order() {
        repository.add(kitchenOrder);
        kitchenOrder.startPrep();

        when(eventLog.eventsBy(new Topic("kitchen_orders")))
                .thenReturn(Arrays.asList(new KitchenOrderAddedEvent(ref, kitchenOrder.state()),
                        new KitchenOrderPrepStartedEvent(ref)));

        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }

    @Test
    public void find_by_ref_hydrates_baking_order() {
        repository.add(kitchenOrder);
        kitchenOrder.startPrep();
        kitchenOrder.startBake();

        when(eventLog.eventsBy(new Topic("kitchen_orders")))
                .thenReturn(Arrays.asList(new KitchenOrderAddedEvent(ref, kitchenOrder.state()),
                        new KitchenOrderPrepStartedEvent(ref),
                        new KitchenOrderBakeStartedEvent(ref)));

        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }

    @Test
    public void find_by_ref_hydrates_assembling_order() {
        repository.add(kitchenOrder);
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        kitchenOrder.startAssembly();

        when(eventLog.eventsBy(new Topic("kitchen_orders")))
                .thenReturn(Arrays.asList(new KitchenOrderAddedEvent(ref, kitchenOrder.state()),
                        new KitchenOrderPrepStartedEvent(ref),
                        new KitchenOrderBakeStartedEvent(ref),
                        new KitchenOrderAssemblyStartedEvent(ref)));

        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }

    @Test
    public void find_by_ref_hydrates_assembled_order() {
        repository.add(kitchenOrder);
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        kitchenOrder.startAssembly();
        kitchenOrder.finishAssembly();

        when(eventLog.eventsBy(new Topic("kitchen_orders")))
                .thenReturn(Arrays.asList(new KitchenOrderAddedEvent(ref, kitchenOrder.state()),
                        new KitchenOrderPrepStartedEvent(ref),
                        new KitchenOrderBakeStartedEvent(ref),
                        new KitchenOrderAssemblyStartedEvent(ref),
                        new KitchenOrderAssemblyFinishedEvent(ref)));

        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }
}
