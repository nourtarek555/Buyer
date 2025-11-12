package com.example.signallingms1

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(
    private val products: MutableList<Product>,
    private val onAddToCart: (Product, Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        Log.d("ProductAdapter", "Binding view at position $position, total items: ${products.size}")
        if (position < products.size) {
            holder.bind(products[position])
        }
    }

    override fun getItemCount(): Int {
        val count = products.size
        Log.d("ProductAdapter", "getItemCount called: $count")
        return count
    }

    fun updateProducts(newProducts: List<Product>) {
        Log.d("ProductAdapter", "updateProducts called with ${newProducts.size} products")
        val oldSize = products.size
        products.clear()
        products.addAll(newProducts)
        Log.d("ProductAdapter", "Products list updated. Old size: $oldSize, New size: ${products.size}")
        notifyDataSetChanged()
        Log.d("ProductAdapter", "notifyDataSetChanged() called")
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val productName: TextView = itemView.findViewById(R.id.tvProductName)
        private val productPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val productQuantity: TextView = itemView.findViewById(R.id.tvProductQuantity)
        private val quantityText: TextView = itemView.findViewById(R.id.tvQuantity)
        private val btnDecrease: Button = itemView.findViewById(R.id.btnDecrease)
        private val btnIncrease: Button = itemView.findViewById(R.id.btnIncrease)
        private val btnAddToCart: Button = itemView.findViewById(R.id.btnAddToCart)

        private var currentQuantity = 0

        fun bind(product: Product) {
            val productNameValue = product.getDisplayName()
            val productPriceValue = product.getDisplayPrice()
            val productStockValue = product.getDisplayStock()
            val productImageUrl = product.getDisplayImageUrl()
            
            productName.text = productNameValue
            productPrice.text = "$${String.format("%.2f", productPriceValue)}"
            productQuantity.text = "Stock: $productStockValue"
            currentQuantity = 0
            quantityText.text = "0"

            // Load image with placeholder - handles empty URLs gracefully
            if (productImageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(productImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(productImage)
            } else {
                // Set placeholder directly if no image URL
                productImage.setImageResource(R.drawable.ic_person)
            }

            btnDecrease.setOnClickListener {
                if (currentQuantity > 0) {
                    currentQuantity--
                    quantityText.text = currentQuantity.toString()
                }
            }

            btnIncrease.setOnClickListener {
                if (currentQuantity < productStockValue) {
                    currentQuantity++
                    quantityText.text = currentQuantity.toString()
                } else {
                    Toast.makeText(itemView.context, "Not enough stock", Toast.LENGTH_SHORT).show()
                }
            }

            btnAddToCart.setOnClickListener {
                if (currentQuantity > 0) {
                    if (currentQuantity <= productStockValue) {
                        // Update product with correct values before adding to cart
                        product.name = productNameValue
                        product.price = productPriceValue
                        product.quantity = productStockValue
                        product.imageUrl = productImageUrl
                        
                        onAddToCart(product, currentQuantity)
                        Toast.makeText(itemView.context, "Added to cart!", Toast.LENGTH_SHORT).show()
                        currentQuantity = 0
                        quantityText.text = "0"
                    } else {
                        Toast.makeText(itemView.context, "Not enough stock", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(itemView.context, "Please select quantity", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

