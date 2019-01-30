package com.mattstine.dddworkshop.pizzashop.delivery.acl.kitchen;

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
public class KitchenOrderRef implements Ref {
    public static final KitchenOrderRef IDENTITY = new KitchenOrderRef("");
    @NonFinal
    String reference;
}
