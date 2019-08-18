package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderRef;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class KitchenOrderTests {

    private KitchenOrder kitchenOrder;
    private EventLog eventLog;
    private KitchenOrderRef ref;
    private OnlineOrderRef onlineOrderRef;

    @Before
    public void setUp() {
        eventLog = mock(EventLog.class);
        ref = new KitchenOrderRef();
        onlineOrderRef = new OnlineOrderRef();
        kitchenOrder = KitchenOrder.builder()
                .ref(ref)
                .onlineOrderRef(onlineOrderRef)
                .eventLog(eventLog)
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.SMALL).build())
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .build();
    }

    @Test
    public void can_build_new_order() {
        assertThat(kitchenOrder).isNotNull();
    }

    @Test
    public void new_order_is_new() {
        assertThat(kitchenOrder.isNew()).isTrue();
    }

    @Test
    public void start_order_prep_updates_state() {
        kitchenOrder.startPrep();
        assertThat(kitchenOrder.isPrepping()).isTrue();
    }

    @Test
    public void only_new_order_can_start_prep() {
        kitchenOrder.startPrep();
        assertThatIllegalStateException().isThrownBy(kitchenOrder::startPrep);
    }

    @Test
    public void start_order_bake_updates_state() {
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        assertThat(kitchenOrder.isBaking()).isTrue();
    }

    @Test
    public void only_prepping_order_can_start_bake() {
        assertThatIllegalStateException().isThrownBy(kitchenOrder::startBake);
    }

    @Test
    public void start_order_assembly_updates_state() {
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        kitchenOrder.startAssembly();
        assertThat(kitchenOrder.hasStartedAssembly()).isTrue();
    }

    @Test
    public void only_baking_order_can_start_assembly() {
        assertThatIllegalStateException().isThrownBy(kitchenOrder::startAssembly);
    }

    @Test
    public void finish_order_assembly_updates_state() {
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        kitchenOrder.startAssembly();
        kitchenOrder.finishAssembly();
        assertThat(kitchenOrder.hasFinishedAssembly()).isTrue();
    }

    @Test
    public void only_assembling_order_can_finish_assembly() {
        assertThatIllegalStateException().isThrownBy(kitchenOrder::finishAssembly);
    }

    @Test
    public void start_order_prep_fires_event() {
        kitchenOrder.startPrep();
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderPrepStartedEvent.class));
    }

    @Test
    public void start_order_bake_fires_event() {
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderPrepStartedEvent.class));
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderBakeStartedEvent.class));
    }

    @Test
    public void start_order_assembly_fires_event() {
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        kitchenOrder.startAssembly();
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderPrepStartedEvent.class));
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderBakeStartedEvent.class));
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderAssemblyStartedEvent.class));
    }

    @Test
    public void finish_order_assembly_fires_event() {
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        kitchenOrder.startAssembly();
        kitchenOrder.finishAssembly();
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderPrepStartedEvent.class));
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderBakeStartedEvent.class));
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderAssemblyStartedEvent.class));
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), isA(KitchenOrderAssemblyFinishedEvent.class));
    }

    @Test
    public void accumulator_apply_with_orderAddedEvent_returns_order() {
        KitchenOrderAddedEvent orderAddedEvent = new KitchenOrderAddedEvent(ref, kitchenOrder.state());
        assertThat(kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder.identity(), orderAddedEvent)).isEqualTo(kitchenOrder);
    }

    @Test
    public void accumulator_apply_with_orderPrepStartedEvent_returns_order() {
        KitchenOrder expectedKitchenOrder = KitchenOrder.builder()
                .ref(ref)
                .onlineOrderRef(onlineOrderRef)
                .eventLog(eventLog)
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.SMALL).build())
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .build();
        expectedKitchenOrder.startPrep();

        KitchenOrderAddedEvent orderAddedEvent = new KitchenOrderAddedEvent(ref, kitchenOrder.state());
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder.identity(), orderAddedEvent);

        KitchenOrderPrepStartedEvent orderPrepStartedEvent = new KitchenOrderPrepStartedEvent(ref);
        assertThat(kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderPrepStartedEvent)).isEqualTo(expectedKitchenOrder);
    }

    @Test
    public void accumulator_apply_with_orderBakeStartedEvent_returns_order() {
        KitchenOrder expectedKitchenOrder = KitchenOrder.builder()
                .ref(ref)
                .onlineOrderRef(onlineOrderRef)
                .eventLog(eventLog)
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.SMALL).build())
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .build();
        expectedKitchenOrder.startPrep();
        expectedKitchenOrder.startBake();

        KitchenOrderAddedEvent orderAddedEvent = new KitchenOrderAddedEvent(ref, kitchenOrder.state());
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder.identity(), orderAddedEvent);

        KitchenOrderPrepStartedEvent orderPrepStartedEvent = new KitchenOrderPrepStartedEvent(ref);
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderPrepStartedEvent);

        KitchenOrderBakeStartedEvent orderBakeStartedEvent = new KitchenOrderBakeStartedEvent(ref);
        assertThat(kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderBakeStartedEvent)).isEqualTo(expectedKitchenOrder);
    }

    @Test
    public void accumulator_apply_with_orderAssemblyStartedEvent_returns_order() {
        KitchenOrder expectedKitchenOrder = KitchenOrder.builder()
                .ref(ref)
                .onlineOrderRef(onlineOrderRef)
                .eventLog(eventLog)
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.SMALL).build())
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .build();
        expectedKitchenOrder.startPrep();
        expectedKitchenOrder.startBake();
        expectedKitchenOrder.startAssembly();

        KitchenOrderAddedEvent orderAddedEvent = new KitchenOrderAddedEvent(ref, kitchenOrder.state());
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder.identity(), orderAddedEvent);

        KitchenOrderPrepStartedEvent orderPrepStartedEvent = new KitchenOrderPrepStartedEvent(ref);
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderPrepStartedEvent);

        KitchenOrderBakeStartedEvent orderBakeStartedEvent = new KitchenOrderBakeStartedEvent(ref);
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderBakeStartedEvent);

        KitchenOrderAssemblyStartedEvent orderAssemblyStartedEvent = new KitchenOrderAssemblyStartedEvent(ref);
        assertThat(kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderAssemblyStartedEvent)).isEqualTo(expectedKitchenOrder);
    }

    @Test
    public void accumulator_apply_with_orderAssemblyFinishedEvent_returns_order() {
        KitchenOrder expectedKitchenOrder = KitchenOrder.builder()
                .ref(ref)
                .onlineOrderRef(onlineOrderRef)
                .eventLog(eventLog)
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.SMALL).build())
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .build();
        expectedKitchenOrder.startPrep();
        expectedKitchenOrder.startBake();
        expectedKitchenOrder.startAssembly();
        expectedKitchenOrder.finishAssembly();

        KitchenOrderAddedEvent orderAddedEvent = new KitchenOrderAddedEvent(ref, kitchenOrder.state());
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder.identity(), orderAddedEvent);

        KitchenOrderPrepStartedEvent orderPrepStartedEvent = new KitchenOrderPrepStartedEvent(ref);
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderPrepStartedEvent);

        KitchenOrderBakeStartedEvent orderBakeStartedEvent = new KitchenOrderBakeStartedEvent(ref);
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderBakeStartedEvent);

        KitchenOrderAssemblyStartedEvent orderAssemblyStartedEvent = new KitchenOrderAssemblyStartedEvent(ref);
        kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderAssemblyStartedEvent);

        KitchenOrderAssemblyFinishedEvent orderAssemblyFinishedEvent = new KitchenOrderAssemblyFinishedEvent(ref);
        assertThat(kitchenOrder.accumulatorFunction(eventLog).apply(kitchenOrder, orderAssemblyFinishedEvent)).isEqualTo(expectedKitchenOrder);
    }
}
