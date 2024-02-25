package dev.brahmkshatriya.echo.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.button.MaterialButton
import dev.brahmkshatriya.echo.common.models.ImageHolder

fun ImageHolder?.loadInto(
    imageView: ImageView,
    placeholder: Int? = null,
    errorDrawable: Int? = null
) {
    var error = errorDrawable
    if (error == null) error = placeholder

    if (this == null) {
        if (error != null) Glide.with(imageView).load(error).into(imageView)
        return
    }

    var builder = when (this) {
        is ImageHolder.BitmapHolder -> Glide.with(imageView).load(this.bitmap)
        is ImageHolder.UrlHolder -> Glide.with(imageView).load(GlideUrl(this.url) { this.headers })
        is ImageHolder.UriHolder -> Glide.with(imageView).load(this.uri)
    }
    placeholder?.let { builder = builder.placeholder(it) }
    error?.let { builder = builder.error(it) }
    builder.into(imageView)
}

fun ImageHolder?.loadInto(
    button: MaterialButton,
    placeholder: Int? = null,
    errorDrawable: Int? = null
) {
    var error = errorDrawable
    if (error == null) error = placeholder

    if (this == null) {
        if (error != null) Glide.with(button).load(error).into(MaterialButtonTarget(button))
        return
    }
    var builder = when (this) {
        is ImageHolder.BitmapHolder -> Glide.with(button).load(this.bitmap)
        is ImageHolder.UrlHolder -> Glide.with(button).load(GlideUrl(this.url) { this.headers })
        is ImageHolder.UriHolder -> Glide.with(button).load(this.uri)
    }
    placeholder?.let { builder = builder.placeholder(it) }
    error?.let { builder = builder.error(it) }
    builder.into(MaterialButtonTarget(button))
}

class MaterialButtonTarget(private val button: MaterialButton) :
    CustomViewTarget<MaterialButton, Drawable>(button) {
    override fun onLoadFailed(errorDrawable: Drawable?) {
        button.icon = errorDrawable
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        button.icon = placeholder
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        button.icon = resource
    }
}