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

        rvCart = view.findViewById(R.id.rvCart)
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        btnClearCart = view.findViewById(R.id.btnClearCart)
        btnPlaceOrder = view.findViewById(R.id.btnPlaceOrder)

        cartAdapter = CartAdapter(cartItems,
            onQuantityChanged = { productId, quantity ->
                CartManager.updateQuantity(requireContext(), productId, quantity)
                refreshCart()
            },
            onItemRemoved = { productId ->
                CartManager.removeFromCart(requireContext(), productId)
                refreshCart()
            }
        )

        rvCart.layoutManager = LinearLayoutManager(context)
        rvCart.adapter = cartAdapter

        btnClearCart.setOnClickListener {
            CartManager.clearCart(requireContext())
            refreshCart()
            Toast.makeText(context, "Cart cleared", Toast.LENGTH_SHORT).show()
        }

        btnPlaceOrder.setOnClickListener {
            placeOrder()
        }

        refreshCart()
    }

    override fun onResume() {
        super.onResume()
        refreshCart()
    }

    private fun refreshCart() {
        val items = CartManager.getCartItemsList(requireContext())
        cartItems.clear()
        cartItems.addAll(items)

        if (cartItems.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvCart.visibility = View.GONE
            btnPlaceOrder.isEnabled = false
        } else {
            tvEmpty.visibility = View.GONE
            rvCart.visibility = View.VISIBLE
            btnPlaceOrder.isEnabled = true
        }

        val total = CartManager.getTotalPrice(requireContext())
        tvTotalPrice.text = "$${String.format("%.2f", total)}"
        cartAdapter.updateCartItems(cartItems)
    }

    private fun placeOrder() {
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
                            Toast.makeText(context, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                            CartManager.clearCart(requireContext())
                            refreshCart()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to place order: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
}

