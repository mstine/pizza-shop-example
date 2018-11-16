package com.mattstine.dddworkshop.pizzashop.ordering.acl.payments;

import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * @author Matt Stine
 */
@Value
@NoArgsConstructor
public class PaymentRef {
	@NonFinal
	String reference;
}
