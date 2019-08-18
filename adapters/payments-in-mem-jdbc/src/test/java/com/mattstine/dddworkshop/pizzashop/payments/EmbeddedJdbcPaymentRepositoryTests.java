package com.mattstine.dddworkshop.pizzashop.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes.Amount;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Matt Stine
 */
public class EmbeddedJdbcPaymentRepositoryTests {

    private EventLog eventLog;
    private PaymentRepository repository;
    private PaymentRef ref;
    private Payment payment;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test", "", "");

        eventLog = mock(EventLog.class);
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
    public void should_bootstrap_schema() throws SQLException {
        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM PAYMENTS");
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        int count = resultSet.getInt(1);
        assertThat(count).isEqualTo(0);
        connection.close();
    }

    @Test
    public void add_stores_in_database() throws SQLException {
        repository.add(payment);

        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT REF, DOLLARS, CENTS FROM PAYMENTS WHERE REF = ?");
        statement.setString(1, payment.getRef().getReference());
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        String reference = resultSet.getString(1);
        assertThat(reference).isEqualTo(payment.getRef().getReference());
        connection.close();
    }

    @Test
    public void find_by_ref_hydrates_payment() {
        repository.add(payment);
        assertThat(repository.findByRef(ref)).isEqualTo(payment);
    }

}
