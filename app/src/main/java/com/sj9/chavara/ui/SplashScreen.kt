package com.sj9.chavara.ui



import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

import android.media.MediaPlayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.fillMaxSize
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import androidx.core.net.toUri

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // This state ensures we only navigate once
    val navigated = remember { mutableStateOf(false) }

    fun navigateOnce() {
        if (!navigated.value) {
            navigated.value = true
            onSplashComplete()
        }
    }

    // This handles the minimum 5-second splash time
    LaunchedEffect(Unit) {
        delay(5000)
        navigateOnce()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black // A black background is better for video
    ) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    clipToPadding = false
                    val videoView = FillScreenVideoView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )

                    val uri = "android.resource://${context.packageName}/${R.raw.video_11}".toUri()
                    setVideoURI(uri)

                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                        start()
                    }

                    // Navigate as soon as the video finishes
                    setOnCompletionListener {
                        navigateOnce()
                    }

                    setOnErrorListener { _, what, extra ->
                        android.util.Log.e("SplashVideo", "Video error: what=$what, extra=$extra")
                        // If video fails, the timeout will still navigate
                        true
                    }
                }
                    addView(videoView)
                }
            },
            // This is the key change for fixing the alignment
            modifier = Modifier.fillMaxSize()
        )
    }
}
@Preview(
    name = "Splash Screen",
    showBackground = true,
    widthDp = 384,
    heightDp = 917
)
@Composable
fun SplashScreenPreview() {
    ChavaraTheme {
        SplashScreen(onSplashComplete = {})
    }
}
