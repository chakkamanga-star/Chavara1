package com.sj9.chavara.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

/**
 * A custom VideoView that overrides onMeasure to force a fullscreen aspect ratio.
 */
class FillScreenVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // This line forces the view to fill the dimensions provided by its parent
        setMeasuredDimension(
            getDefaultSize(0, widthMeasureSpec),
            getDefaultSize(0, heightMeasureSpec)
        )
    }
}