package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.ordering.acl.payments.PaymentRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Matt Stine
 */
public class InProcessEventSourcedOnlineOnlineOrderRepositoryIntegrationTests {

    private OnlineOrderRepository repository;
    private OnlineOrder onlineOrder;
    private Pizza pizza;
    private InProcessEventLog eventLog;

    @Before
    public void setUp() {
        eventLog = InProcessEventLog.instance();
        repository = new InProcessEventSourcedOnlineOrderRepository(eventLog,
                new Topic("ordering"));
        OnlineOrderRef ref = repository.nextIdentity();
        onlineOrder = OnlineOrder.builder()
                .ref(ref)
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .build();
        pizza = Pizza.builder().size(Pizza.Size.MEDIUM).build();
    }

    @After
    public void tearDown() {
        this.eventLog.purgeSubscribers();
    }

    @Test
    public void find_by_paymentRef_hydrates_order() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();

        PaymentRef paymentRef = new PaymentRef();
        onlineOrder.assignPaymentRef(paymentRef);

        assertThat(repository.findByPaymentRef(paymentRef)).isEqualTo(onlineOrder);
    }
}
