package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

final class EmbeddedJdbcPizzaRepository implements PizzaRepository {

    private final EventLog eventLog;
    private final Topic topic;
    private final JdbcConnectionPool pool;

    EmbeddedJdbcPizzaRepository(EventLog eventLog, Topic topic, JdbcConnectionPool pool) {
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

        eventLog.subscribe(topic, e -> {
            if (e instanceof PizzaPrepStartedEvent) {
                PizzaPrepStartedEvent ppse = (PizzaPrepStartedEvent) e;
                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE PIZZAS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, Pizza.State.PREPPING.ordinal());
                    statement.setString(2, ppse.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in PIZZAS table: ", ex);
                }
            } else if (e instanceof PizzaPrepFinishedEvent) {
                PizzaPrepFinishedEvent ppfe = (PizzaPrepFinishedEvent) e;
                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE PIZZAS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, Pizza.State.PREPPED.ordinal());
                    statement.setString(2, ppfe.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in PIZZAS table: ", ex);
                }
            } else if (e instanceof PizzaBakeStartedEvent) {
                PizzaBakeStartedEvent pbse = (PizzaBakeStartedEvent) e;
                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE PIZZAS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, Pizza.State.BAKING.ordinal());
                    statement.setString(2, pbse.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in PIZZAS table: ", ex);
                }
            } else if (e instanceof PizzaBakeFinishedEvent) {
                PizzaBakeFinishedEvent pbfe = (PizzaBakeFinishedEvent) e;
                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE PIZZAS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, Pizza.State.BAKED.ordinal());
                    statement.setString(2, pbfe.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in PIZZAS table: ", ex);
                }
            }
        });
    }

    @Override
    public Set<Pizza> findPizzasByKitchenOrderRef(KitchenOrderRef kitchenOrderRef) {
        Set<Pizza> pizzas = new HashSet<>();
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, SIZE, KITCHEN_ORDER_REF, STATE FROM PIZZAS WHERE KITCHEN_ORDER_REF = ?");
            statement.setString(1, kitchenOrderRef.getReference());
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                pizzas.add(buildPizza(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve Pizzas from PIZZAS table: ", e);
        }
        return pizzas;
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

        eventLog.publish(topic, new PizzaAddedEvent(pizza.getRef(), pizza.state()));
    }

    @Override
    public Pizza findByRef(PizzaRef ref) {
        Pizza pizza;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, SIZE, KITCHEN_ORDER_REF, STATE FROM PIZZAS WHERE REF = ?");
            pizza = rehydratePizza(statement, ref);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve Pizza from PIZZAS table: ", e);
        }
        return pizza;
    }

    private Pizza rehydratePizza(PreparedStatement statement, Ref reference) throws SQLException {
        Pizza pizza = null;
        statement.setString(1, reference.getReference());
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.first()) {
            pizza = buildPizza(resultSet);
        }
        return pizza;
    }

    private Pizza buildPizza(ResultSet resultSet) throws SQLException {
        Pizza pizza;
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
        return pizza;
    }
}
