package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Matt Stine
 */
public class InProcessEventSourcedDeliveryOrderRepositoryIntegrationTests {

    private DeliveryOrderRepository repository;
    private InProcessEventLog eventLog;
    private DeliveryOrder deliveryOrder;
    private KitchenOrderRef kitchenOrderRef;

    @Before
    public void setUp() {
        eventLog = InProcessEventLog.instance();
        repository = new InProcessEventSourcedDeliveryOrderRepository(eventLog,
                new Topic("delivery_orders"));
        DeliveryOrderRef ref = repository.nextIdentity();
        kitchenOrderRef = new KitchenOrderRef();
        deliveryOrder = DeliveryOrder.builder()
                .ref(ref)
                .kitchenOrderRef(kitchenOrderRef)
                .onlineOrderRef(new OnlineOrderRef())
                .pizza(DeliveryOrder.Pizza.builder().size(DeliveryOrder.Pizza.Size.MEDIUM).build())
                .eventLog(eventLog)
                .build();
    }

    @After
    public void tearDown() {
        this.eventLog.purgeSubscribers();
    }

    @Test
    public void find_by_kitchenOrderRef_hydrates_deliveryOrder() {
        repository.add(deliveryOrder);

        assertThat(repository.findByKitchenOrderRef(kitchenOrderRef)).isEqualTo(deliveryOrder);
    }
}
