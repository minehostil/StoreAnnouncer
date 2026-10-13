package org.minehostil.storeannouncer.purchase;

public record Purchase(String playerName, String uuid, String producto, int cantidad, double precio) {
}