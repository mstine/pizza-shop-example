package com.mattstine.dddworkshop.pizzashop.ordering.acl.payments;

import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
public class PaymentSuccessfulEvent {
	PaymentRef ref;
}
