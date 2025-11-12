package com.example.signallingms1

data class Order(
    var orderId: String = "",
    var buyerId: String = "",
    var sellerId: String = "",
    var items: Map<String, CartItem> = emptyMap(),
    var totalPrice: Double = 0.0,
    var status: String = "pending", // pending, confirmed, delivered, cancelled
    var timestamp: Long = System.currentTimeMillis(),
    var buyerName: String = "",
    var buyerAddress: String = "",
    var sellerName: String = ""
)

