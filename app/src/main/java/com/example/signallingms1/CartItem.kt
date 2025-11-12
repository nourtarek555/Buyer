package com.example.signallingms1

data class CartItem(
    var productId: String = "",
    var sellerId: String = "",
    var productName: String = "",
    var price: Double = 0.0,
    var quantity: Int = 1,
    var imageUrl: String = ""
) {
    fun getTotalPrice(): Double = price * quantity
}

