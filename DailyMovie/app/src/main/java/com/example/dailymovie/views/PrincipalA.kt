package com.example.dailymovie.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.dailymovie.R
import com.example.dailymovie.databinding.ActivityPrincipalBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class PrincipalA : AppCompatActivity() {
    private lateinit var binding: ActivityPrincipalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView:BottomNavigationView=binding.navView
        val navController =findNavController(R.id.nav_host_fragment_activity_principal)
        val appBarConfiguration= AppBarConfiguration(
            setOf(
                R.id.menu_home, R.id.menu_explorar, R.id.menu_listas
            )
        )
        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
    private fun replaceFragment(fragment: Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.nav_host_fragment_activity_principal, fragment)
        fragmentTransaction.commit()
    }
}