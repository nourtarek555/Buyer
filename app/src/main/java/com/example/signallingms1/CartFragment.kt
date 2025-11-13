package com.example.signallingms1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class CartFragment : Fragment() {

    private lateinit var rvCart: RecyclerView
    private lateinit var tvTotalPrice: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnClearCart: Button
    private lateinit var btnPlaceOrder: Button
    private lateinit var cartAdapter: CartAdapter
    private val cartItems = mutableListOf<CartItem>()
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            if (!isAdded || context == null) return

            rvCart = view.findViewById(R.id.rvCart)
            tvTotalPrice = view.findViewById(R.id.tvTotalPrice)
            tvEmpty = view.findViewById(R.id.tvEmpty)
            btnClearCart = view.findViewById(R.id.btnClearCart)
            btnPlaceOrder = view.findViewById(R.id.btnPlaceOrder)

            cartAdapter = CartAdapter(cartItems,
                onQuantityChanged = { productId, sellerId, quantity ->
                    if (isAdded && context != null) {
                        // Fetch current stock from Firebase before updating
                        fetchAndUpdateQuantity(productId, sellerId, quantity)
                    }
                },
                onItemRemoved = { productId ->
                    if (isAdded && context != null) {
                        CartManager.removeFromCart(requireContext(), productId)
                        refreshCart()
                    }
                }
            )

            rvCart.layoutManager = LinearLayoutManager(requireContext())
            rvCart.adapter = cartAdapter

            btnClearCart.setOnClickListener {
                if (isAdded && context != null) {
                    CartManager.clearCart(requireContext())
                    refreshCart()
                    Toast.makeText(context, "Cart cleared", Toast.LENGTH_SHORT).show()
                }
            }

            btnPlaceOrder.setOnClickListener {
                placeOrder()
            }

            // Delay refresh to ensure views are fully initialized
            view.post {
                refreshCart()
            }
        } catch (e: Exception) {
            android.util.Log.e("CartFragment", "Error in onViewCreated: ${e.message}", e)
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && view != null) {
            view?.post {
                refreshCart()
            }
        }
    }

    private fun refreshCart() {
        if (!isAdded || context == null || view == null) {
            android.util.Log.w("CartFragment", "Cannot refresh cart - fragment not ready")
            return
        }
        
        try {
            // Check if adapter is initialized
            if (!::cartAdapter.isInitialized) {
                android.util.Log.w("CartFragment", "Adapter not initialized yet")
                return
            }
            
            val items = CartManager.getCartItemsList(requireContext())
            android.util.Log.d("CartFragment", "Cart items count: ${items.size}")
            
            if (items.isNotEmpty()) {
                android.util.Log.d("CartFragment", "Cart items: ${items.map { "${it.productName} x${it.quantity}" }}")
            }
            
            cartItems.clear()
            cartItems.addAll(items)

            if (cartItems.isEmpty()) {
                android.util.Log.d("CartFragment", "Cart is empty, showing empty message")
                tvEmpty.visibility = View.VISIBLE
                rvCart.visibility = View.GONE
                btnPlaceOrder.isEnabled = false
            } else {
                android.util.Log.d("CartFragment", "Cart has ${cartItems.size} items, showing cart")
                tvEmpty.visibility = View.GONE
                rvCart.visibility = View.VISIBLE
                btnPlaceOrder.isEnabled = true
            }

            val total = CartManager.getTotalPrice(requireContext())
            android.util.Log.d("CartFragment", "Total price: $total")
            tvTotalPrice.text = "$${String.format("%.2f", total)}"
            
            // Update adapter
            cartAdapter.updateCartItems(ArrayList(cartItems))
            android.util.Log.d("CartFragment", "Adapter updated with ${cartAdapter.itemCount} items")
        } catch (e: Exception) {
            android.util.Log.e("CartFragment", "Error refreshing cart: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun fetchAndUpdateQuantity(productId: String, sellerId: String, quantity: Int) {
        if (!isAdded || context == null) return
        
        // Fetch current stock from Firebase - try "Products" first, then "products"
        val productsRef = database.getReference("Seller").child(sellerId).child("Products").child(productId)
        productsRef.get().addOnSuccessListener { snapshot ->
            if (!isAdded || context == null) return@addOnSuccessListener
            
            if (snapshot.exists()) {
                try {
                    // Get stock value (can be String or Int)
                    val stockValue = snapshot.child("stock").getValue(Any::class.java)
                    val currentStock = when (stockValue) {
                        is Int -> stockValue
                        is Long -> stockValue.toInt()
                        is String -> stockValue.toIntOrNull() ?: 0
                        is Number -> stockValue.toInt()
                        else -> 0
                    }
                    
                    // Update quantity with current stock validation
                    val (success, message) = CartManager.updateQuantity(requireContext(), productId, quantity, currentStock)
                    if (!success) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    refreshCart()
                } catch (e: Exception) {
                    android.util.Log.e("CartFragment", "Error fetching stock: ${e.message}", e)
                    handleStockFetchFallback(productId, quantity)
                }
            } else {
                // Try lowercase "products"
                val productsRefLower = database.getReference("Seller").child(sellerId).child("products").child(productId)
                productsRefLower.get().addOnSuccessListener { snapshot2 ->
                    if (!isAdded || context == null) return@addOnSuccessListener
                    try {
                        val stockValue = snapshot2.child("stock").getValue(Any::class.java)
                        val currentStock = when (stockValue) {
                            is Int -> stockValue
                            is Long -> stockValue.toInt()
                            is String -> stockValue.toIntOrNull() ?: 0
                            is Number -> stockValue.toInt()
                            else -> 0
                        }
                        val (success, message) = CartManager.updateQuantity(requireContext(), productId, quantity, currentStock)
                        if (!success) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                        refreshCart()
                    } catch (e: Exception) {
                        android.util.Log.e("CartFragment", "Error fetching stock: ${e.message}", e)
                        handleStockFetchFallback(productId, quantity)
                    }
                }.addOnFailureListener {
                    handleStockFetchFallback(productId, quantity)
                }
            }
        }.addOnFailureListener { error ->
            if (isAdded && context != null) {
                android.util.Log.e("CartFragment", "Failed to fetch stock: ${error.message}", error)
                handleStockFetchFallback(productId, quantity)
            }
        }
    }
    
    private fun handleStockFetchFallback(productId: String, quantity: Int) {
        if (!isAdded || context == null) return
        // Fallback to stored maxStock if fetch fails
        val (success, message) = CartManager.updateQuantity(requireContext(), productId, quantity)
        if (!success) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        refreshCart()
    }

    private fun placeOrder() {
        if (!isAdded || context == null) return
        
        val buyerId = auth.currentUser?.uid
        if (buyerId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (cartItems.isEmpty()) {
            Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Group items by seller
        val ordersBySeller = cartItems.groupBy { it.sellerId }

        // Load buyer info
        database.getReference("Buyers").child(buyerId).get().addOnSuccessListener { buyerSnapshot ->
            val buyer = buyerSnapshot.getValue(UserProfile::class.java)
            val buyerName = buyer?.name ?: "Unknown"
            val buyerAddress = buyer?.address ?: ""

            // Create orders for each seller
            ordersBySeller.forEach { (sellerId, items) ->
                // Load seller info
                database.getReference("Seller").child(sellerId).get().addOnSuccessListener { sellerSnapshot ->
                    val seller = sellerSnapshot.getValue(Seller::class.java)
                    val sellerName = seller?.shopName?.ifEmpty { seller?.name } ?: "Unknown"

                    // Create order
                    val orderId = database.getReference("Orders").push().key ?: return@addOnSuccessListener
                    val itemsMap = items.associateBy { it.productId }
                    val totalPrice = items.sumOf { it.getTotalPrice() }

                    val order = Order(
                        orderId = orderId,
                        buyerId = buyerId,
                        sellerId = sellerId,
                        items = itemsMap,
                        totalPrice = totalPrice,
                        status = "pending",
                        timestamp = System.currentTimeMillis(),
                        buyerName = buyerName,
                        buyerAddress = buyerAddress,
                        sellerName = sellerName
                    )

                    // Save order
                    database.getReference("Orders").child(orderId).setValue(order)
                        .addOnSuccessListener {
                            if (isAdded && context != null) {
                                Toast.makeText(context, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                                CartManager.clearCart(requireContext())
                                refreshCart()
                            }
                        }
                        .addOnFailureListener {
                            if (isAdded && context != null) {
                                Toast.makeText(context, "Failed to place order: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }
}

