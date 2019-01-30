package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenOrder;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenOrderAssemblyFinishedEvent;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenService;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering.OnlineOrder;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering.OrderingService;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Matt Stine
 */
public class DeliveryServiceWithEventSourcingIntegrationTests {

    private InProcessEventLog eventLog;
    private DeliveryService deliveryService;
    private OrderingService orderingService;
    private KitchenService kitchenService;

    @Before
    public void setUp() {
        eventLog = InProcessEventLog.instance();
        DeliveryOrderRepository deliveryOrderRepository = new InProcessEventSourcedDeliveryOrderRepository(eventLog,
                new Topic("delivery_orders"));
        orderingService = mock(OrderingService.class);
        kitchenService = mock(KitchenService.class);
        deliveryService = new DeliveryService(eventLog, deliveryOrderRepository, orderingService, kitchenService);
    }

    @After
    public void tearDown() {
        this.eventLog.purgeSubscribers();
    }

    @Test
    public void on_kitchenOrderAssemblyFinished_add_to_queue() {
        KitchenOrderRef kitchenOrderRef = new KitchenOrderRef();
        KitchenOrderAssemblyFinishedEvent kitchenOrderAssemblyFinishedEvent = new KitchenOrderAssemblyFinishedEvent(kitchenOrderRef);

        OnlineOrderRef onlineOrderRef = new OnlineOrderRef();
        OnlineOrder onlineOrder = OnlineOrder.builder()
                .type(OnlineOrder.Type.DELIVERY)
                .eventLog(eventLog)
                .ref(onlineOrderRef)
                .build();

        KitchenOrder kitchenOrder = KitchenOrder.builder()
                .ref(kitchenOrderRef)
                .onlineOrderRef(onlineOrderRef)
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.LARGE).build())
                .eventLog(eventLog)
                .build();

        when(orderingService.findByRef(onlineOrderRef)).thenReturn(onlineOrder);
        when(kitchenService.findKitchenOrderByRef(kitchenOrderRef)).thenReturn(kitchenOrder);

        eventLog.publish(new Topic("kitchen_orders"), kitchenOrderAssemblyFinishedEvent);

        DeliveryOrder deliveryOrder = deliveryService.findDeliveryOrderByKitchenOrderRef(kitchenOrderRef);
        assertThat(deliveryOrder).isNotNull();
    }
}
