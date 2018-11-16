package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.payments.PaymentRef;
import com.mattstine.dddworkshop.pizzashop.payments.PaymentService;
import com.mattstine.dddworkshop.pizzashop.payments.PaymentSuccessfulEvent;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Matt Stine
 */
public class DefaultOrderingServiceIntegrationTests {

    private EventLog eventLog;
    private OnlineOrderRepository repository;

    @Before
    public void setUp() {
        eventLog = InProcessEventLog.instance();
        repository = new InProcessEventSourcedOnlineOrderRepository(eventLog,
                new Topic("ordering"));
        new DefaultOrderingService(eventLog, repository, mock(PaymentService.class));
    }

    @Test
    public void on_successful_payment_mark_paid() {
        OnlineOrderRef onlineOrderRef = new OnlineOrderRef();
        OnlineOrder onlineOrder = OnlineOrder.builder()
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .ref(onlineOrderRef)
                .build();
        repository.add(onlineOrder);
        onlineOrder.addPizza(Pizza.builder().size(Pizza.Size.MEDIUM).build());
        onlineOrder.submit();
        PaymentRef paymentRef = new PaymentRef();
        onlineOrder.assignPaymentRef(paymentRef);

        eventLog.publish(new Topic("payments"), new PaymentSuccessfulEvent(paymentRef));

        onlineOrder = repository.findByRef(onlineOrderRef);
        assertThat(onlineOrder.isPaid()).isTrue();
    }
}
