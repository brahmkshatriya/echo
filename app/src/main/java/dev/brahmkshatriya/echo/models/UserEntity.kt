package dev.brahmkshatriya.echo.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.TypeConverters
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.models.ImageHolderEntity.Companion.toEntity
import dev.brahmkshatriya.echo.models.ImageHolderEntity.Companion.toImageHolder
import dev.brahmkshatriya.echo.utils.MapTypeConverter

@Entity(primaryKeys = ["id", "clientId"])
@TypeConverters(MapTypeConverter::class)
data class UserEntity(
    val clientId: String,
    val id: String,
    val name: String,
    @Embedded("cover_")
    val cover: ImageHolderEntity? = null,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf()
) {
    companion object {
        fun User.toEntity(clientId: String) =
            UserEntity(clientId, id, name, cover?.toEntity(), null, extras)

        fun UserEntity.toUser() =
            User(id, name, cover?.toImageHolder(), extras)

        fun UserEntity.toCurrentUser() =
            CurrentUser(clientId, id)
    }
}
