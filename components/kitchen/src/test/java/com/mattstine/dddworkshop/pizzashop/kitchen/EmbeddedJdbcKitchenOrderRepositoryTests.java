package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventHandler;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderRef;
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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EmbeddedJdbcKitchenOrderRepositoryTests {

    private KitchenOrderRepository repository;
    private EventLog eventLog;
    private KitchenOrderRef ref;
    private KitchenOrder kitchenOrder;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test", "", "");

        eventLog = mock(EventLog.class);
        repository = new EmbeddedJdbcKitchenOrderRepository(eventLog,
                new Topic("kitchen_orders"),
                pool);
        ref = repository.nextIdentity();
        kitchenOrder = KitchenOrder.builder()
                .ref(ref)
                .onlineOrderRef(new OnlineOrderRef())
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
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
        repository.add(kitchenOrder);
        KitchenOrderAddedEvent event = new KitchenOrderAddedEvent(ref, kitchenOrder.state());
        verify(eventLog).publish(eq(new Topic("kitchen_orders")), eq(event));
    }

    @Test
    public void should_bootstrap_schema() throws SQLException {
        Connection connection = pool.getConnection();

        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM KITCHEN_ORDERS");
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        int count = resultSet.getInt(1);
        assertThat(count).isEqualTo(0);

        statement = connection.prepareStatement("SELECT COUNT(*) FROM KITCHEN_ORDER_PIZZAS");
        resultSet = statement.executeQuery();
        resultSet.first();
        count = resultSet.getInt(1);
        assertThat(count).isEqualTo(0);

        connection.close();
    }

    @Test
    public void add_stores_in_database() throws SQLException {
        repository.add(kitchenOrder);

        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT REF, ONLINE_ORDER_REF, STATE FROM KITCHEN_ORDERS WHERE REF = ?");
        statement.setString(1, kitchenOrder.getRef().getReference());
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        String reference = resultSet.getString(1);
        assertThat(reference).isEqualTo(kitchenOrder.getRef().getReference());
        connection.close();
    }

    @Test
    public void find_by_ref_hydrates_added_order() {
        repository.add(kitchenOrder);
        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }

    @Test
    public void subscribes_to_kitchen_orders_topic() {
        verify(eventLog).subscribe(eq(new Topic("kitchen_orders")), isA(EventHandler.class));
    }
}
