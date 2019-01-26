package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class EmbeddedJdbcPizzaRepository implements PizzaRepository {

    private final EventLog eventLog;
    private final Topic topic;
    private final JdbcConnectionPool pool;

    public EmbeddedJdbcPizzaRepository(EventLog eventLog, Topic topic, JdbcConnectionPool pool) {
        this.eventLog = eventLog;
        this.topic = topic;
        this.pool = pool;

        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection
                .prepareStatement("CREATE TABLE PIZZAS (REF VARCHAR(255), SIZE INT, KITCHEN_ORDER_REF VARCHAR(255), STATE INT)");
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initialize PIZZAS table: ", e);
        }

        eventLog.subscribe(new Topic("pizzas"), e -> {

        });
    }

    @Override
    public Set<Pizza> findPizzasByKitchenOrderRef(KitchenOrderRef kitchenOrderRef) {
        return null;
    }

    @Override
    public PizzaRef nextIdentity() {
        return new PizzaRef();
    }

    @Override
    public void add(Pizza pizza) {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO PIZZAS (REF, SIZE, KITCHEN_ORDER_REF, STATE) VALUES (?, ?, ?, ?)");
            statement.setString(1, pizza.getRef().getReference());
            statement.setInt(2, pizza.getSize().ordinal());
            statement.setString(3, pizza.getKitchenOrderRef().getReference());
            statement.setInt(4, pizza.getState().ordinal());
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert Pizza into PIZZAS table: ", e);
        }

        eventLog.publish(new Topic("pizzas"), new PizzaAddedEvent(pizza.getRef(), pizza.state()));
    }

    @Override
    public Pizza findByRef(PizzaRef ref) {
        Pizza pizza;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, SIZE, KITCHEN_ORDER_REF, STATE FROM PIZZAS WHERE REF = ?");
            pizza = rehydratePizza(connection, statement, ref);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve Pizza from PIZZAS table: ", e);
        }
        return pizza;
    }

    private Pizza rehydratePizza(Connection connection, PreparedStatement statement, Ref reference) throws SQLException {
        Pizza pizza = null;
        statement.setString(1, reference.getReference());
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.first()) {
            PizzaRef pizzaRef = new PizzaRef(resultSet.getString(1));
            int size = resultSet.getInt(2);
            KitchenOrderRef kitchenOrderRef = new KitchenOrderRef(resultSet.getString(3));
            int state = resultSet.getInt(4);

            pizza = Pizza.builder()
                    .ref(pizzaRef)
                    .size(Pizza.Size.values()[size])
                    .kitchenOrderRef(kitchenOrderRef)
                    .eventLog(eventLog)
                    .build();
            pizza.setState(Pizza.State.values()[state]);
        }
        return pizza;
    }
}
