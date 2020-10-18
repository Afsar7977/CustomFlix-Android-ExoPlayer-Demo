@file:Suppress("PrivatePropertyName", "DEPRECATION")

package com.afsar.customflix

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.afsar.customflix.Activity.Home

class MainActivity : AppCompatActivity() {
    private val SPLASH_TIME_OUT = 3000L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Handler().postDelayed(
            {
                val intent  = Intent(applicationContext, Home::class.java)
                startActivity(intent)
                finish()
            }, SPLASH_TIME_OUT
        )
    }
}