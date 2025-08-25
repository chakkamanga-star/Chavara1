package com.sj9.chavara

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.sj9.chavara.data.service.ImageLoaderModule

class ChavaraApplication : Application(), ImageLoaderFactory {
    /**
     * Lazily creates and returns a singleton ImageLoader instance.
     *
     * This function is part of the ImageLoaderFactory interface and is called
     * automatically by Coil to get the app-wide ImageLoader. This approach
     * centralizes the ImageLoader creation, ensuring a single instance
     * with a shared memory and disk cache, which is optimal for performance.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoaderModule.create(this)
    }
}