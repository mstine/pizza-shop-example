package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.services.RefStringGenerator;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderRef;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedJdbcKitchenOrderRepositoryIntegrationTests {

    private KitchenOrderRepository repository;
    private InProcessEventLog eventLog;
    private KitchenOrderRef ref;
    private KitchenOrder kitchenOrder;
    private JdbcConnectionPool pool;

    @Before
    public void setUp() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MVCC=FALSE", "", "");

        eventLog = InProcessEventLog.instance();
        repository = new EmbeddedJdbcKitchenOrderRepository(eventLog,
                new Topic("kitchen_orders"),
                pool);
        ref = repository.nextIdentity();
        kitchenOrder = KitchenOrder.builder()
                .ref(ref)
                .onlineOrderRef(new OnlineOrderRef(RefStringGenerator.generateRefString()))
                .pizza(KitchenOrder.Pizza.builder().size(KitchenOrder.Pizza.Size.MEDIUM).build())
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
    public void find_by_ref_hydrates_prepping_order() {
        repository.add(kitchenOrder);
        kitchenOrder.startPrep();
        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }

    @Test
    public void find_by_ref_hydrates_baking_order() {
        repository.add(kitchenOrder);
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }

    @Test
    public void find_by_ref_hydrates_assembling_order() {
        repository.add(kitchenOrder);
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        kitchenOrder.startAssembly();
        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }

    @Test
    public void find_by_ref_hydrates_assembled_order() {
        repository.add(kitchenOrder);
        kitchenOrder.startPrep();
        kitchenOrder.startBake();
        kitchenOrder.startAssembly();
        kitchenOrder.finishAssembly();
        assertThat(repository.findByRef(ref)).isEqualTo(kitchenOrder);
    }

    /*
    @Test
    public void find_by_paymentRef_hydrates_order() {
        repository.add(onlineOrder);
        onlineOrder.addPizza(pizza);
        onlineOrder.submit();

        PaymentRef paymentRef = new PaymentRef(RefStringGenerator.generateRefString());
        onlineOrder.assignPaymentRef(paymentRef);

        assertThat(repository.findByPaymentRef(paymentRef)).isEqualTo(onlineOrder);
    }
     */

    @Test
    public void find_by_onlineOrderRef_hydrates_order() {
        repository.add(kitchenOrder);
        assertThat(repository.findByOnlineOrderRef(kitchenOrder.getOnlineOrderRef())).isEqualTo(kitchenOrder);
    }
}


