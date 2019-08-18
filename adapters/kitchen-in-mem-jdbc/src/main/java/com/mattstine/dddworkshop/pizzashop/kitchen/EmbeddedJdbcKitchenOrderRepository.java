package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderRef;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class EmbeddedJdbcKitchenOrderRepository implements KitchenOrderRepository {
    private final EventLog eventLog;
    private final Topic topic;
    private final JdbcConnectionPool pool;

    EmbeddedJdbcKitchenOrderRepository(EventLog eventLog, Topic topic, JdbcConnectionPool pool) {
        this.eventLog = eventLog;
        this.topic = topic;
        this.pool = pool;

        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("CREATE TABLE KITCHEN_ORDERS (REF VARCHAR(255), ONLINE_ORDER_REF VARCHAR(255), STATE INT)");
            statement.execute();

            statement = connection.prepareStatement("CREATE TABLE KITCHEN_ORDER_PIZZAS (REF VARCHAR(255), INDEX INT, SIZE INT)");
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initialize KITCHEN_ORDERS table: ", e);
        }

        eventLog.subscribe(topic, e -> {
            if (e instanceof KitchenOrderPrepStartedEvent) {
                KitchenOrderPrepStartedEvent kopse = (KitchenOrderPrepStartedEvent) e;

                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE KITCHEN_ORDERS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, KitchenOrder.State.PREPPING.ordinal());
                    statement.setString(2, kopse.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in KITCHEN_ORDERS table: ", ex);
                }
            } else if (e instanceof KitchenOrderBakeStartedEvent) {
                KitchenOrderBakeStartedEvent kobse = (KitchenOrderBakeStartedEvent) e;

                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE KITCHEN_ORDERS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, KitchenOrder.State.BAKING.ordinal());
                    statement.setString(2, kobse.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in KITCHEN_ORDERS table: ", ex);
                }
            } else if (e instanceof KitchenOrderAssemblyStartedEvent) {
                KitchenOrderAssemblyStartedEvent koase = (KitchenOrderAssemblyStartedEvent) e;

                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE KITCHEN_ORDERS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, KitchenOrder.State.ASSEMBLING.ordinal());
                    statement.setString(2, koase.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in KITCHEN_ORDERS table: ", ex);
                }
            } else if (e instanceof KitchenOrderAssemblyFinishedEvent) {
                KitchenOrderAssemblyFinishedEvent koafe = (KitchenOrderAssemblyFinishedEvent) e;

                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE KITCHEN_ORDERS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, KitchenOrder.State.ASSEMBLED.ordinal());
                    statement.setString(2, koafe.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in KITCHEN_ORDERS table: ", ex);
                }
            }
        });
    }

    @Override
    public KitchenOrder findByRef(KitchenOrderRef ref) {
        KitchenOrder kitchenOrder;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, ONLINE_ORDER_REF, STATE FROM KITCHEN_ORDERS WHERE REF = ?");
            kitchenOrder = rehydrateKitchenOrder(connection, statement, ref);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve KitchenOrder from KITCHEN_ORDERS table: ", e);
        }
        return kitchenOrder;
    }

    @Override
    public KitchenOrder findByOnlineOrderRef(OnlineOrderRef ref) {
        KitchenOrder kitchenOrder;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, ONLINE_ORDER_REF, STATE FROM KITCHEN_ORDERS WHERE ONLINE_ORDER_REF = ?");
            kitchenOrder = rehydrateKitchenOrder(connection, statement, ref);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve KitchenOrder from KITCHEN_ORDERS table: ", e);
        }
        return kitchenOrder;
    }

    private KitchenOrder rehydrateKitchenOrder(Connection connection, PreparedStatement statement, Ref reference) throws SQLException {
        KitchenOrder kitchenOrder = null;
        statement.setString(1, reference.getReference());
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.first()) {
            KitchenOrderRef kitchenOrderRef = new KitchenOrderRef(resultSet.getString(1));
            OnlineOrderRef onlineOrderRef = new OnlineOrderRef(resultSet.getString(2));
            int state = resultSet.getInt(3);

            statement = connection.prepareStatement("SELECT REF, INDEX, SIZE FROM KITCHEN_ORDER_PIZZAS WHERE REF = ? ORDER BY INDEX");
            statement.setString(1, kitchenOrderRef.getReference());
            resultSet = statement.executeQuery();

            List<KitchenOrder.Pizza> pizzas = new ArrayList<>();
            while (resultSet.next()) {
                int size = resultSet.getInt(3);
                pizzas.add(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.values()[size]).build());
            }

            kitchenOrder = KitchenOrder.builder()
                    .ref(kitchenOrderRef)
                    .onlineOrderRef(onlineOrderRef)
                    .eventLog(eventLog)
                    .pizzas(pizzas)
                    .build();
            kitchenOrder.setState(KitchenOrder.State.values()[state]);

        }
        return kitchenOrder;
    }

    @Override
    public KitchenOrderRef nextIdentity() {
        return new KitchenOrderRef();
    }

    @Override
    public void add(KitchenOrder kitchenOrder) {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO KITCHEN_ORDERS (REF, ONLINE_ORDER_REF, STATE) VALUES (?, ?, ?)");
            statement.setString(1, kitchenOrder.getRef().getReference());
            statement.setString(2, kitchenOrder.getOnlineOrderRef().getReference());
            statement.setInt(3, kitchenOrder.getState().ordinal());
            statement.execute();

            statement = connection.prepareStatement("INSERT INTO KITCHEN_ORDER_PIZZAS (REF, INDEX, SIZE) VALUES (?, ?, ?)");
            for (int i = 0; i < kitchenOrder.getPizzas().size(); i++) {
                KitchenOrder.Pizza pizza = kitchenOrder.getPizzas().get(i);
                statement.setString(1, kitchenOrder.getRef().getReference());
                statement.setInt(2, i);
                statement.setInt(3, pizza.getSize().ordinal());
                statement.execute();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert KitchenOrder into KITCHEN_ORDERS table: ", e);
        }

        this.eventLog.publish(topic, new KitchenOrderAddedEvent(kitchenOrder.getRef(), kitchenOrder.state()));
    }

}
