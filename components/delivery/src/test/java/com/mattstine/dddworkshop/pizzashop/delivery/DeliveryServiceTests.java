package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenService;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering.OrderingService;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventHandler;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Matt Stine
 */
public class DeliveryServiceTests {

    private DeliveryService service;
    private EventLog eventLog;
    private DeliveryOrderRepository deliveryOrderRepository;

    @Before
    public void setUp() {
        eventLog = mock(EventLog.class);
        deliveryOrderRepository = mock(DeliveryOrderRepository.class);
        OrderingService orderingService = mock(OrderingService.class);
        KitchenService kitchenService = mock(KitchenService.class);
        service = new DeliveryService(eventLog, deliveryOrderRepository, orderingService, kitchenService);
    }

    @Test
    public void subscribes_to_kitchen_orders_topic() {
        verify(eventLog).subscribe(eq(new Topic("kitchen_orders")), isA(EventHandler.class));
    }

    @Test
    public void should_return_deliveryOrder_by_kitchenOrderRef() {
        KitchenOrderRef kitchenOrderRef = new KitchenOrderRef();

        DeliveryOrder deliveryOrder = DeliveryOrder.builder()
                .ref(new DeliveryOrderRef())
                .kitchenOrderRef(new KitchenOrderRef())
                .onlineOrderRef(new OnlineOrderRef())
                .pizza(DeliveryOrder.Pizza.builder().size(DeliveryOrder.Pizza.Size.MEDIUM).build())
                .eventLog(eventLog)
                .build();

        when(deliveryOrderRepository.findByKitchenOrderRef(eq(kitchenOrderRef))).thenReturn(deliveryOrder);

        assertThat(service.findDeliveryOrderByKitchenOrderRef(kitchenOrderRef)).isEqualTo(deliveryOrder);
    }
}
