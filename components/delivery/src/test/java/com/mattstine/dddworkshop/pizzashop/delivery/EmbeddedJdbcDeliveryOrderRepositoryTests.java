package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.services.RefStringGenerator;
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
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class EmbeddedJdbcDeliveryOrderRepositoryTests {

    private DeliveryOrderRepository repository;
    private EventLog eventLog;
    private DeliveryOrderRef ref;
    private DeliveryOrder deliveryOrder;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MVCC=FALSE", "", "");

        eventLog = mock(EventLog.class);
        repository = new EmbeddedJdbcDeliveryOrderRepository(eventLog,
                new Topic("delivery_orders"),
                pool);
        ref = repository.nextIdentity();
        deliveryOrder = DeliveryOrder.builder()
                .ref(ref)
                .kitchenOrderRef(new KitchenOrderRef(RefStringGenerator.generateRefString()))
                .onlineOrderRef(new OnlineOrderRef(RefStringGenerator.generateRefString()))
                .pizza(DeliveryOrder.Pizza.builder().size(DeliveryOrder.Pizza.Size.MEDIUM).build())
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
    public void should_bootstrap_schema() throws SQLException {
        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM DELIVERY_ORDERS");
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        int count = resultSet.getInt(1);
        assertThat(count).isEqualTo(0);

        statement = connection.prepareStatement("SELECT COUNT(*) FROM DELIVERY_ORDER_PIZZAS");
        resultSet = statement.executeQuery();
        resultSet.first();
        count = resultSet.getInt(1);
        assertThat(count).isEqualTo(0);

        connection.close();
    }

    @Test
    public void add_stores_in_database() throws SQLException {
        repository.add(deliveryOrder);

        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT REF, KITCHEN_ORDER_REF, ONLINE_ORDER_REF, STATE FROM DELIVERY_ORDERS WHERE REF = ?");
        statement.setString(1, deliveryOrder.getRef().getReference());
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        String reference = resultSet.getString(1);
        assertThat(reference).isEqualTo(deliveryOrder.getRef().getReference());

        statement = connection.prepareStatement("SELECT REF, INDEX INT, SIZE INT FROM DELIVERY_ORDER_PIZZAS WHERE REF = ?");
        statement.setString(1, deliveryOrder.getRef().getReference());
        resultSet = statement.executeQuery();
        resultSet.first();
        reference = resultSet.getString(1);
        assertThat(reference).isEqualTo(deliveryOrder.getRef().getReference());
        connection.close();
    }

    @Test
    public void provides_next_identity() {
        assertThat(ref).isNotNull();
    }

    @Test
    public void add_fires_event() {
        repository.add(deliveryOrder);
        DeliveryOrderAddedEvent event = new DeliveryOrderAddedEvent(ref, deliveryOrder.state());
        verify(eventLog).publish(eq(new Topic("delivery_orders")), eq(event));
    }

    @Test
    public void find_by_ref_hydrates_added_order() {
        repository.add(deliveryOrder);

        when(eventLog.eventsBy(new Topic("delivery_orders")))
                .thenReturn(Collections.singletonList(new DeliveryOrderAddedEvent(ref, deliveryOrder.state())));

        assertThat(repository.findByRef(ref)).isEqualTo(deliveryOrder);
    }
}
