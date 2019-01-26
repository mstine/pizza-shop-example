package com.mattstine.dddworkshop.pizzashop.kitchen;

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
import static org.mockito.Mockito.*;

public class EmbeddedJdbcPizzaRepositoryTests {

    private PizzaRepository repository;
    private EventLog eventLog;
    private PizzaRef ref;
    private Pizza pizza;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MVCC=FALSE", "", "");

        eventLog = mock(EventLog.class);
        repository = new EmbeddedJdbcPizzaRepository(eventLog,
                new Topic("pizzas"),
                pool);
        ref = repository.nextIdentity();
        pizza = Pizza.builder()
                .ref(ref)
                .size(Pizza.Size.MEDIUM)
                .kitchenOrderRef(new KitchenOrderRef())
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

        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM PIZZAS");
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        int count = resultSet.getInt(1);
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void add_stores_in_database() throws SQLException {
        repository.add(pizza);

        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT REF, SIZE, KITCHEN_ORDER_REF, STATE FROM PIZZAS WHERE REF = ?");
        statement.setString(1, pizza.getRef().getReference());
        ResultSet resultSet = statement.executeQuery();
        resultSet.first();
        String reference = resultSet.getString(1);
        assertThat(reference).isEqualTo(pizza.getRef().getReference());
        connection.close();
    }

    @Test
    public void provides_next_identity() {
        assertThat(ref).isNotNull();
    }

    @Test
    public void add_fires_event() {
        repository.add(pizza);
        PizzaAddedEvent event = new PizzaAddedEvent(ref, pizza.state());
        verify(eventLog).publish(eq(new Topic("pizzas")), eq(event));
    }

    @Test
    public void find_by_ref_hydrates_added_pizza() {
        repository.add(pizza);
        assertThat(repository.findByRef(ref)).isEqualTo(pizza);
    }

    @Test
    public void subscribes_to_pizzas_topic() {
        verify(eventLog).subscribe(eq(new Topic("pizzas")), isA(EventHandler.class));
    }

}
