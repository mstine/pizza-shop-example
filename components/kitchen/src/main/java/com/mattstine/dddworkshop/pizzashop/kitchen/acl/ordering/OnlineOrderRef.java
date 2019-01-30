package com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering;

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
public class OnlineOrderRef implements Ref {
    public static final OnlineOrderRef IDENTITY = new OnlineOrderRef("");
    @NonFinal
    String reference;
}
