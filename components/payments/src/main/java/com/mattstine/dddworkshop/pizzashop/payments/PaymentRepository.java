package com.mattstine.dddworkshop.pizzashop.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Repository;

/**
 * @author Matt Stine
 */
interface PaymentRepository extends Repository<PaymentRef, Payment, Payment.PaymentState, PaymentEvent, PaymentAddedEvent> {
}
