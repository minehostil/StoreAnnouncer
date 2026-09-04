package com.tutienda.storeannouncer.purchase;

public record Purchase(String playerName, String uuid, String producto, int cantidad, double precio) {
}
