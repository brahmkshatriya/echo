package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class EchoGlideApp : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setLogLevel(Log.ERROR)
        val diskCacheSizeBytes = 1024 * 1024 * 100L // 100 MB
        builder.setDiskCache(
            InternalCacheDiskCacheFactory(
                context,
                "imageCache",
                diskCacheSizeBytes
            )
        )
        builder.setDefaultTransitionOptions(
            Drawable::class.java,
            withCrossFade()
        )
    }
}