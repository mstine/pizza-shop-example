package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrder;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderPaidEvent;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OrderingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultKitchenServiceWithEventSourcingIntegrationTests {

    private InProcessEventLog eventLog;
    private KitchenService kitchenService;
    private OrderingService orderingService;
    private KitchenOrderRepository kitchenOrderRepository;
    private PizzaRepository pizzaRepository;
    private KitchenOrderRef kitchenOrderRef;
    private KitchenOrder kitchenOrder;

    @Before
    public void setUp() {
        eventLog = InProcessEventLog.instance();
        kitchenOrderRepository = new InProcessEventSourcedKitchenOrderRepository(eventLog,
                new Topic("kitchen_orders"));
        pizzaRepository = new InProcessEventSourcedPizzaRepository(eventLog,
                new Topic("pizzas"));
        orderingService = mock(OrderingService.class);
        kitchenService = new DefaultKitchenService(eventLog, kitchenOrderRepository, pizzaRepository, orderingService);
        kitchenOrderRef = kitchenOrderRepository.nextIdentity();
        kitchenOrder = KitchenOrder.builder()
                .ref(kitchenOrderRef)
                .onlineOrderRef(new OnlineOrderRef())
                .eventLog(eventLog)
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.LARGE).build())
                .build();
        kitchenOrderRepository.add(kitchenOrder);
    }

    @After
    public void tearDown() {
        this.eventLog.purgeSubscribers();
    }

    @Test
    public void on_orderPaidEvent_add_to_queue() {
        OnlineOrderRef ref = new OnlineOrderRef();
        OnlineOrderPaidEvent orderPaidEvent = new OnlineOrderPaidEvent(ref);

        OnlineOrder onlineOrder = OnlineOrder.builder()
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .ref(ref)
                .build();

        onlineOrder.addPizza(com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.Pizza.builder()
                .size(com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.Pizza.Size.MEDIUM)
                .build());

        when(orderingService.findByRef(eq(ref))).thenReturn(onlineOrder);

        eventLog.publish(new Topic("ordering"), orderPaidEvent);

        KitchenOrder kitchenOrder = kitchenService.findKitchenOrderByOnlineOrderRef(ref);
        assertThat(kitchenOrder).isNotNull();
    }

    @Test
    public void on_kitchenOrderPrepStartedEvent_start_prep_on_all_pizzas() {
        kitchenOrder.startPrep();

        Set<Pizza> pizzas = kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef);

        assertThat(pizzas.size()).isEqualTo(2);

        assertThat(pizzas.stream()
                .filter(pizza -> pizza.getSize() == Pizza.Size.MEDIUM)
                .count()).isEqualTo(1);

        assertThat(pizzas.stream()
                .filter(pizza -> pizza.getSize() == Pizza.Size.LARGE)
                .count()).isEqualTo(1);

        assertThat(pizzas.stream()
                .filter(pizza -> pizza.getState() == Pizza.State.PREPPING)
                .count()).isEqualTo(2);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void on_pizzaPrepFinished_start_pizzaBake() {
        kitchenOrder.startPrep();

        Set<Pizza> pizzasByKitchenOrderRef = kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef);

        Pizza pizza = pizzasByKitchenOrderRef.stream()
                .findFirst()
                .get();

        pizza.finishPrep();

        pizza = pizzaRepository.findByRef(pizza.getRef());
        assertThat(pizza.isBaking()).isTrue();
    }

    @Test
    public void on_pizzaBakeStarted_start_orderBake() {
        kitchenOrder.startPrep();

        Set<Pizza> pizzasByKitchenOrderRef = kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef);

        pizzasByKitchenOrderRef.forEach(Pizza::finishPrep);

        kitchenOrder = kitchenOrderRepository.findByRef(kitchenOrderRef);
        assertThat(kitchenOrder.isBaking()).isTrue();
    }

    @Test
    public void on_first_pizzaBakeFinished_start_orderAssembly() {
        kitchenOrder.startPrep();

        // Load pizzas that are prepping...
        Set<Pizza> pizzasByKitchenOrderRef = kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef);
        pizzasByKitchenOrderRef.forEach(Pizza::finishPrep);

        // Load pizzas that are baking...
        pizzasByKitchenOrderRef = kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef);
        pizzasByKitchenOrderRef.stream()
                .findFirst()
                .ifPresent(Pizza::finishBake);

        // Ensure order has started assembly...
        kitchenOrder = kitchenOrderRepository.findByRef(kitchenOrderRef);
        assertThat(kitchenOrder.hasStartedAssembly()).isTrue();
    }

    @Test
    public void should_start_kitchenOrder_prep() {
        kitchenService.startOrderPrep(kitchenOrderRef);
        kitchenOrder = kitchenService.findKitchenOrderByRef(kitchenOrderRef);
        assertThat(kitchenOrder.isPrepping()).isTrue();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void should_finish_pizza_prep() {
        kitchenOrder.startPrep();

        Pizza pizza = kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef).stream()
                .findFirst().get();

        PizzaRef ref = pizza.getRef();
        kitchenService.finishPizzaPrep(ref);
        pizza = kitchenService.findPizzaByRef(ref);

        assertThat(pizza.isBaking()).isTrue();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void should_remove_pizza_from_oven() {
        kitchenOrder.startPrep();

        Pizza pizza = kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef).stream()
                .findFirst().get();

        pizza.finishPrep();

        kitchenService.removePizzaFromOven(pizza.getRef());

        pizza = kitchenService.findPizzaByRef(pizza.getRef());

        assertThat(pizza.hasFinishedBaking()).isTrue();
    }

    @Test
    public void on_final_pizzaBakeFinished_finish_orderAssembly() {
        kitchenOrder.startPrep();

        kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef)
                .forEach(Pizza::finishPrep);

        kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef)
                .forEach(pizza -> kitchenService.removePizzaFromOven(pizza.getRef()));

        kitchenOrder = kitchenService.findKitchenOrderByRef(kitchenOrderRef);

        assertThat(kitchenOrder.hasFinishedAssembly()).isTrue();
    }

    @Test
    public void test_bug_only_one_pizza_would_never_finish() {
        kitchenOrderRef = kitchenOrderRepository.nextIdentity();

        kitchenOrder = KitchenOrder.builder()
                .ref(kitchenOrderRef)
                .onlineOrderRef(new OnlineOrderRef())
                .eventLog(eventLog)
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .build();
        kitchenOrderRepository.add(kitchenOrder);


        kitchenOrder.startPrep();

        kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef)
                .forEach(Pizza::finishPrep);

        kitchenService.findPizzasByKitchenOrderRef(kitchenOrderRef)
                .forEach(pizza -> kitchenService.removePizzaFromOven(pizza.getRef()));

        kitchenOrder = kitchenService.findKitchenOrderByRef(kitchenOrderRef);

        assertThat(kitchenOrder.hasFinishedAssembly()).isTrue();
    }
}
