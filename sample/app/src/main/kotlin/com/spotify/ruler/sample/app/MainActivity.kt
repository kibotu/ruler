package com.spotify.ruler.sample.app

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.spotify.ruler.sample.lib.ClassToObfuscate

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", ClassToObfuscate.string())
    }
}
