package com.example.signallingms1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProductsFragment : Fragment() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvShopName: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var productAdapter: ProductAdapter
    private val products = mutableListOf<Product>()
    private val database = FirebaseDatabase.getInstance().getReference("Seller")
    private var currentSellerId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            rvProducts = view.findViewById(R.id.rvProducts)
            progressBar = view.findViewById(R.id.progressBar)
            tvEmpty = view.findViewById(R.id.tvEmpty)
            tvShopName = view.findViewById(R.id.tvShopName)
            btnBack = view.findViewById(R.id.btnBack)

            productAdapter = ProductAdapter(products) { product, quantity ->
                if (isAdded && context != null) {
                    CartManager.addToCart(requireContext(), product, quantity)
                }
            }

            rvProducts.layoutManager = GridLayoutManager(context, 2)
            rvProducts.adapter = productAdapter
            rvProducts.visibility = View.VISIBLE
            rvProducts.setHasFixedSize(false)
            
            android.util.Log.d("ProductsFragment", "RecyclerView initialized, adapter set")

            // Back button functionality
            btnBack.setOnClickListener {
                if (isAdded) {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }

            // Load products if seller ID was passed via arguments
            arguments?.getString("sellerId")?.let { sellerId ->
                arguments?.getString("sellerName")?.let { sellerName ->
                    tvShopName.text = sellerName
                    loadProducts(sellerId)
                }
            } ?: run {
                // If no arguments, show empty state
                tvShopName.text = "Select a shop to view products"
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "No shop selected"
                rvProducts.visibility = View.GONE
                progressBar.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (isAdded && context != null) {
                Toast.makeText(context, "Error initializing products view: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadProductsForSeller(seller: Seller) {
        if (!isAdded || view == null) {
            return
        }
        
        try {
            currentSellerId = seller.uid
            tvShopName.text = seller.shopName.ifEmpty { seller.name }
            rvProducts.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            loadProducts(seller.uid)
        } catch (e: Exception) {
            e.printStackTrace()
            if (isAdded && context != null) {
                Toast.makeText(context, "Error loading products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProducts(sellerId: String) {
        if (!isAdded || context == null || view == null) {
            return
        }
        
        try {
            progressBar.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            rvProducts.visibility = View.VISIBLE

            // Try "Products" (capital P) first, then fallback to "products"
            val productsRef = database.child(sellerId).child("Products")
            productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || context == null || view == null) {
                        return
                    }
                    
                    try {
                        progressBar.visibility = View.GONE
                        products.clear()

                        // Check if the products node exists
                        if (!snapshot.exists()) {
                            // Try lowercase "products" as fallback
                            val productsRefLower = database.child(sellerId).child("products")
                            productsRefLower.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot2: DataSnapshot) {
                                    if (!isAdded || context == null || view == null) return
                                    processProducts(snapshot2, sellerId)
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    if (!isAdded || context == null || view == null) return
                                    showError("No products available for this shop")
                                }
                            })
                            return
                        }

                        processProducts(snapshot, sellerId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (isAdded && context != null) {
                            showError("Error processing products: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (!isAdded || context == null || view == null) return
                    try {
                        progressBar.visibility = View.GONE
                        Toast.makeText(context, "Failed to load products: ${error.message}", Toast.LENGTH_SHORT).show()
                        showError("Failed to load products")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            if (isAdded && context != null) {
                progressBar.visibility = View.GONE
                showError("Error loading products: ${e.message}")
            }
        }
    }
    
    private fun processProducts(snapshot: DataSnapshot, sellerId: String) {
        if (!isAdded || context == null || view == null) return
        
        try {
            products.clear()

            if (!snapshot.exists()) {
                showError("No products available for this shop")
                return
            }

            android.util.Log.d("ProductsFragment", "Processing ${snapshot.childrenCount} products")
            
            for (productSnapshot in snapshot.children) {
                try {
                    val productKey = productSnapshot.key ?: "unknown"
                    android.util.Log.d("ProductsFragment", "Processing product key: $productKey")
                    android.util.Log.d("ProductsFragment", "Product data: ${productSnapshot.value}")
                    
                    // Try to get all fields first to see what's available
                    val nameField = productSnapshot.child("Name")
                    val priceField = productSnapshot.child("Price")
                    val stockField = productSnapshot.child("Stock")
                    val photoUrlField = productSnapshot.child("PhotoUrl")
                    
                    android.util.Log.d("ProductsFragment", "Name exists: ${nameField.exists()}, value: ${nameField.getValue(String::class.java)}")
                    android.util.Log.d("ProductsFragment", "Price exists: ${priceField.exists()}, value: ${priceField.getValue(Double::class.java)}")
                    android.util.Log.d("ProductsFragment", "Stock exists: ${stockField.exists()}, value: ${stockField.getValue(Int::class.java)}")
                    android.util.Log.d("ProductsFragment", "PhotoUrl exists: ${photoUrlField.exists()}, value: ${photoUrlField.getValue(String::class.java)}")
                    
                    val product = productSnapshot.getValue(Product::class.java)
                    if (product != null) {
                        product.productId = productKey
                        product.sellerId = sellerId
                        android.util.Log.d("ProductsFragment", "Successfully deserialized product: ${product.getDisplayName()}, price: ${product.getDisplayPrice()}, stock: ${product.getDisplayStock()}")
                        products.add(product)
                    } else {
                        android.util.Log.w("ProductsFragment", "Failed to deserialize product, trying manual mapping")
                        // Manual mapping if deserialization fails
                        val name = productSnapshot.child("Name").getValue(String::class.java) 
                            ?: productSnapshot.child("name").getValue(String::class.java) ?: ""
                        val price = productSnapshot.child("Price").getValue(Double::class.java)
                            ?: productSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                        val stock = productSnapshot.child("Stock").getValue(Int::class.java)
                            ?: productSnapshot.child("stock").getValue(Int::class.java)
                            ?: productSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                        val photoUrl = productSnapshot.child("PhotoUrl").getValue(String::class.java)
                            ?: productSnapshot.child("photoUrl").getValue(String::class.java)
                            ?: productSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
                        
                        android.util.Log.d("ProductsFragment", "Manual mapping - name: $name, price: $price, stock: $stock, photoUrl: $photoUrl")
                        
                        val manualProduct = Product(
                            productId = productKey,
                            sellerId = sellerId,
                            name = name,
                            price = price,
                            stock = stock,
                            imageUrl = photoUrl
                        )
                        android.util.Log.d("ProductsFragment", "Manually created product: ${manualProduct.getDisplayName()}, price: ${manualProduct.getDisplayPrice()}, stock: ${manualProduct.getDisplayStock()}")
                        products.add(manualProduct)
                    }
                } catch (e: Exception) {
                    // Skip this product if mapping fails
                    android.util.Log.e("ProductsFragment", "Error processing product: ${e.message}", e)
                    e.printStackTrace()
                }
            }

            android.util.Log.d("ProductsFragment", "Total products processed: ${products.size}")
            
            if (products.isEmpty()) {
                android.util.Log.w("ProductsFragment", "No products found after processing")
                showError("No products available for this shop")
            } else {
                android.util.Log.d("ProductsFragment", "Updating adapter with ${products.size} products")
                // Ensure UI updates happen on main thread
                if (isAdded && view != null) {
                    view?.post {
                        if (isAdded && view != null) {
                            tvEmpty.visibility = View.GONE
                            rvProducts.visibility = View.VISIBLE
                            val productsCopy = ArrayList(products)
                            productAdapter.updateProducts(productsCopy)
                            android.util.Log.d("ProductsFragment", "Adapter updated. Item count: ${productAdapter.itemCount}")
                            android.util.Log.d("ProductsFragment", "RecyclerView visibility: ${rvProducts.visibility}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (isAdded && context != null) {
                showError("Error processing products: ${e.message}")
            }
        }
    }
    
    private fun showError(message: String) {
        if (!isAdded || context == null || view == null) return
        try {
            android.util.Log.d("ProductsFragment", "Showing error: $message")
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = message
            rvProducts.visibility = View.GONE
            progressBar.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

