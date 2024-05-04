package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import java.io.File
import java.io.FileInputStream

fun cacheDir(context: Context, folderName: String) =
    File(context.cacheDir, folderName).apply { mkdirs() }

fun <T> Context.getFromCache(
    id: String, folderName: String, creator: (Parcel) -> T?
): T? {
    val fileName = id.hashCode().toString()
    val cacheDir = cacheDir(this, folderName)
    val file = File(cacheDir, fileName)
    return if (file.exists()) tryWith {
        val bytes = FileInputStream(file).use { it.readBytes() }
        val parcel = Parcel.obtain()
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
        val value = creator(parcel)
        parcel.recycle()
        value
    } else null
}

fun Context.saveToCache(
    id: String, folderName: String, writer: (Parcel) -> Unit
) {
    val fileName = id.hashCode().toString()
    val cacheDir = cacheDir(this, folderName)
    val parcel = Parcel.obtain()
    writer(parcel)
    val bytes = parcel.marshall()
    parcel.recycle()
    File(cacheDir, fileName).outputStream().use { it.write(bytes) }
}

inline fun <reified T : Parcelable> Context.getFromCache(
    id: String, creator: Parcelable.Creator<T>, folderName: String? = null
) = getFromCache(id, folderName ?: T::class.java.simpleName) { creator.createFromParcel(it) }

inline fun <reified T : Parcelable> Context.saveToCache(
    id: String, value: T, folderName: String? = null
) = saveToCache(id, folderName ?: T::class.java.simpleName) { value.writeToParcel(it, 0) }

inline fun <reified T : Parcelable> Context.saveToCache(
    id: String, value: List<T>, folderName: String? = null
) = saveToCache(id, folderName ?: T::class.java.simpleName) { it.writeTypedList(value) }

inline fun <reified T : Parcelable> Context.getListFromCache(
    id: String, creator: Parcelable.Creator<T>, folderName: String? = null
) = getFromCache(id, folderName ?: T::class.java.simpleName) { it.createTypedArrayList(creator) }
