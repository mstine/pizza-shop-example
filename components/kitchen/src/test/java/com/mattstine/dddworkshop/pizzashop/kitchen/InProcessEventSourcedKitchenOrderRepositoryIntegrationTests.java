package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InProcessEventSourcedKitchenOrderRepositoryIntegrationTests {

    private KitchenOrderRepository repository;
    private InProcessEventLog eventLog;
    private KitchenOrder kitchenOrder;
    private OnlineOrderRef onlineOrderRef;

    @Before
    public void setUp() {
        eventLog = InProcessEventLog.instance();
        repository = new InProcessEventSourcedKitchenOrderRepository(eventLog,
                new Topic("kitchen_orders"));
        KitchenOrderRef ref = repository.nextIdentity();
        onlineOrderRef = new OnlineOrderRef();
        kitchenOrder = KitchenOrder.builder()
                .ref(ref)
                .onlineOrderRef(onlineOrderRef)
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
                .eventLog(eventLog)
                .build();
    }

    @After
    public void tearDown() {
        this.eventLog.purgeSubscribers();
    }

    @Test
    public void find_by_onlineOrderRef_hydrates_kitchenOrder() {
        repository.add(kitchenOrder);

        assertThat(repository.findByOnlineOrderRef(onlineOrderRef)).isEqualTo(kitchenOrder);
    }

}
