package com.sj9.chavara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.sj9.chavara.navigation.AppNavigation
import com.sj9.chavara.ui.theme.ChavaraTheme
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            ChavaraTheme {
                // No need to provide an ImageLoader here anymore.
                // Coil will automatically use the singleton instance
                // from ChavaraApplication.
                AppNavigation(modifier = Modifier.fillMaxSize())
            }
        }
    }
}