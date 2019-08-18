package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.services.RefStringGenerator;
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

public class EmbeddedJdbcPizzaRepositoryIntegrationTests {

    private PizzaRepository repository;
    private InProcessEventLog eventLog;
    private PizzaRef ref;
    private Pizza pizza;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MVCC=FALSE", "", "");

        eventLog = InProcessEventLog.instance();
        repository = new EmbeddedJdbcPizzaRepository(eventLog,
                new Topic("pizzas"),
                pool);
        ref = repository.nextIdentity();
        pizza = Pizza.builder()
                .ref(ref)
                .size(Pizza.Size.MEDIUM)
                .kitchenOrderRef(new KitchenOrderRef(RefStringGenerator.generateRefString()))
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
    public void find_by_ref_hydrates_prepping_pizza() {
        repository.add(pizza);
        pizza.startPrep();
        assertThat(repository.findByRef(ref)).isEqualTo(pizza);
    }

    @Test
    public void find_by_ref_hydrates_prepped_pizza() {
        repository.add(pizza);
        pizza.startPrep();
        pizza.finishPrep();
        assertThat(repository.findByRef(ref)).isEqualTo(pizza);
    }

    @Test
    public void find_by_ref_hydrates_baking_pizza() {
        repository.add(pizza);
        pizza.startPrep();
        pizza.finishPrep();
        pizza.startBake();
        assertThat(repository.findByRef(ref)).isEqualTo(pizza);
    }

    @Test
    public void find_by_ref_hydrates_baked_pizza() {
        repository.add(pizza);
        pizza.startPrep();
        pizza.finishPrep();
        pizza.startBake();
        pizza.finishBake();
        assertThat(repository.findByRef(ref)).isEqualTo(pizza);
    }

}
