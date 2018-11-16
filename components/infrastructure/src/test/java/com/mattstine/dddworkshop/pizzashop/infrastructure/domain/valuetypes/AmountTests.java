package com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Matt Stine
 */
public class AmountTests {

    @Test
    public void adds_amounts_without_overflow() {
        assertThat(Amount.of(6, 20)
                .plus(Amount.of(6, 20)))
                .isEqualTo(Amount.of(12, 40));
    }

    @Test
    public void adds_amounts_with_perfect_overflow() {
        assertThat(Amount.of(6, 50)
                .plus(Amount.of(6, 50)))
                .isEqualTo(Amount.of(13, 0));
    }

    @Test
    public void adds_amounts_with_imperfect_overflow() {
        assertThat(Amount.of(6, 50)
                .plus(Amount.of(6, 60)))
                .isEqualTo(Amount.of(13, 10));
    }

    @Test
    public void dollars_cannot_be_negative() {
        assertThatIllegalArgumentException().isThrownBy(() -> Amount.of(-1, 0));
    }

    @Test
    public void cents_cannot_be_negative() {
        assertThatIllegalArgumentException().isThrownBy(() -> Amount.of(0, -1));
    }

    @Test
    public void cents_must_be_less_than_100() {
        assertThatIllegalArgumentException().isThrownBy(() -> Amount.of(0, 100));
    }
}
