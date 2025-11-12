package com.example.signallingms1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class ShopsFragment : Fragment() {

    private lateinit var rvShops: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var shopAdapter: ShopAdapter
    private val shops = mutableListOf<Seller>()
    private val database = FirebaseDatabase.getInstance().getReference("Seller")

    var onShopSelected: ((Seller) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shops, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvShops = view.findViewById(R.id.rvShops)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        // Create adapter with empty list initially
        shopAdapter = ShopAdapter(mutableListOf()) { seller ->
            onShopSelected?.invoke(seller)
        }

        val layoutManager = GridLayoutManager(context, 2)
        rvShops.layoutManager = layoutManager
        rvShops.adapter = shopAdapter
        rvShops.visibility = View.VISIBLE
        rvShops.setHasFixedSize(false)
        
        Log.d("ShopsFragment", "RecyclerView initialized, adapter set with empty list")

        loadShops()
    }

    private fun loadShops() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        Log.d("ShopsFragment", "Loading shops from: ${database.path}")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
                shops.clear()

                Log.d("ShopsFragment", "DataSnapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")

                if (!snapshot.exists()) {
                    Log.w("ShopsFragment", "Snapshot does not exist at path: ${database.path}")
                    tvEmpty.visibility = View.VISIBLE
                    return
                }

                for (sellerSnapshot in snapshot.children) {
                    val sellerKey = sellerSnapshot.key ?: "unknown"
                    Log.d("ShopsFragment", "Processing seller: $sellerKey")
                    Log.d("ShopsFragment", "Seller data: ${sellerSnapshot.value}")

                    val seller = sellerSnapshot.getValue(Seller::class.java)
                    if (seller != null) {
                        seller.uid = sellerKey
                        Log.d("ShopsFragment", "Successfully loaded seller: ${seller.name}, uid: ${seller.uid}")
                        shops.add(seller)
                    } else {
                        Log.e("ShopsFragment", "Failed to deserialize seller: $sellerKey")
                        // Try manual mapping as fallback
                        try {
                            val name = sellerSnapshot.child("name").getValue(String::class.java) ?: ""
                            val address = sellerSnapshot.child("address").getValue(String::class.java) ?: ""
                            val phone = sellerSnapshot.child("phone").getValue(String::class.java) ?: ""
                            val email = sellerSnapshot.child("email").getValue(String::class.java) ?: ""
                            val photoUrl = sellerSnapshot.child("photoUrl").getValue(String::class.java) ?: ""
                            val appType = sellerSnapshot.child("appType").getValue(String::class.java) ?: "Seller"
                            
                            val manualSeller = Seller(
                                uid = sellerKey,
                                name = name,
                                phone = phone,
                                email = email,
                                address = address,
                                appType = appType,
                                photoUrl = photoUrl,
                                shopName = ""
                            )
                            shops.add(manualSeller)
                            Log.d("ShopsFragment", "Manually created seller: ${manualSeller.name}")
                        } catch (e: Exception) {
                            Log.e("ShopsFragment", "Error creating seller manually: ${e.message}")
                        }
                    }
                }

                Log.d("ShopsFragment", "Total shops loaded: ${shops.size}")

                // Check if fragment is still attached
                if (!isAdded || context == null) {
                    Log.w("ShopsFragment", "Fragment not attached, skipping UI update")
                    return
                }

                // Ensure UI updates happen on main thread (Firebase callbacks are already on main thread, but being safe)
                if (shops.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvShops.visibility = View.GONE
                    Log.w("ShopsFragment", "No shops found")
                } else {
                    tvEmpty.visibility = View.GONE
                    rvShops.visibility = View.VISIBLE
                    
                    // Create a new list to avoid reference issues
                    val shopsCopy = ArrayList<Seller>(shops)
                    shopAdapter.updateShops(shopsCopy)
                    
                    Log.d("ShopsFragment", "Updated adapter with ${shopsCopy.size} shops")
                    Log.d("ShopsFragment", "Adapter item count: ${shopAdapter.itemCount}")
                    Log.d("ShopsFragment", "RecyclerView visibility: ${rvShops.visibility}")
                    Log.d("ShopsFragment", "RecyclerView is attached: ${rvShops.isAttachedToWindow}")
                    Log.d("ShopsFragment", "RecyclerView has layout manager: ${rvShops.layoutManager != null}")
                    Log.d("ShopsFragment", "RecyclerView has adapter: ${rvShops.adapter != null}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Log.e("ShopsFragment", "Database error: ${error.message}, code: ${error.code}")
                Toast.makeText(context, "Failed to load shops: ${error.message}", Toast.LENGTH_SHORT).show()
                tvEmpty.visibility = View.VISIBLE
            }
        })
    }
}

