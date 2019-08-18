package com.mattstine.dddworkshop.pizzashop.delivery;

import com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.delivery.acl.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class EmbeddedJdbcDeliveryOrderRepository implements DeliveryOrderRepository {


    private final EventLog eventLog;
    private final Topic topic;
    private final JdbcConnectionPool pool;

    public EmbeddedJdbcDeliveryOrderRepository(EventLog eventLog, Topic topic, JdbcConnectionPool pool) {
        this.eventLog = eventLog;
        this.topic = topic;
        this.pool = pool;

        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("CREATE TABLE DELIVERY_ORDERS (REF VARCHAR(255), KITCHEN_ORDER_REF VARCHAR(255), ONLINE_ORDER_REF VARCHAR(255), STATE INT)");
            statement.execute();

            statement = connection.prepareStatement("CREATE TABLE DELIVERY_ORDER_PIZZAS (REF VARCHAR(255), INDEX INT, SIZE INT)");
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initialize KITCHEN_ORDERS table: ", e);
        }
    }

    @Override
    public DeliveryOrderRef nextIdentity() {
        return new DeliveryOrderRef();
    }

    @Override
    public void add(DeliveryOrder deliveryOrder) {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO DELIVERY_ORDERS (REF, KITCHEN_ORDER_REF, ONLINE_ORDER_REF, STATE) VALUES (?, ?, ?, ?)");
            statement.setString(1, deliveryOrder.getRef().getReference());
            statement.setString(2, deliveryOrder.getKitchenOrderRef().getReference());
            statement.setString(3, deliveryOrder.getOnlineOrderRef().getReference());
            statement.setInt(4, deliveryOrder.getState().ordinal());
            statement.execute();

            statement = connection.prepareStatement("INSERT INTO DELIVERY_ORDER_PIZZAS (REF, INDEX, SIZE) VALUES (?, ?, ?)");
            for (int i = 0; i < deliveryOrder.getPizzas().size(); i++) {
                DeliveryOrder.Pizza pizza = deliveryOrder.getPizzas().get(i);
                statement.setString(1, deliveryOrder.getRef().getReference());
                statement.setInt(2, i);
                statement.setInt(3, pizza.getSize().ordinal());
                statement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert DeliveryOrder into DELIVERY_ORDERS table: ", e);
        }

        eventLog.publish(topic, new DeliveryOrderAddedEvent(deliveryOrder.getRef(), deliveryOrder.state()));
    }

    @Override
    public DeliveryOrder findByRef(DeliveryOrderRef ref) {
        DeliveryOrder deliveryOrder;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, KITCHEN_ORDER_REF, ONLINE_ORDER_REF, STATE FROM DELIVERY_ORDERS WHERE REF = ?");
            deliveryOrder = rehydrateDeliveryOrder(connection, statement, ref);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve DeliveryOrder from DELIVERY_ORDERS table: ", e);
        }
        return deliveryOrder;
    }

    @Override
    public DeliveryOrder findByKitchenOrderRef(KitchenOrderRef kitchenOrderRef) {
        DeliveryOrder deliveryOrder;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, KITCHEN_ORDER_REF, ONLINE_ORDER_REF, STATE FROM DELIVERY_ORDERS WHERE KITCHEN_ORDER_REF = ?");
            deliveryOrder = rehydrateDeliveryOrder(connection, statement, kitchenOrderRef);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve DeliveryOrder from DELIVERY_ORDERS table: ", e);
        }
        return deliveryOrder;
    }

    private DeliveryOrder rehydrateDeliveryOrder(Connection connection, PreparedStatement statement, Ref reference) throws SQLException {
        DeliveryOrder deliveryOrder = null;
        statement.setString(1, reference.getReference());
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.first()) {
            DeliveryOrderRef dor = new DeliveryOrderRef(resultSet.getString(1));
            KitchenOrderRef kor = new KitchenOrderRef(resultSet.getString(2));
            OnlineOrderRef onlineOrderRef = new OnlineOrderRef(resultSet.getString(3));
            int state = resultSet.getInt(4);

            statement = connection.prepareStatement("SELECT REF, INDEX, SIZE FROM DELIVERY_ORDER_PIZZAS WHERE REF = ? ORDER BY INDEX");
            statement.setString(1, dor.getReference());
            resultSet = statement.executeQuery();

            List<DeliveryOrder.Pizza> pizzas = new ArrayList<>();
            while (resultSet.next()) {
                int size = resultSet.getInt(3);
                pizzas.add(DeliveryOrder.Pizza.builder().size(DeliveryOrder.Pizza.Size.values()[size]).build());
            }

            deliveryOrder = DeliveryOrder.builder()
                    .ref(dor)
                    .kitchenOrderRef(kor)
                    .onlineOrderRef(onlineOrderRef)
                    .eventLog(eventLog)
                    .pizzas(pizzas)
                    .build();
            deliveryOrder.setState(DeliveryOrder.State.values()[state]);
        }
        return deliveryOrder;
    }
}
