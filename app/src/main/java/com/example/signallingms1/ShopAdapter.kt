package com.example.signallingms1

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ShopAdapter(
    private val shops: MutableList<Seller>,
    private val onShopClick: (Seller) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        Log.d("ShopAdapter", "Binding view at position $position, total items: ${shops.size}")
        if (position < shops.size) {
            holder.bind(shops[position])
        }
    }

    override fun getItemCount(): Int {
        val count = shops.size
        Log.d("ShopAdapter", "getItemCount called: $count")
        return count
    }

    fun updateShops(newShops: List<Seller>) {
        Log.d("ShopAdapter", "updateShops called with ${newShops.size} shops")
        val oldSize = shops.size
        shops.clear()
        shops.addAll(newShops)
        Log.d("ShopAdapter", "Shops list updated. Old size: $oldSize, New size: ${shops.size}")
        if (oldSize == 0 && newShops.isNotEmpty()) {
            notifyItemRangeInserted(0, newShops.size)
            Log.d("ShopAdapter", "Called notifyItemRangeInserted(0, ${newShops.size})")
        } else {
            notifyDataSetChanged()
            Log.d("ShopAdapter", "Called notifyDataSetChanged()")
        }
    }

    inner class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val shopImage: ImageView = itemView.findViewById(R.id.ivShopImage)
        private val shopName: TextView = itemView.findViewById(R.id.tvShopName)
        private val shopAddress: TextView = itemView.findViewById(R.id.tvShopAddress)

        fun bind(seller: Seller) {
            shopName.text = seller.shopName.ifEmpty { seller.name }
            shopAddress.text = seller.address

            Glide.with(itemView.context)
                .load(seller.photoUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(shopImage)

            itemView.setOnClickListener {
                onShopClick(seller)
            }
        }
    }
}

