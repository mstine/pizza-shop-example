package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.services.RefStringGenerator;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;
import lombok.Value;

@Value
public final class PizzaRef implements Ref {
    public static final PizzaRef IDENTITY = new PizzaRef("");
    String reference;

    public PizzaRef() {
        this.reference = RefStringGenerator.generateRefString();
    }

    @SuppressWarnings("SameParameterValue")
    PizzaRef(String reference) {
        this.reference = reference;
    }

    @Override
    public String getReference() {
        return reference;
    }
}
