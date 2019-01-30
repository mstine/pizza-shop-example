package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventHandler;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OrderingService;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DefaultKitchenServiceTests {

    private KitchenService service;
    private KitchenOrderRepository kitchenOrderRepository;
    private EventLog eventLog;

    @Before
    public void setUp() {
        eventLog = mock(EventLog.class);
        kitchenOrderRepository = mock(KitchenOrderRepository.class);
        PizzaRepository pizzaRepository = mock(PizzaRepository.class);
        OrderingService orderingService = mock(OrderingService.class);
        service = new DefaultKitchenService(eventLog, kitchenOrderRepository, pizzaRepository, orderingService);
    }

    @Test
    public void subscribes_to_ordering_topic() {
        verify(eventLog).subscribe(eq(new Topic("ordering")), isA(EventHandler.class));
    }

    @Test
    public void should_return_kitchenOrder_by_onlineOrderRef() {
        OnlineOrderRef onlineOrderRef = new OnlineOrderRef();

        KitchenOrder kitchenOrder = KitchenOrder.builder()
                .eventLog(eventLog)
                .onlineOrderRef(onlineOrderRef)
                .ref(new KitchenOrderRef())
                .build();

        when(kitchenOrderRepository.findByOnlineOrderRef(eq(onlineOrderRef))).thenReturn(kitchenOrder);

        assertThat(service.findKitchenOrderByOnlineOrderRef(onlineOrderRef)).isEqualTo(kitchenOrder);
    }

    @Test
    public void subscribes_to_kitchen_orders_topic() {
        verify(eventLog).subscribe(eq(new Topic("kitchen_orders")), isA(EventHandler.class));
    }

    @Test
    public void subscribes_to_pizzas_topic() {
        verify(eventLog).subscribe(eq(new Topic("pizzas")), isA(EventHandler.class));
    }

}
