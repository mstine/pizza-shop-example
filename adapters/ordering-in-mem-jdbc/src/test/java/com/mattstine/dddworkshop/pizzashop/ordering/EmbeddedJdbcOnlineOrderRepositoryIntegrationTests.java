package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.services.RefStringGenerator;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.ordering.acl.payments.PaymentRef;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Matt Stine
 */
public class EmbeddedJdbcOnlineOrderRepositoryIntegrationTests {
    private InProcessEventLog eventLog;
    private OnlineOrderRepository repository;
    private OnlineOrderRef ref;
    private OnlineOrder onlineOrder;
    private JdbcConnectionPool pool;
    private Pizza pizza;

    @Before
    public void setUp() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MVCC=FALSE", "", "");

        eventLog = InProcessEventLog.instance();
        repository = new EmbeddedJdbcOnlineOrderRepository(eventLog,
                new Topic("ordering"),
                pool);
        ref = repository.nextIdentity();
        onlineOrder = OnlineOrder.builder()
                .ref(ref)
                .type(OnlineOrder.Type.PICKUP)
                .eventLog(eventLog)
                .build();
        pizza = Pizza.builder().size(Pizza.Size.MEDIUM).build();
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
    public void find_by_ref_hydrates_order_with_added_pizza() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.addPizza(Pizza.builder().size(Pizza.Size.LARGE).build());

        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void find_by_ref_hydrates_submitted_order() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();

        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void find_by_ref_hydrates_order_with_paymentRef_assigned() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();

        PaymentRef paymentRef = new PaymentRef(RefStringGenerator.generateRefString());
        onlineOrder.assignPaymentRef(paymentRef);

        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void find_by_ref_hydrates_order_marked_paid() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();

        PaymentRef paymentRef = new PaymentRef(RefStringGenerator.generateRefString());
        onlineOrder.assignPaymentRef(paymentRef);

        onlineOrder.markPaid();

        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void find_by_paymentRef_hydrates_order() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();

        PaymentRef paymentRef = new PaymentRef(RefStringGenerator.generateRefString());
        onlineOrder.assignPaymentRef(paymentRef);

        assertThat(repository.findByPaymentRef(paymentRef)).isEqualTo(onlineOrder);
    }
}
