package com.mattstine.dddworkshop.pizzashop.ordering;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Matt Stine
 */
public class PizzaTests {

    @Test
    public void calculates_price() {
        Pizza pizza = Pizza.builder().size(Pizza.Size.MEDIUM).build();
        assertThat(pizza.calculatePrice()).isEqualTo(Pizza.Size.MEDIUM.getPrice());
    }

}
