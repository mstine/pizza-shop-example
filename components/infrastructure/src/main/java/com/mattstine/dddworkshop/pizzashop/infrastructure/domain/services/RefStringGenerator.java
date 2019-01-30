package com.mattstine.dddworkshop.pizzashop.infrastructure.domain.services;

import java.util.UUID;

/**
 * @author Matt Stine
 */
public final class RefStringGenerator {
    /**
     * Generate a random upper cased UUID for use as <code>Ref</code>'s <code>reference</code> property.
     *
     * @return random upper cased UUID String
     */
    public static String generateRefString() {
        return UUID.randomUUID().toString().toUpperCase();
    }
}
