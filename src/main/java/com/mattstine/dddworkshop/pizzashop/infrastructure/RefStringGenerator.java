package com.mattstine.dddworkshop.pizzashop.infrastructure;

import java.util.UUID;

/**
 * @author Matt Stine
 */
public class RefStringGenerator {
	public static String generateRefString() {
		return UUID.randomUUID().toString().toUpperCase();
	}
}