package com.example.signallingms1

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var backStackListenerAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val tabTitles = listOf("Profile", "Shops", "Cart")

        viewPager.adapter = MainPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
        
        // Handle tab clicks to allow navigation even when ProductsFragment is visible
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                
                // If ProductsFragment is visible, hide it and show ViewPager
                val fragmentContainer = findViewById<androidx.fragment.app.FragmentContainerView>(R.id.fragmentContainer)
                if (fragmentContainer?.visibility == View.VISIBLE) {
                    // Pop back stack to hide ProductsFragment
                    supportFragmentManager.popBackStack()
                    fragmentContainer.visibility = View.GONE
                    viewPager.visibility = View.VISIBLE
                }
                
                // Switch to selected tab
                viewPager.setCurrentItem(position, false)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        // Set up back stack listener once
        setupBackStackListener()
    }
    
    fun setShopSelectionCallback(shopsFragment: ShopsFragment) {
        shopsFragment.onShopSelected = { seller ->
            navigateToProducts(seller)
        }
    }
    
    private fun setupBackStackListener() {
        if (!backStackListenerAdded) {
            supportFragmentManager.addOnBackStackChangedListener {
                if (supportFragmentManager.backStackEntryCount == 0) {
                    val fragmentContainer = findViewById<androidx.fragment.app.FragmentContainerView>(R.id.fragmentContainer)
                    fragmentContainer?.visibility = View.GONE
                    viewPager.visibility = View.VISIBLE
                }
            }
            backStackListenerAdded = true
        }
    }

    private fun navigateToProducts(seller: Seller) {
        try {
            val productsFragment = ProductsFragment().apply {
                arguments = Bundle().apply {
                    putString("sellerId", seller.uid)
                    putString("sellerName", seller.shopName.ifEmpty { seller.name })
                }
            }
            
            val fragmentContainer = findViewById<androidx.fragment.app.FragmentContainerView>(R.id.fragmentContainer)
            if (fragmentContainer != null) {
                fragmentContainer.visibility = View.VISIBLE
                viewPager.visibility = View.GONE
                
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, productsFragment, "products_fragment")
                    .addToBackStack("shops")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commitAllowingStateLoss()
            } else {
                Toast.makeText(this, "Error: Fragment container not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error navigating to products: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private class MainPagerAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ProfileFragment()
                1 -> ShopsFragment()
                2 -> CartFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
