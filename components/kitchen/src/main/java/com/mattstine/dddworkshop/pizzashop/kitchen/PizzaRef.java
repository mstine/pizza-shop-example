package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.services.RefStringGenerator;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;
import lombok.Value;

@Value
public final class PizzaRef implements Ref {
    String reference;
    public static final PizzaRef IDENTITY = new PizzaRef("");

    public PizzaRef() {
        this.reference = RefStringGenerator.generateRefString();
    }

    @SuppressWarnings("SameParameterValue")
    private PizzaRef(String reference) {
        this.reference = reference;
    }

    @Override
    public String getReference() {
        return reference;
    }
}
