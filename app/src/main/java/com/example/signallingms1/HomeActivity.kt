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
    private lateinit var shopsFragment: ShopsFragment
    private var backStackListenerAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        shopsFragment = ShopsFragment()

        // Set up shop selection callback - navigate to products fragment
        shopsFragment.onShopSelected = { seller ->
            navigateToProducts(seller)
        }

        val fragments = listOf<Fragment>(
            ProfileFragment(),
            shopsFragment,
            CartFragment()
        )

        val tabTitles = listOf("Profile", "Shops", "Cart")

        viewPager.adapter = MainPagerAdapter(this, fragments)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
        
        // Set up back stack listener once
        setupBackStackListener()
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
        fragmentActivity: FragmentActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}
