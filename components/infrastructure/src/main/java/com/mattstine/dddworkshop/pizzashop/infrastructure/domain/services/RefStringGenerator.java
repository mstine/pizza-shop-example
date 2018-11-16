package com.mattstine.dddworkshop.pizzashop.infrastructure.domain.services;

import java.util.UUID;

/**
 * @author Matt Stine
 */
public final class RefStringGenerator {
    public static String generateRefString() {
        return UUID.randomUUID().toString().toUpperCase();
    }
}
