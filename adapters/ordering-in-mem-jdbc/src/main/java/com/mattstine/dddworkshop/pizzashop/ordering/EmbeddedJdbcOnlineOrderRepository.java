package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;
import com.mattstine.dddworkshop.pizzashop.ordering.acl.payments.PaymentRef;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * @author Matt Stine
 */
final class EmbeddedJdbcOnlineOrderRepository implements OnlineOrderRepository {
    private final EventLog eventLog;
    private final Topic topic;
    private final JdbcConnectionPool pool;

    EmbeddedJdbcOnlineOrderRepository(EventLog eventLog, Topic topic, JdbcConnectionPool pool) {
        this.eventLog = eventLog;
        this.topic = topic;
        this.pool = pool;

        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("CREATE TABLE ONLINE_ORDERS (REF VARCHAR(255), TYPE INT, STATE INT, PAYMENT_REF VARCHAR(255))");
            statement.execute();

            statement = connection.prepareStatement("CREATE TABLE ONLINE_ORDER_PIZZAS (REF VARCHAR(255), INDEX INT, SIZE INT)");
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initialize ONLINE_ORDERS table: ", e);
        }

        eventLog.subscribe(topic, e -> {
            if (e instanceof PizzaAddedEvent) {
                PizzaAddedEvent pae = (PizzaAddedEvent) e;

                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("SELECT MAX(INDEX) FROM ONLINE_ORDER_PIZZAS WHERE REF = ? FOR UPDATE");
                    statement.setString(1, pae.getRef().getReference());
                    ResultSet resultSet = statement.executeQuery();
                    int index = 0;
                    if (resultSet.first()) {
                        index = resultSet.getInt(1) + 1;
                    }

                    statement = connection.prepareStatement("INSERT INTO ONLINE_ORDER_PIZZAS (REF, INDEX, SIZE) VALUES (?, ?, ?)");
                    statement.setString(1, pae.getRef().getReference());
                    statement.setInt(2, index);
                    statement.setInt(3, pae.getPizza().getSize().ordinal());
                    statement.execute();

                    connection.commit();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to insert Pizza into ONLINE_ORDER_PIZZAS table: ", ex);
                }
            } else if (e instanceof PaymentRefAssignedEvent) {
                PaymentRefAssignedEvent prae = (PaymentRefAssignedEvent) e;

                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE ONLINE_ORDERS SET PAYMENT_REF = ? WHERE REF = ?");
                    statement.setString(1, prae.getPaymentRef().getReference());
                    statement.setString(2, prae.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update PAYMENT_REF in ONLINE_ORDERS table: ", ex);
                }
            } else if (e instanceof OnlineOrderPaidEvent) {
                OnlineOrderPaidEvent oope = (OnlineOrderPaidEvent) e;

                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE ONLINE_ORDERS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, OnlineOrder.State.PAID.ordinal());
                    statement.setString(2, oope.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in ONLINE_ORDERS table: ", ex);
                }
            } else if (e instanceof OnlineOrderSubmittedEvent) {
                OnlineOrderSubmittedEvent oose = (OnlineOrderSubmittedEvent) e;

                try (Connection connection = pool.getConnection()) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE ONLINE_ORDERS SET STATE = ? WHERE REF = ?");
                    statement.setInt(1, OnlineOrder.State.SUBMITTED.ordinal());
                    statement.setString(2, oose.getRef().getReference());
                    statement.execute();
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to update STATE in ONLINE_ORDERS table: ", ex);
                }
            }

        });
    }

    @Override
    public void add(OnlineOrder onlineOrder) {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO ONLINE_ORDERS (REF, TYPE, STATE) VALUES (?, ?, ?)");
            statement.setString(1, onlineOrder.getRef().getReference());
            statement.setInt(2, onlineOrder.getType().ordinal());
            statement.setInt(3, onlineOrder.getState().ordinal());
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert OnlineOrder into ONLINE_ORDERS table: ", e);
        }

        this.eventLog.publish(topic, new OnlineOrderAddedEvent(onlineOrder.getRef(), onlineOrder.state()));
    }

    @Override
    public OnlineOrderRef nextIdentity() {
        return new OnlineOrderRef();
    }

    @Override
    public OnlineOrder findByRef(OnlineOrderRef ref) {
        OnlineOrder onlineOrder;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, TYPE, STATE, PAYMENT_REF FROM ONLINE_ORDERS WHERE REF = ?");
            onlineOrder = rehydrateOnlineOrder(ref, connection, statement);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve OnlineOrder from ONLINE_ORDERS table: ", e);
        }
        return onlineOrder;
    }

    @Override
    public OnlineOrder findByPaymentRef(PaymentRef paymentRef) {
        OnlineOrder onlineOrder;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, TYPE, STATE, PAYMENT_REF FROM ONLINE_ORDERS WHERE PAYMENT_REF = ?");
            onlineOrder = rehydrateOnlineOrder(paymentRef, connection, statement);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve OnlineOrder from ONLINE_ORDERS table: ", e);
        }
        return onlineOrder;
    }

    private OnlineOrder rehydrateOnlineOrder(Ref ref, Connection connection, PreparedStatement statement) throws SQLException {
        OnlineOrder onlineOrder = null;
        statement.setString(1, ref.getReference());
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.first()) {
            OnlineOrderRef onlineOrderRef = new OnlineOrderRef(resultSet.getString(1));
            int type = resultSet.getInt(2);
            int state = resultSet.getInt(3);
            onlineOrder = OnlineOrder.builder()
                    .ref(onlineOrderRef)
                    .type(OnlineOrder.Type.values()[type])
                    .eventLog(eventLog)
                    .build();
            Optional<String> optPaymentRef = Optional.ofNullable(resultSet.getString(4));
            if (optPaymentRef.isPresent()) {
                onlineOrder.setPaymentRef(new PaymentRef(optPaymentRef.get()));
            }
            onlineOrder.setState(OnlineOrder.State.values()[state]);

            statement = connection.prepareStatement("SELECT REF, INDEX, SIZE FROM ONLINE_ORDER_PIZZAS WHERE REF = ? ORDER BY INDEX");
            statement.setString(1, onlineOrderRef.getReference());
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                onlineOrder.getPizzas().add(Pizza.builder().size(Pizza.Size.values()[resultSet.getInt(3)]).build());
            }
        }
        return onlineOrder;
    }
}
