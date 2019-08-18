package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventHandler;
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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Matt Stine
 */
public class EmbeddedJdbcOnlineOrderRepositoryTests {

    private EventLog eventLog;
    private OnlineOrderRepository repository;
    private OnlineOrderRef ref;
    private OnlineOrder onlineOrder;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test", "", "");

        eventLog = mock(EventLog.class);
        repository = new EmbeddedJdbcOnlineOrderRepository(eventLog,
                new Topic("ordering"),
                pool);
        ref = repository.nextIdentity();
        onlineOrder = OnlineOrder.builder()
                .ref(ref)
                .type(OnlineOrder.Type.PICKUP)
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
        repository.add(onlineOrder);
        OnlineOrderAddedEvent event = new OnlineOrderAddedEvent(onlineOrder.getRef(), onlineOrder.state());
        verify(eventLog).publish(eq(new Topic("ordering")), eq(event));
    }

    @Test
    public void should_bootstrap_schema() throws SQLException {
        Connection connection = pool.getConnection();

        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM ONLINE_ORDERS");
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        int count = resultSet.getInt(1);
        assertThat(count).isEqualTo(0);

        statement = connection.prepareStatement("SELECT COUNT(*) FROM ONLINE_ORDER_PIZZAS");
        resultSet = statement.executeQuery();
        resultSet.first();
        count = resultSet.getInt(1);
        assertThat(count).isEqualTo(0);

        connection.close();
    }

    @Test
    public void add_stores_in_database() throws SQLException {
        repository.add(onlineOrder);

        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT REF, TYPE, STATE FROM ONLINE_ORDERS WHERE REF = ?");
        statement.setString(1, onlineOrder.getRef().getReference());
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        String reference = resultSet.getString(1);
        assertThat(reference).isEqualTo(onlineOrder.getRef().getReference());
        connection.close();
    }

    @Test
    public void find_by_ref_hydrates_online_order() {
        repository.add(onlineOrder);
        assertThat(repository.findByRef(ref)).isEqualTo(onlineOrder);
    }

    @Test
    public void subscribes_to_ordering_topic() {
        verify(eventLog).subscribe(eq(new Topic("ordering")), isA(EventHandler.class));
    }

}
