package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes.Amount;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventHandler;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.payments.PaymentRef;
import com.mattstine.dddworkshop.pizzashop.payments.PaymentService;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Matt Stine
 */
public class DefaultOrderingServiceTests {

    private EventLog eventLog;
    private OnlineOrderRepository repository;
    private OrderingService orderingService;
    private PaymentService paymentService;

    @Before
    public void setUp() {
        eventLog = mock(EventLog.class);
        repository = mock(OnlineOrderRepository.class);
        paymentService = mock(PaymentService.class);
        orderingService = new DefaultOrderingService(eventLog, repository, paymentService);
    }

    @Test
    public void subscribes_to_payments_topic() {
        verify(eventLog).subscribe(eq(new Topic("payments")), isA(EventHandler.class));
    }

    @Test
    public void adds_new_order_to_repository() {
        when(repository.nextIdentity()).thenReturn(new OnlineOrderRef());
        orderingService.createOrder(OnlineOrder.Type.PICKUP);
        verify(repository).add(isA(OnlineOrder.class));
    }

    @Test
    public void returns_ref_to_new_order() {
        OnlineOrderRef ref = new OnlineOrderRef();
        when(repository.nextIdentity()).thenReturn(ref);
        OnlineOrderRef onlineOrderRef = orderingService.createOrder(OnlineOrder.Type.PICKUP);
        assertThat(onlineOrderRef).isEqualTo(onlineOrderRef);
    }

    @Test
    public void adds_pizza_to_order() {
        OnlineOrderRef onlineOrderRef = new OnlineOrderRef();
        OnlineOrder onlineOrder = OnlineOrder.builder()
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .ref(onlineOrderRef)
                .build();

        when(repository.findByRef(onlineOrderRef)).thenReturn(onlineOrder);

        Pizza pizza = Pizza.builder().size(Pizza.Size.MEDIUM).build();
        orderingService.addPizza(onlineOrderRef, pizza);

        assertThat(onlineOrder.getPizzas()).contains(pizza);
    }

    @Test
    public void requests_payment_for_order() {
        OnlineOrderRef onlineOrderRef = new OnlineOrderRef();
        OnlineOrder onlineOrder = OnlineOrder.builder()
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .ref(onlineOrderRef)
                .build();
        when(repository.findByRef(onlineOrderRef)).thenReturn(onlineOrder);

        PaymentRef paymentRef = new PaymentRef();
        when(paymentService.createPaymentOf(Amount.of(10, 0))).thenReturn(paymentRef);

        orderingService.requestPayment(onlineOrderRef);
        assertThat(onlineOrder.getPaymentRef()).isEqualTo(paymentRef);

        verify(paymentService).requestPaymentFor(eq(paymentRef));
    }

    @Test
    public void should_return_order_by_ref() {
        OnlineOrderRef onlineOrderRef = new OnlineOrderRef();
        OnlineOrder onlineOrder = OnlineOrder.builder()
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .ref(onlineOrderRef)
                .build();
        when(repository.findByRef(onlineOrderRef)).thenReturn(onlineOrder);

        assertThat(orderingService.findByRef(onlineOrderRef)).isEqualTo(onlineOrder);
    }

}
