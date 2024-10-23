package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import coil3.Bitmap
import coil3.Image
import coil3.asDrawable
import coil3.imageLoader
import coil3.load
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import coil3.request.transformations
import coil3.target.GenericViewTarget
import coil3.toBitmap
import coil3.transform.CircleCropTransformation
import coil3.transform.Transformation
import dev.brahmkshatriya.echo.common.models.ImageHolder

private fun <T> tryWith(print: Boolean = false, block: () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        if (print) e.printStackTrace()
        null
    }
}

private suspend fun <T> tryWithSuspend(print: Boolean = true, block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        if (print) e.printStackTrace()
        null
    }
}

fun View.enqueue(builder: ImageRequest.Builder) = context.imageLoader.enqueue(builder.build())

fun ImageHolder?.loadInto(
    imageView: ImageView, placeholder: Int? = null, errorDrawable: Int? = null
) = tryWith {
    val request = createRequest(imageView.context, placeholder, errorDrawable)
    request.target(imageView)
    imageView.enqueue(request)
}

fun ImageHolder?.loadWithThumb(
    imageView: ImageView, thumbnail: ImageHolder? = null,
    error: Int? = null, onDrawable: (Drawable?) -> Unit = {}
) = tryWith {
    if (this == null) {
        thumbnail.loadWith(imageView, null, error, onDrawable)
        return@tryWith
    }
    val request = createRequest(imageView.context, null, error)
    request.target(ViewTarget(imageView) {
        imageView.load(it)
        tryWith(false) { onDrawable(it) }
    })
    imageView.enqueue(request)
}

fun ImageHolder?.loadWith(
    imageView: ImageView, placeholder: Int? = null,
    error: Int? = null, onDrawable: (Drawable?) -> Unit = {}
) = tryWith {
    val request = createRequest(imageView.context, placeholder, error)
    request.target(ViewTarget(imageView) {
        imageView.load(it)
        tryWith(false) { onDrawable(it) }
    })
    imageView.enqueue(request)
}

class ViewTarget(
    override val view: View,
    val onDrawable: (Drawable?) -> Unit,
) : GenericViewTarget<View>() {
    private var mDrawable: Drawable? = null
    override var drawable: Drawable?
        get() = mDrawable
        set(value) {
            mDrawable = value
            onDrawable(value)
        }
}

val circleCrop = CircleCropTransformation()
val squareCrop = SquareCropTransformation()
fun <T : View> ImageHolder?.loadAsCircle(
    view: T, placeholder: Int? = null, errorDrawable: Int? = null, onDrawable: (Drawable?) -> Unit
) = tryWith {
    val request = createRequest(view.context, placeholder, errorDrawable, circleCrop)
    fun setDrawable(image: Image?) {
        val drawable = image?.asDrawable(view.resources)
        tryWith(false) { onDrawable(drawable) }
    }
    request.target(::setDrawable, ::setDrawable, ::setDrawable)
    view.enqueue(request)
}

suspend fun ImageHolder?.loadBitmap(
    context: Context, placeholder: Int? = null
) = tryWithSuspend {
    val request = createRequest(context, null, placeholder)
    context.imageLoader.execute(request.build()).image?.toBitmap()
}

fun ImageView.load(placeHolder: Int) = tryWith {
    load(placeHolder)
}

fun ImageView.load(drawable: Drawable?) = tryWith {
    load(drawable)
}

fun ImageView.load(drawable: Drawable?, size: Int) = tryWith {
    load(drawable) { size(size) }
}

fun ImageView.loadBlurred(bitmap: Bitmap?, radius: Float) = tryWith {
    load(bitmap) {
        transformations(BlurTransformation(context, radius))
        crossfade(false)
    }
}

private fun createRequest(
    imageHolder: ImageHolder,
    builder: ImageRequest.Builder,
) = imageHolder.run {
    when (this) {
        is ImageHolder.UriImageHolder -> builder.data(uri)
        is ImageHolder.UrlRequestImageHolder -> {
            if (request.headers.isNotEmpty())
                builder.httpHeaders(NetworkHeaders.Builder().apply {
                    request.headers.forEach { (t, u) -> add(t, u) }
                }.build())
            builder.data(request.url)
        }
    }
}

private fun ImageHolder?.createRequest(
    context: Context,
    placeholder: Int?,
    errorDrawable: Int?,
    vararg transformations: Transformation
): ImageRequest.Builder {
    val builder = ImageRequest.Builder(context)
    var error = errorDrawable
    if (error == null) error = placeholder

    if (this == null) {
        if (error != null) builder.data(error)
        return builder
    }
    createRequest(this, builder)
    placeholder?.let { builder.placeholder(it) }
    error?.let { builder.error(it) }
    val list = if (crop) listOf(squareCrop, *transformations) else transformations.toList()
    if (list.isNotEmpty()) builder.transformations(list)
    return builder
}