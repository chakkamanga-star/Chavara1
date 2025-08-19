package com.sj9.chavara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.sj9.chavara.navigation.AppNavigation
import com.sj9.chavara.ui.theme.ChavaraTheme
import androidx.core.view.WindowCompat

import coil.compose.LocalImageLoader
import com.sj9.chavara.data.service.ImageLoaderModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            // Get the shared ImageLoader instance from the new, optimized module
            val sharedImageLoader = ImageLoaderModule.getSharedImageLoader(applicationContext)

            ChavaraTheme {
                // Provide the shared loader to all Composables inside the app
                CompositionLocalProvider(LocalImageLoader provides sharedImageLoader) {
                    AppNavigation(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
