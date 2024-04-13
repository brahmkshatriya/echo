package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import java.io.File
import java.io.FileInputStream

fun <T : Parcelable> Context.getFromCache(
    id: String, creator: (Parcel) -> T
): T? {
    val fileName = id.hashCode().toString()
    val file = File(cacheDir, fileName)
    return if (file.exists()) {
        tryWith {
            val bytes = FileInputStream(file).use { it.readBytes() }
            val parcel = Parcel.obtain()
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            val value = creator(parcel)
            parcel.recycle()
            value
        }
    } else {
        null
    }
}

fun <T : Parcelable> Context.saveToCache(
    id: String, value: T
) {
    val fileName = id.hashCode().toString()
    val parcel = Parcel.obtain()
    value.writeToParcel(parcel, 0)
    val bytes = parcel.marshall()
    parcel.recycle()
    File(cacheDir, fileName).outputStream().use { it.write(bytes) }
}