package com.mattstine.dddworkshop.pizzashop.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes.Amount;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Matt Stine
 */
final class EmbeddedJdbcPaymentRepository implements PaymentRepository {

    private final EventLog eventLog;
    private final Topic topic;
    private final JdbcConnectionPool pool;

    EmbeddedJdbcPaymentRepository(EventLog eventLog, Topic topic, JdbcConnectionPool pool) {
        this.eventLog = eventLog;
        this.topic = topic;
        this.pool = pool;

        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("CREATE TABLE PAYMENTS (REF VARCHAR(255) PRIMARY KEY, DOLLARS INT, CENTS INT, STATE INT)");
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initialize PAYMENTS table: ", e);
        }

        eventLog.subscribe(topic, e -> {
            if (e instanceof PaymentRequestedEvent) {
                PaymentRequestedEvent pre = (PaymentRequestedEvent) e;
                updatePaymentState(pre.getRef(), Payment.State.REQUESTED);
            } else if (e instanceof PaymentSuccessfulEvent) {
                PaymentSuccessfulEvent pre = (PaymentSuccessfulEvent) e;
                updatePaymentState(pre.getRef(), Payment.State.SUCCESSFUL);
            } else if (e instanceof PaymentFailedEvent) {
                PaymentFailedEvent pre = (PaymentFailedEvent) e;
                updatePaymentState(pre.getRef(), Payment.State.FAILED);
            }
        });
    }

    private void updatePaymentState(PaymentRef ref, Payment.State state) {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE PAYMENTS SET STATE = ? WHERE REF = ?");
            statement.setInt(1, state.ordinal());
            statement.setString(2, ref.getReference());
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update PAYMENTS.STATE: ", e);
        }
    }

    @Override
    public PaymentRef nextIdentity() {
        return new PaymentRef();
    }

    @Override
    public void add(Payment payment) {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO PAYMENTS (REF, DOLLARS, CENTS, STATE) VALUES (?, ?, ?, ?)");
            statement.setString(1, payment.getRef().getReference());
            statement.setInt(2, payment.getAmount().getDollars());
            statement.setInt(3, payment.getAmount().getCents());
            statement.setInt(4, payment.getState().ordinal());
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert Payment into PAYMENTS table: ", e);
        }

        this.eventLog.publish(topic, new PaymentAddedEvent(payment.getRef(), payment.state()));
    }

    @Override
    public Payment findByRef(PaymentRef ref) {
        Payment payment = null;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT REF, DOLLARS, CENTS, STATE FROM PAYMENTS WHERE REF = ?");
            statement.setString(1, ref.getReference());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.first()) {
                PaymentRef paymentRef = new PaymentRef(resultSet.getString(1));
                int dollars = resultSet.getInt(2);
                int cents = resultSet.getInt(3);
                int state = resultSet.getInt(4);
                payment = Payment.builder()
                        .ref(paymentRef)
                        .amount(Amount.of(dollars, cents))
                        .paymentProcessor(DummyPaymentProcessor.instance())
                        .eventLog(eventLog)
                        .build();
                payment.setState(Payment.State.values()[state]);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve Payment from PAYMENTS table: ", e);
        }
        return payment;
    }
}
