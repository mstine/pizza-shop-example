package com.mattstine.dddworkshop.pizzashop.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes.Amount;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Matt Stine
 */
public class InProcessEventSourcedPaymentRepositoryTests {

    private PaymentRepository repository;
    private EventLog eventLog;
    private PaymentRef ref;
    private Payment payment;

    @Before
    public void setUp() {
        eventLog = mock(EventLog.class);
        repository = new InProcessEventSourcedPaymentRepository(eventLog,
                new Topic("payments"));
        ref = repository.nextIdentity();
        payment = Payment.builder()
                .ref(ref)
                .amount(Amount.of(10, 0))
                .paymentProcessor(mock(PaymentProcessor.class))
                .eventLog(eventLog)
                .build();
    }

    @Test
    public void provides_next_identity() {
        assertThat(ref).isNotNull();
    }

    @Test
    public void add_fires_event() {
        repository.add(payment);
        PaymentAddedEvent event = new PaymentAddedEvent(payment.getRef(), payment.state());
        verify(eventLog).publish(eq(new Topic("payments")), eq(event));
    }

    @Test
    public void find_by_ref_hydrates_added_payment() {
        repository.add(payment);

        when(eventLog.eventsBy(new Topic("payments")))
                .thenReturn(Collections.singletonList(new PaymentAddedEvent(ref, payment.state())));


        assertThat(repository.findByRef(ref)).isEqualTo(payment);
    }

    @Test
    public void find_by_ref_hydrates_requested_payment() {
        repository.add(payment);
        payment.request();

        when(eventLog.eventsBy(new Topic("payments")))
                .thenReturn(Arrays.asList(new PaymentAddedEvent(ref, payment.state()),
                        new PaymentRequestedEvent(ref)));

        assertThat(repository.findByRef(ref)).isEqualTo(payment);
    }

    @Test
    public void find_by_ref_hydrates_successful_payment() {
        repository.add(payment);
        payment.request();
        payment.markSuccessful();

        when(eventLog.eventsBy(new Topic("payments")))
                .thenReturn(Arrays.asList(new PaymentAddedEvent(ref, payment.state()),
                        new PaymentRequestedEvent(ref),
                        new PaymentSuccessfulEvent(ref)));

        assertThat(repository.findByRef(ref)).isEqualTo(payment);
    }

    @Test
    public void find_by_ref_hydrates_failed_payment() {
        repository.add(payment);
        payment.request();
        payment.markFailed();

        when(eventLog.eventsBy(new Topic("payments")))
                .thenReturn(Arrays.asList(new PaymentAddedEvent(ref, payment.state()),
                        new PaymentRequestedEvent(ref),
                        new PaymentFailedEvent(ref)));

        assertThat(repository.findByRef(ref)).isEqualTo(payment);
    }
}
