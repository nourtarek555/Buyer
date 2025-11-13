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
    
    /**
     * Adds a product to cart with stock validation
     * @return Pair<Boolean, String> - (success, message)
     */
    fun addToCart(context: Context, product: Product, quantity: Int = 1): Pair<Boolean, String> {
        val cartItems = getCartItems(context).toMutableMap()
        val cartItem = cartItems[product.productId]
        val currentStock = product.getDisplayStock()
        
        if (cartItem != null) {
            // Item already in cart - check if adding more would exceed stock
            val newQuantity = cartItem.quantity + quantity
            
            // Use current stock from database (product.getDisplayStock()) as the limit
            if (newQuantity > currentStock) {
                val available = currentStock - cartItem.quantity
                return if (available > 0) {
                    // Can add some, but not the full requested amount
                    val finalQuantity = cartItem.quantity + available
                    cartItems[product.productId] = CartItem(
                        productId = cartItem.productId,
                        sellerId = cartItem.sellerId,
                        productName = cartItem.productName,
                        price = cartItem.price,
                        quantity = finalQuantity,
                        imageUrl = cartItem.imageUrl,
                        maxStock = currentStock
                    )
                    saveCartItems(context, cartItems)
                    Pair(true, "Only $available more available. Added $available to cart.")
                } else {
                    // Already at max stock
                    Pair(false, "Not enough stock. Maximum available: $currentStock (already have ${cartItem.quantity} in cart)")
                }
            } else {
                // Can add the full quantity
                cartItems[product.productId] = CartItem(
                    productId = cartItem.productId,
                    sellerId = cartItem.sellerId,
                    productName = cartItem.productName,
                    price = cartItem.price,
                    quantity = newQuantity,
                    imageUrl = cartItem.imageUrl,
                    maxStock = currentStock
                )
                saveCartItems(context, cartItems)
                return Pair(true, "Added $quantity to cart")
            }
        } else {
            // New item - check if quantity exceeds stock
            if (quantity > currentStock) {
                return Pair(false, "Not enough stock. Maximum available: $currentStock")
            }
            
            // Add new item
            cartItems[product.productId] = CartItem(
                productId = product.productId,
                sellerId = product.sellerId,
                productName = product.getDisplayName(),
                price = product.getDisplayPrice(),
                quantity = quantity,
                imageUrl = product.getDisplayImageUrl(),
                maxStock = currentStock
            )
            saveCartItems(context, cartItems)
            return Pair(true, "Added $quantity to cart")
        }
    }
    
    fun removeFromCart(context: Context, productId: String) {
        val cartItems = getCartItems(context).toMutableMap()
        cartItems.remove(productId)
        saveCartItems(context, cartItems)
    }
    
    /**
     * Updates quantity of an item in cart with stock validation
     * @return Pair<Boolean, String> - (success, message)
     */
    fun updateQuantity(context: Context, productId: String, quantity: Int, currentStock: Int? = null): Pair<Boolean, String> {
        if (quantity <= 0) {
            removeFromCart(context, productId)
            return Pair(true, "Item removed from cart")
        }
        
        val cartItems = getCartItems(context).toMutableMap()
        val existingItem = cartItems[productId]
        if (existingItem != null) {
            // Use provided currentStock if available, otherwise use stored maxStock
            val maxStock = currentStock ?: (if (existingItem.maxStock > 0) existingItem.maxStock else existingItem.quantity)
            
            if (quantity > maxStock) {
                return Pair(false, "Not enough stock. Maximum available: $maxStock")
            }
            
            // Create a new CartItem with updated quantity to ensure proper serialization
            cartItems[productId] = CartItem(
                productId = existingItem.productId,
                sellerId = existingItem.sellerId,
                productName = existingItem.productName,
                price = existingItem.price,
                quantity = quantity,
                imageUrl = existingItem.imageUrl,
                maxStock = maxStock  // Update maxStock if currentStock was provided
            )
            saveCartItems(context, cartItems)
            return Pair(true, "Quantity updated")
        }
        return Pair(false, "Item not found in cart")
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

