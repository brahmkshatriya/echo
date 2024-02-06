package dev.brahmkshatriya.echo.ui.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import dev.brahmkshatriya.echo.data.models.ImageHolder

fun ImageHolder.loadInto(imageView: ImageView) {
    if (this is ImageHolder.BitmapHolder)
        Glide.with(imageView)
            .load(this.bitmap)
            .into(imageView)
    if (this is ImageHolder.UrlHolder)
        Glide.with(imageView)
            .load(GlideUrl(this.url) { this.headers })
            .into(imageView)
    if (this is ImageHolder.UriHolder)
        Glide.with(imageView)
            .load(this.uri)
            .into(imageView)
}