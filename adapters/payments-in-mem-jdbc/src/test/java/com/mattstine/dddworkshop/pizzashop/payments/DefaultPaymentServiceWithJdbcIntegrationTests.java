package com.mattstine.dddworkshop.pizzashop.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes.Amount;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Matt Stine
 */
public class DefaultPaymentServiceWithJdbcIntegrationTests {

    private InProcessEventLog eventLog;
    private PaymentRepository repository;
    private PaymentProcessor processor;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        eventLog = InProcessEventLog.instance();
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test", "", "");
        repository = new EmbeddedJdbcPaymentRepository(eventLog,
                new Topic("payments"), pool);
        processor = mock(PaymentProcessor.class);
        new DefaultPaymentService(processor,
                repository,
                eventLog);
    }

    @After
    public void tearDown() throws SQLException {
        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("DROP ALL OBJECTS");
        statement.execute();
        connection.close();

        pool.dispose();
        eventLog.purgeSubscribers();
    }

    @Test
    public void on_successful_processing_mark_success() {
        PaymentRef ref = new PaymentRef();
        Payment payment = Payment.builder()
                .eventLog(eventLog)
                .paymentProcessor(processor)
                .amount(Amount.of(10, 0))
                .ref(ref)
                .build();
        repository.add(payment);
        payment.request();

        eventLog.publish(new Topic("payment_processor"), new PaymentProcessedEvent(ref, PaymentProcessedEvent.Status.SUCCESSFUL));

        payment = repository.findByRef(ref);
        assertThat(payment.isSuccessful()).isTrue();
    }

    @Test
    public void on_failed_processing_mark_failed() {
        PaymentRef ref = new PaymentRef();
        Payment payment = Payment.builder()
                .eventLog(eventLog)
                .paymentProcessor(processor)
                .amount(Amount.of(10, 0))
                .ref(ref)
                .build();
        repository.add(payment);
        payment.request();

        eventLog.publish(new Topic("payment_processor"), new PaymentProcessedEvent(ref, PaymentProcessedEvent.Status.FAILED));

        payment = repository.findByRef(ref);
        assertThat(payment.isFailed()).isTrue();
    }
}
