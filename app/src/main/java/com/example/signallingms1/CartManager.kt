package com.example.signallingms1

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CartManager {
    private const val PREFS_NAME = "cart_prefs"
    private const val KEY_CART_ITEMS = "cart_items"
    private val gson = Gson()
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun addToCart(context: Context, product: Product, quantity: Int = 1) {
        val cartItems = getCartItems(context).toMutableMap()
        val cartItem = cartItems[product.productId]
        
        if (cartItem != null) {
            // Update quantity if item already exists
            cartItem.quantity += quantity
        } else {
            // Add new item
            cartItems[product.productId] = CartItem(
                productId = product.productId,
                sellerId = product.sellerId,
                productName = product.name,
                price = product.price,
                quantity = quantity,
                imageUrl = product.imageUrl
            )
        }
        
        saveCartItems(context, cartItems)
    }
    
    fun removeFromCart(context: Context, productId: String) {
        val cartItems = getCartItems(context).toMutableMap()
        cartItems.remove(productId)
        saveCartItems(context, cartItems)
    }
    
    fun updateQuantity(context: Context, productId: String, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(context, productId)
            return
        }
        
        val cartItems = getCartItems(context).toMutableMap()
        cartItems[productId]?.quantity = quantity
        saveCartItems(context, cartItems)
    }
    
    fun getCartItems(context: Context): Map<String, CartItem> {
        val json = getSharedPreferences(context).getString(KEY_CART_ITEMS, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, CartItem>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } else {
            emptyMap()
        }
    }
    
    fun getCartItemsList(context: Context): List<CartItem> {
        return getCartItems(context).values.toList()
    }
    
    fun getTotalPrice(context: Context): Double {
        return getCartItemsList(context).sumOf { it.getTotalPrice() }
    }
    
    fun clearCart(context: Context) {
        getSharedPreferences(context).edit().remove(KEY_CART_ITEMS).apply()
    }
    
    fun getCartItemCount(context: Context): Int {
        return getCartItemsList(context).sumOf { it.quantity }
    }
    
    private fun saveCartItems(context: Context, items: Map<String, CartItem>) {
        val json = gson.toJson(items)
        getSharedPreferences(context).edit().putString(KEY_CART_ITEMS, json).apply()
    }
}

