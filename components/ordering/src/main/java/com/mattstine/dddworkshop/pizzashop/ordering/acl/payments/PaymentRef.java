package com.mattstine.dddworkshop.pizzashop.ordering.acl.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * @author Matt Stine
 */
@Value
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRef implements Ref {
    @NonFinal
    String reference;
}
