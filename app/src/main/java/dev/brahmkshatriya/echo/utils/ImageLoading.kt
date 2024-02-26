package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.material.button.MaterialButton
import dev.brahmkshatriya.echo.common.models.ImageHolder
import okhttp3.Headers.Companion.toHeaders

private fun ImageHolder?.createRequest(
    context: Context,
    placeholder: Int?,
    errorDrawable: Int?
): ImageRequest.Builder {
    val request = ImageRequest.Builder(context)
    var error = errorDrawable
    if (error == null) error = placeholder

    if (this == null) {
        if (error != null) request.data(error)
        return request
    }
    when (this) {
        is ImageHolder.BitmapHolder -> request.data(bitmap)
        is ImageHolder.UrlHolder -> {
            request.data(url)
            request.headers(headers.toHeaders())
        }
        is ImageHolder.UriHolder -> request.data(uri)
    }
    placeholder?.let { request.placeholder(it) }
    error?.let { request.error(it) }
    return request
}

fun ImageHolder?.loadInto(
    imageView: ImageView,
    placeholder: Int? = null,
    errorDrawable: Int? = null
) {
    var request = createRequest(imageView.context, placeholder, errorDrawable)
    request = request.target(imageView)
    imageView.context.imageLoader.enqueue(request.build())
}

fun ImageHolder?.loadInto(
    button: MaterialButton,
    placeholder: Int? = null,
    errorDrawable: Int? = null
) {
    var request = createRequest(button.context, placeholder, errorDrawable)
    val icon: (Drawable?) -> Unit = { button.icon = it }
    request = request.target(icon, icon, icon)
    button.context.imageLoader.enqueue(request.build())
}