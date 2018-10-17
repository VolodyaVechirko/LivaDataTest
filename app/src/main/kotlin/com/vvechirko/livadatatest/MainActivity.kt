package com.vvechirko.livadatatest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vvechirko.livadatatest.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment())
                    .commitNow()
        }
    }

    fun addFragment() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment())
                .addToBackStack(null)
                .commit()
    }
}
