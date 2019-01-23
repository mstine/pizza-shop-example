package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.services.RefStringGenerator;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.ordering.acl.payments.PaymentRef;
import com.mattstine.dddworkshop.pizzashop.ordering.acl.payments.PaymentService;
import com.mattstine.dddworkshop.pizzashop.ordering.acl.payments.PaymentSuccessfulEvent;
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
public class DefaultOrderingServiceWithJdbcIntegrationTests {

    private InProcessEventLog eventLog;
    private OnlineOrderRepository repository;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        eventLog = InProcessEventLog.instance();
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MVCC=FALSE", "", "");
        repository = new EmbeddedJdbcOnlineOrderRepository(eventLog,
                new Topic("ordering"), pool);
        new DefaultOrderingService(eventLog, repository, mock(PaymentService.class));
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
        PaymentRef paymentRef = new PaymentRef(RefStringGenerator.generateRefString());
        onlineOrder.assignPaymentRef(paymentRef);

        eventLog.publish(new Topic("payments"), new PaymentSuccessfulEvent(paymentRef));

        onlineOrder = repository.findByRef(onlineOrderRef);
        assertThat(onlineOrder.isPaid()).isTrue();
    }
}
