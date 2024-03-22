package dev.brahmkshatriya.echo.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.button.MaterialButton
import dev.brahmkshatriya.echo.common.models.ImageHolder

fun <T> ImageHolder?.createRequest(
    requestBuilder: RequestBuilder<T>, placeholder: Int? = null, errorDrawable: Int? = null
): RequestBuilder<T> {
    var error = errorDrawable
    if (error == null) error = placeholder

    if (this == null) return requestBuilder.load(error)

    var request = when (this) {
        is ImageHolder.BitmapHolder -> requestBuilder.load(bitmap)

        is ImageHolder.UrlHolder -> requestBuilder.load(GlideUrl(url) { headers })

        is ImageHolder.UriHolder -> requestBuilder.load(uri)
    }
    request = placeholder?.let { request.placeholder(it) } ?: request
    request = error?.let { request.error(it) } ?: request
    return if (crop) request.centerCrop() else request.centerInside()
}

fun ImageHolder?.loadInto(
    imageView: ImageView, placeholder: Int? = null, errorDrawable: Int? = null
) {
    val builder = Glide.with(imageView).asDrawable()
    val request = createRequest(builder, placeholder, errorDrawable)
    request.into(imageView)
}

fun ImageHolder?.loadWith(
    imageView: ImageView,
    placeholder: Int? = null,
    errorDrawable: Int? = null,
    block: () -> Unit
) {
    val builder = Glide.with(imageView).asDrawable()
    val request = createRequest(builder, placeholder, errorDrawable)
    request.into(ImageTarget(imageView, block))
}

class ImageTarget(private val imageView: ImageView, private val block: () -> Unit) :
    CustomViewTarget<ImageView, Drawable>(imageView) {
    private fun setDrawable(drawable: Drawable?) {
        imageView.setImageDrawable(drawable)
        tryWith(false) { block() }
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        setDrawable(errorDrawable)
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        setDrawable(placeholder)
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        setDrawable(resource)
    }
}


fun ImageHolder?.loadInto(
    button: MaterialButton, placeholder: Int? = null, errorDrawable: Int? = null
) {
    val builder = Glide.with(button).asDrawable()
    val request = createRequest(builder, placeholder, errorDrawable)
    request.into(MaterialButtonTarget(button))
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