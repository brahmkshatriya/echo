package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
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
//    val builder = Glide.with(imageView).asDrawable()
//    val request = createRequest(builder, placeholder, errorDrawable)
//    request.into(imageView)
}

fun ImageHolder?.loadWith(
    imageView: ImageView,
    placeholder: Int? = null,
    errorDrawable: Int? = null,
    onDrawable: (Drawable?) -> Unit
) {
//    val builder = Glide.with(imageView).asDrawable()
//    val request = createRequest(builder, placeholder, errorDrawable)
//    request.into(ViewTarget(imageView) {
//        imageView.setImageDrawable(it)
//        tryWith(false) { onDrawable(it) }
//    })
}

fun ImageView.load(placeHolder: Int) {
//    Glide.with(this).load(placeHolder).into(this)
}

fun ImageView.load(drawable: Drawable?) {
//    Glide.with(this).load(drawable).into(this)
}

suspend fun ImageHolder.getBitmap(context: Context): Bitmap? = when (this) {
    is ImageHolder.BitmapHolder -> bitmap
    else -> tryWithSuspend { createRequest(Glide.with(context).asBitmap()).submit().get() }
}

fun <T : View> ImageHolder?.load(
    view: T, placeholder: Int? = null, errorDrawable: Int? = null, onDrawable: (Drawable?) -> Unit
) {
//    val builder = Glide.with(view).asDrawable()
//    val request = createRequest(builder, placeholder, errorDrawable)
//    request.circleCrop().into(ViewTarget(view, onDrawable))
}

class ViewTarget<T : View>(val target: T, private val onDrawable: (Drawable?) -> Unit) :
    CustomViewTarget<View, Drawable>(target) {
    override fun onLoadFailed(errorDrawable: Drawable?) {
        onDrawable(errorDrawable)
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        onDrawable(placeholder)
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        onDrawable(resource)
    }
}