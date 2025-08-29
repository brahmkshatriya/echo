package dev.brahmkshatriya.echo.extensions.builtin.unified

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedDatabase.PlaylistEntity.Companion.toEntity
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedDatabase.PlaylistTrackEntity.Companion.toEntity
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedDatabase.SavedEntity.Companion.toEntity
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.EXTENSION_ID
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.UNIFIED_ID
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.extensionId
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import java.util.Calendar

@Database(
    entities = [
        UnifiedDatabase.PlaylistEntity::class,
        UnifiedDatabase.PlaylistTrackEntity::class,
        UnifiedDatabase.SavedEntity::class,
    ],
    version = 4,
    exportSchema = false
)
abstract class UnifiedDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    private val dao by lazy { playlistDao() }

    suspend fun getCreatedPlaylists(): List<Playlist> {
        return dao.getPlaylists().filter { it.actualId.isEmpty() }.map { it.playlist }
    }

    suspend fun getSaved(): List<EchoMediaItem> {
        return dao.getSaved().map { it.item }
    }

    suspend fun isSaved(item: EchoMediaItem): Boolean {
        val extId = item.extras.extensionId
        return dao.isSaved(item.id, extId)
    }

    suspend fun save(item: EchoMediaItem) {
        dao.insertSaved(item.toEntity())
    }

    suspend fun deleteSaved(item: EchoMediaItem) {
        dao.deleteSaved(item.toEntity())
    }

    private fun getDateNow(): Date {
        val calendar = Calendar.getInstance()
        calendar.time = java.util.Date()
        return Date(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH),
            day = calendar.get(Calendar.DAY_OF_MONTH),
        )
    }

    suspend fun getLikedPlaylist(context: Context): Playlist {
        val liked = dao.getPlaylist("Liked")
        return if (liked == null) {
            val playlist = PlaylistEntity(
                0,
                getDateNow().toJson(),
                "Liked",
                context.getString(R.string.unified_liked_playlist_summary),
                null
            )
            val id = dao.insertPlaylist(playlist)
            playlist.copy(id = id).playlist
        } else liked.playlist
    }

    suspend fun createPlaylist(
        title: String,
        description: String?,
        cover: ImageHolder? = null,
        actualId: String = ""
    ): Playlist {
        val playlist = PlaylistEntity(
            0,
            getDateNow().toJson(),
            title,
            description ?: "",
            cover?.toJson(),
            actualId = actualId
        )
        val id = dao.insertPlaylist(playlist)
        return playlist.copy(id = id).playlist
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        dao.deleteAllTracks(playlist.id.toLong())
        dao.deletePlaylist(playlist.toEntity())
    }

    suspend fun editPlaylistMetadata(playlist: Playlist, title: String, description: String?) {
        val entity = playlist.toEntity().copy(name = title, description = description ?: "")
        dao.insertPlaylist(entity)
    }

    suspend fun loadPlaylist(playlist: Playlist): Playlist {
        val entity = dao.getPlaylist(playlist.toEntity().id)
        val tracks = dao.getTracks(entity.id).map { it.track }
        if (tracks.isEmpty()) return playlist.copy(trackCount = 0, duration = null)
        val durations = tracks.mapNotNull { it.duration }
        val average = durations.average().toLong()
        return entity.playlist.copy(
            trackCount = tracks.size.toLong(),
            duration = average * tracks.size
        )
    }

    suspend fun getTracks(playlist: Playlist): List<Track> {
        val entity = playlist.toEntity()
        val new = mutableListOf<Track>()
        val tracks = dao.getTracks(entity.id).associateBy { it.eid }
        if (tracks.isEmpty()) return emptyList()
        var last = entity.last
        if (last !in tracks) return new // A workaround when last track of playlist is updated by removeTracksFromPlaylist() while having old playlist here
        while (last != null) {
            val track = tracks[last]!!
            new.add(track.track)
            last = track.after
        }
        return new.reversed()
    }

    suspend fun addTracksToPlaylist(
        playlist: Playlist, tracks: List<Track>, index: Int, new: List<Track>
    ) {
        if (new.isEmpty()) return
        val entity = dao.getPlaylist(playlist.toEntity().id)
        val trackEntities = tracks.map { it.toEntity() }
        var before = trackEntities.getOrNull(index - 1)?.eid
        val after = trackEntities.getOrNull(index)
        val newTrackEntities = new.map {
            val trackEntity = PlaylistTrackEntity(
                0, entity.id, it.id, it.extras.extensionId, it.toJson(), before
            )
            val id = dao.insertPlaylistTrack(trackEntity)
            before = id
            trackEntity.copy(eid = id)
        }
        val last = newTrackEntities.last()
        if (after != null) dao.insertPlaylistTrack(after.copy(after = last.eid))
        else dao.insertPlaylist(entity.copy(last = last.eid))
    }

    suspend fun removeTracksFromPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        indexes: List<Int>
    ) {
        indexes.forEach {
            val track = dao.getTrack(tracks[it].toEntity().eid)!!
            dao.deletePlaylistTrack(track)

            val after = dao.getAfterTrack(track.eid)
            if (after != null) dao.insertPlaylistTrack(after.copy(after = track.after))

            val entity = dao.getPlaylist(playlist.toEntity().id)
            if (entity.last == track.eid) dao.insertPlaylist(entity.copy(last = track.after))
        }
    }

    suspend fun moveTrack(playlist: Playlist, tracks: List<Track>, from: Int, to: Int) {
        val diff = from - to
        if (diff == 0) return

        val entity = dao.getPlaylist(playlist.toEntity().id)
        val trackEntities = tracks.map { it.toEntity() }
        val toChange = trackEntities[from]

        val afterFrom = dao.getTrack(trackEntities.getOrNull(from + 1)?.eid)
        if (afterFrom != null) dao.insertPlaylistTrack(afterFrom.copy(after = toChange.after))
        else dao.insertPlaylist(entity.copy(last = toChange.after))

        val oldTo = dao.getTrack(trackEntities.getOrNull(to)?.eid)
        if (diff > 0) {
            if (oldTo != null) {
                dao.insertPlaylistTrack(toChange.copy(after = oldTo.after))
                dao.insertPlaylistTrack(oldTo.copy(after = toChange.eid))
            }
        } else {
            val afterTo = dao.getTrack(trackEntities.getOrNull(to + 1)?.eid)
            if (afterTo != null) {
                dao.insertPlaylistTrack(toChange.copy(after = afterTo.after))
                dao.insertPlaylistTrack(afterTo.copy(after = toChange.eid))
            } else {
                dao.insertPlaylistTrack(toChange.copy(after = oldTo?.eid))
                dao.insertPlaylist(entity.copy(last = toChange.eid))
            }
        }
    }

    suspend fun isLiked(track: Track): Boolean {
        val liked = dao.getPlaylist("Liked") ?: return false
        return dao.getTracks(liked.id)
            .any { it.trackId == track.id && it.extId == track.extras.extensionId }
    }

    suspend fun getOrCreate(app: Context, context: EchoMediaItem): Playlist {
        return dao.getPlaylistByActualId(context.id)?.playlist ?: createPlaylist(
            context.title,
            app.getString(R.string.downloaded_x, ""),
            context.cover,
            context.id
        )
    }

    suspend fun getPlaylist(mediaItem: EchoMediaItem): Playlist? {
        return dao.getPlaylistByActualId(mediaItem.id)?.playlist
    }

    @Dao
    interface PlaylistDao {
        @Query("SELECT * FROM PlaylistEntity")
        suspend fun getPlaylists(): List<PlaylistEntity>

        @Query("SELECT * FROM PlaylistEntity WHERE id = :id")
        suspend fun getPlaylist(id: Long): PlaylistEntity

        @Query("SELECT * FROM PlaylistEntity WHERE name = :name")
        suspend fun getPlaylist(name: String): PlaylistEntity?

        @Query("SELECT * FROM PlaylistEntity WHERE actualId = :actualId")
        suspend fun getPlaylistByActualId(actualId: String): PlaylistEntity?

        @Insert(onConflict = REPLACE)
        suspend fun insertPlaylist(playlist: PlaylistEntity): Long

        @Delete
        suspend fun deletePlaylist(playlist: PlaylistEntity)

        @Query("SELECT * FROM PlaylistTrackEntity WHERE playlistId = :playlistId")
        suspend fun getTracks(playlistId: Long): List<PlaylistTrackEntity>

        @Insert(onConflict = REPLACE)
        suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrackEntity): Long

        @Delete
        suspend fun deletePlaylistTrack(playlistTrack: PlaylistTrackEntity)

        @Query("DELETE FROM PlaylistTrackEntity WHERE playlistId = :playlistId")
        suspend fun deleteAllTracks(playlistId: Long)

        @Query("SELECT * FROM SavedEntity")
        suspend fun getSaved(): List<SavedEntity>

        @Query("SELECT EXISTS(SELECT 1 FROM SavedEntity WHERE id = :id AND extId = :extId)")
        suspend fun isSaved(id: String, extId: String): Boolean

        @Insert(onConflict = REPLACE)
        suspend fun insertSaved(saved: SavedEntity): Long

        @Delete
        suspend fun deleteSaved(saved: SavedEntity)

        @Query("SELECT * FROM PlaylistTrackEntity WHERE eid = :eid")
        suspend fun getTrack(eid: Long?): PlaylistTrackEntity?

        @Query("SELECT * FROM PlaylistTrackEntity WHERE \"after\" = :eid")
        suspend fun getAfterTrack(eid: Long): PlaylistTrackEntity?
    }

    @Entity
    data class PlaylistEntity(
        @PrimaryKey(true)
        val id: Long,
        val modified: String,
        val name: String,
        val description: String,
        val cover: String?,
        val last: Long? = null,
        val actualId: String = ""
    ) {
        val playlist by lazy {
            Playlist(
                id.toString(),
                name,
                true,
                isPrivate = false,
                cover = cover?.toData(),
                creationDate = modified.toData<Date>(),
                description = description.takeIf { it.isNotBlank() },
                extras = mapOf(
                    EXTENSION_ID to UNIFIED_ID,
                    "last" to last.toString(),
                    "actualId" to actualId
                )
            )
        }

        companion object {
            fun Playlist.toEntity(): PlaylistEntity {
                return PlaylistEntity(
                    id.toLong(),
                    creationDate.toJson(),
                    title,
                    description ?: "",
                    cover?.toJson(),
                    extras["last"]?.toLongOrNull(),
                    extras["actualId"] ?: ""
                )
            }
        }
    }

    @Entity
    data class PlaylistTrackEntity(
        @PrimaryKey(autoGenerate = true)
        val eid: Long,
        val playlistId: Long,
        val trackId: String,
        val extId: String,
        val data: String,
        val after: Long? = null,
    ) {
        val track by lazy {
            data.toData<Track>().run {
                this.copy(
                    type = type,
                    extras = extras + mapOf(
                        "pId" to playlistId.toString(),
                        "eId" to eid.toString(),
                        "after" to after.toString()
                    )
                )
            }
        }

        companion object {
            fun Track.toEntity(): PlaylistTrackEntity {
                val pId = extras["pId"]!!.toLong()
                val eId = extras["eId"]!!.toLong()
                val after = extras["after"]?.toLongOrNull()
                return PlaylistTrackEntity(eId, pId, id, extras.extensionId, this.toJson(), after)
            }
        }
    }

    @Entity(primaryKeys = ["id", "extId"])
    data class SavedEntity(
        val id: String,
        val extId: String,
        val data: String,
    ) {
        val item by lazy { data.toData<EchoMediaItem>() }

        companion object {
            fun EchoMediaItem.toEntity(): SavedEntity {
                val extId = extras.extensionId
                return SavedEntity(id, extId, this.toJson())
            }
        }
    }
}
