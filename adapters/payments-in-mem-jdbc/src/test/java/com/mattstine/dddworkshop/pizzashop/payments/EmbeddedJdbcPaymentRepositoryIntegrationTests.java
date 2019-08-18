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
public class EmbeddedJdbcPaymentRepositoryIntegrationTests {
    private InProcessEventLog eventLog;
    private PaymentRepository repository;
    private PaymentRef ref;
    private Payment payment;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test", "", "");

        eventLog = InProcessEventLog.instance();
        repository = new EmbeddedJdbcPaymentRepository(eventLog,
                new Topic("payments"),
                pool);
        ref = repository.nextIdentity();
        payment = Payment.builder()
                .ref(ref)
                .amount(Amount.of(10, 0))
                .paymentProcessor(mock(PaymentProcessor.class))
                .eventLog(eventLog)
                .build();
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
    public void find_by_ref_hydrates_requested_payment() {
        repository.add(payment);
        payment.request();

        assertThat(repository.findByRef(ref)).isEqualTo(payment);
    }

    @Test
    public void find_by_ref_hydrates_successful_payment() {
        repository.add(payment);
        payment.request();
        payment.markSuccessful();

        assertThat(repository.findByRef(ref)).isEqualTo(payment);
    }

    @Test
    public void find_by_ref_hydrates_failed_payment() {
        repository.add(payment);
        payment.request();
        payment.markFailed();

        assertThat(repository.findByRef(ref)).isEqualTo(payment);
    }
}
