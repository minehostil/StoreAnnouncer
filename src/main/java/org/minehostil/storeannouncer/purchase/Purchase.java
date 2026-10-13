package org.minehostil.storeannouncer.purchase;

public record Purchase(String playerName, String uuid, String product, int quantity, double price) {
}