package dev.brahmkshatriya.echo.offline

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.preference.PreferenceManager
import dev.brahmkshatriya.echo.R
import java.io.File
import java.util.PriorityQueue

/**
 * Blatantly kanged from [Gramophone](https://github.com/AkaneTan/Gramophone/blob/beta/app/src/main/kotlin/org/akanework/gramophone/logic/utils/MediaStoreUtils.kt)
 */
object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"
    private const val DEBUG_MISSING_SONG = false

    interface Item {
        val id: Long?
        val title: String?
        val songList: MutableSet<MediaItem>
    }

    /**
     * [MAlbum] stores Album metadata.
     */
    interface MAlbum : Item {
        override val id: Long?
        override val title: String?
        val artists: List<MArtist?>
        val albumYear: Int?
        val cover: Uri?
        override val songList: MutableSet<MediaItem>
    }

    private data class AlbumImpl(
        override val id: Long?,
        override val title: String?,
        override val artists: List<MArtist?>,
        override val albumYear: Int?,
        override var cover: Uri?,
        override val songList: MutableSet<MediaItem>
    ) : MAlbum

    /**
     * [MArtist] stores Artist metadata.
     */
    data class MArtist(
        override val id: Long?,
        override val title: String?,
        override val songList: MutableSet<MediaItem>,
        val albumList: MutableList<MAlbum>,
    ) : Item

    /**
     * [Genre] stores Genre metadata.
     */
    data class Genre(
        override val id: Long?,
        override val title: String?,
        override val songList: MutableSet<MediaItem>,
    ) : Item

    /**
     * [Date] stores Date metadata.
     */
    data class Date(
        override val id: Long,
        override val title: String?,
        override val songList: MutableSet<MediaItem>,
    ) : Item

    /**
     * [MPlaylist] stores playlist information.
     */
    open class MPlaylist(
        override val id: Long,
        override val title: String?,
        override val songList: MutableSet<MediaItem>,
        val description: String?,
        val modifiedDate: Long
    ) : Item


    /**
     * [LibraryStoreClass] collects above metadata classes
     * together for more convenient reading/writing.
     */
    @Suppress("unused")
    class LibraryStoreClass(
        val songList: MutableList<MediaItem>,
        val albumList: MutableList<MAlbum>,
        val albumArtistList: MutableList<MArtist>,
        val artistMap: MutableMap<Long?, MArtist>,
        val genreList: MutableList<Genre>,
        val dateList: MutableList<Date>,
        val playlistList: MutableList<MPlaylist>,
        val likedPlaylist: MPlaylist,
        val folderStructure: FileNode,
        val shallowFolder: FileNode,
        val folders: Set<String>
    )

    class FileNode(
        val folderName: String
    ) {
        val folderList = hashMapOf<String, FileNode>()
        val songList = mutableListOf<MediaItem>()
        var albumId: Long? = null
            private set

        fun addSong(item: MediaItem, id: Long?) {
            if (albumId != null && id != albumId) {
                albumId = null
            } else if (albumId == null && songList.isEmpty()) {
                albumId = id
            }
            songList.add(item)
        }
    }

    private fun handleMediaFolder(path: String, rootNode: FileNode): FileNode {
        val newPath = if (path.endsWith('/')) path.substring(1, path.length - 1)
        else path.substring(1)
        val splitPath = newPath.split('/')
        var node: FileNode = rootNode
        for (fld in splitPath.subList(0, splitPath.size - 1)) {
            var newNode = node.folderList[fld]
            if (newNode == null) {
                newNode = FileNode(fld)
                node.folderList[newNode.folderName] = newNode
            }
            node = newNode
        }
        return node
    }

    private fun handleShallowMediaItem(
        mediaItem: MediaItem,
        albumId: Long?,
        path: String,
        shallowFolder: FileNode,
        folderArray: MutableList<String>
    ) {
        val newPath = if (path.endsWith('/')) path.substring(0, path.length - 1) else path
        val splitPath = newPath.split('/')
        if (splitPath.size < 2) throw IllegalArgumentException("splitPath.size < 2: $newPath")
        val lastFolderName = splitPath[splitPath.size - 2]
        var folder = shallowFolder.folderList[lastFolderName]
        if (folder == null) {
            folder = FileNode(lastFolderName)
            shallowFolder.folderList[folder.folderName] = folder
            // hack to cut off /
            folderArray.add(
                newPath.substring(0, splitPath[splitPath.size - 1].length + 1)
            )
        }
        folder.addSong(mediaItem, albumId)
    }

    /**
     * [getAllSongs] gets all of your songs from your local disk.
     *
     * @param context
     * @return
     */
    fun getAllSongs(context: Context): LibraryStoreClass {
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0" +
                listOf(
                    "audio/x-wav",
                    "audio/ogg",
                    "audio/aac",
                    "audio/midi"
                ).joinToString("") { " or ${MediaStore.Audio.Media.MIME_TYPE} = '$it'" }
        val projection =
            arrayListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED
            ).apply {
                if (hasImprovedMediaStore()) {
                    add(MediaStore.Audio.Media.GENRE)
                    add(MediaStore.Audio.Media.GENRE_ID)
                    add(MediaStore.Audio.Media.CD_TRACK_NUMBER)
                    add(MediaStore.Audio.Media.COMPILATION)
                    add(MediaStore.Audio.Media.COMPOSER)
                    add(MediaStore.Audio.Media.DATE_TAKEN)
                    add(MediaStore.Audio.Media.WRITER)
                    add(MediaStore.Audio.Media.DISC_NUMBER)
                    add(MediaStore.Audio.Media.AUTHOR)
                }
            }.toTypedArray()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val limitValue = 10
        val haveImgPerm = if (hasScopedStorageWithMediaTypes()) context.hasImagePermission() else
            prefs.getBoolean("album_covers", false)
        val coverUri = Uri.parse("content://media/external/audio/albumart")
        val folderFilter = prefs.getStringSet("folderFilter", setOf()) ?: setOf()

        // Initialize list and maps.
        val coverCache = if (haveImgPerm) hashMapOf<Long, Pair<File, FileNode>>() else null
        val folders = hashSetOf<String>()
        val folderArray = mutableListOf<String>()
        val root = FileNode("storage")
        val shallowRoot = FileNode("shallow")
        val songs = mutableListOf<MediaItem>()
        val albumMap = mutableMapOf<Long?, AlbumImpl>()
        val artistMap = mutableMapOf<Long?, MArtist>()
        val albumArtistMap =
            hashMapOf<MArtist?, Pair<MutableSet<MAlbum>, MutableSet<MediaItem>>>()
        // Note: it has been observed on a user's Pixel(!) that MediaStore assigned 3 different IDs
        // for "Unknown genre" (null genre tag), hence we practically ignore genre IDs as key
        val genreMap = hashMapOf<String?, Genre>()
        val dateMap = hashMapOf<Int?, Date>()
        val playlists = mutableListOf<Pair<MPlaylist, MutableList<Long>>>()
        var foundPlaylistContent = false
        context.contentResolver.query(
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            arrayOf(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists._ID,
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME,
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.DATE_MODIFIED
            ),
            null, null, null
        )?.use {
            val playlistIdColumn = it.getColumnIndexOrThrow(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists._ID
            )
            val playlistNameColumn = it.getColumnIndexOrThrow(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME
            )
            val playlistModifiedDateColumn = it.getColumnIndexOrThrow(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.DATE_MODIFIED
            )
            while (it.moveToNext()) {
                val playlistId = it.getLong(playlistIdColumn)
                val playlistName = it.getString(playlistNameColumn)?.ifEmpty { null }
                val modifiedDate = it.getLong(playlistModifiedDateColumn)
                val content = mutableListOf<Long>()
                context.contentResolver.query(
                    @Suppress("DEPRECATION")
                    MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                    arrayOf(
                        @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID
                    ), null, null,
                    @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC"
                )?.use { cursor ->
                    val column = cursor.getColumnIndexOrThrow(
                        @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID
                    )
                    while (cursor.moveToNext()) {
                        foundPlaylistContent = true
                        content.add(cursor.getLong(column))
                    }
                }

                val playlist =
                    MPlaylist(playlistId, playlistName, mutableSetOf(), null, modifiedDate)
                playlists.add(Pair(playlist, content))
            }
        }

        val idMap = if (foundPlaylistContent) hashMapOf<Long, MediaItem>() else null
        val likedAudios = playlists.find { it.first.title == "Liked" }?.second ?: emptyList()
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.TITLE + " COLLATE UNICODE ASC",
        )
        val recentlyAddedMap = PriorityQueue<Pair<Long, MediaItem>>(
            // PriorityQueue throws if initialCapacity < 1
            (cursor?.count ?: 1).coerceAtLeast(1),
            Comparator { a, b ->
                // reversed int order to sort from most recent to least recent
                return@Comparator if (a.first == b.first) 0 else (if (a.first > b.first) -1 else 1)
            })
        cursor?.use {
            // Get columns from mediaStore.
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val discNumberColumn = it.getColumnIndexOrNull(MediaStore.Audio.Media.DISC_NUMBER)
            val trackNumberColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val genreColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE) else null
            val genreIdColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE_ID) else null
            val cdTrackNumberColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.CD_TRACK_NUMBER) else null
            val compilationColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPILATION) else null
            val composerColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER) else null
            val writerColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.WRITER) else null
            val authorColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.AUTHOR) else null
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addDateColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val modifiedDateColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (it.moveToNext()) {
                val path = it.getStringOrNull(pathColumn) ?: continue
                val duration = it.getLongOrNull(durationColumn)
                val pathFile = File(path)
                val fldPath = pathFile.parentFile!!.absolutePath
                val skip =
                    (duration != null && duration < limitValue * 1000) || folderFilter.contains(
                        fldPath
                    )
                // We need to add blacklisted songs to idMap as they can be referenced by playlist
                if (skip && !foundPlaylistContent) continue
                val id = it.getLongOrNull(idColumn)!!
                val title = it.getStringOrNull(titleColumn)!!
                val artist = it.getStringOrNull(artistColumn)
                    .let { v -> if (v == "<unknown>") null else v }
                val album = it.getStringOrNull(albumColumn)
                val albumArtist = it.getStringOrNull(albumArtistColumn)
                val year = it.getIntOrNull(yearColumn).let { v -> if (v == 0) null else v }
                val albumId = it.getLongOrNull(albumIdColumn)
                val mimeType = it.getStringOrNull(mimeTypeColumn)
                var discNumber = discNumberColumn?.let { col -> it.getIntOrNull(col) }
                var trackNumber = it.getIntOrNull(trackNumberColumn)
                val cdTrackNumber = cdTrackNumberColumn?.let { col -> it.getStringOrNull(col) }
                val compilation = compilationColumn?.let { col -> it.getStringOrNull(col) }
                val composer = composerColumn?.let { col -> it.getStringOrNull(col) }
                val writer = writerColumn?.let { col -> it.getStringOrNull(col) }
                val author = authorColumn?.let { col -> it.getStringOrNull(col) }
                val genre = genreColumn?.let { col -> it.getStringOrNull(col) }
                val genreId = genreIdColumn?.let { col -> it.getLongOrNull(col) }
                val addDate = it.getLongOrNull(addDateColumn)
                val modifiedDate = it.getLongOrNull(modifiedDateColumn)

                // Since we're using glide, we can get album cover with a uri.
                val imgUri = ContentUris.appendId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon(), id
                ).appendPath("albumart").build()

                // Process track numbers that have disc number added on.
                // e.g. 1001 - Disc 01, Track 01
                if (trackNumber != null && trackNumber >= 1000) {
                    discNumber = trackNumber / 1000
                    trackNumber %= 1000
                }

                // Build our mediaItem.
                val song = MediaItem
                    .Builder()
                    .setUri(pathFile.toUri())
                    .setMediaId(id.toString())
                    .setMimeType(mimeType)
                    .setMediaMetadata(
                        MediaMetadata
                            .Builder()
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setTitle(title)
                            .setWriter(writer)
                            .setCompilation(compilation)
                            .setComposer(composer)
                            .setArtist(artist)
                            .setAlbumTitle(album)
                            .setAlbumArtist(albumArtist)
                            .setArtworkUri(imgUri)
                            .setTrackNumber(trackNumber)
                            .setDiscNumber(discNumber)
                            .setGenre(genre)
                            .setReleaseYear(year)
                            .setExtras(Bundle().apply {
                                if (albumId != null) {
                                    putLong("AlbumId", albumId)
                                }
                                if (genreId != null) {
                                    putLong("GenreId", genreId)
                                }
                                putString("Author", author)
                                if (addDate != null) {
                                    putLong("AddDate", addDate)
                                }
                                if (duration != null) {
                                    putLong("Duration", duration)
                                }
                                if (modifiedDate != null) {
                                    putLong("ModifiedDate", modifiedDate)
                                }
                                putBoolean("Liked", likedAudios.contains(id))
                                cdTrackNumber?.toIntOrNull()
                                    ?.let { it1 -> putInt("CdTrackNumber", it1) }
                            })
                            .build(),
                    ).build()

                // Build our metadata maps/lists.
                idMap?.put(id, song)
                // Now that the song can be found by playlists, do NOT register other metadata.
                if (skip) continue
                songs.add(song)
                if (addDate != null) {
                    recentlyAddedMap.add(Pair(addDate, song))
                }
                fun String?.toMArtists() = this.splitArtists().map { artistName ->
                    val artistId = artistName?.id()
                    val mArtist = artistMap.getOrPut(artistId) {
                        MArtist(artistId, artistName, mutableSetOf(), mutableListOf())
                    }
                    mArtist.songList.add(song)
                    mArtist
                }
                artist.toMArtists()
                albumMap.getOrPut(albumId) {
                    // in haveImgPerm case, cover uri is created later using coverCache
                    val cover = if (haveImgPerm || albumId == null) null else
                        ContentUris.withAppendedId(coverUri, albumId)
                    val artistStr = (albumArtist ?: "").toMArtists()
                    AlbumImpl(
                        albumId,
                        album,
                        artistStr,
                        year,
                        cover,
                        mutableSetOf()
                    ).also { alb ->
                        alb.artists.forEach { mArtist ->
                            albumArtistMap.getOrPut(mArtist) {
                                Pair(mutableSetOf(), mutableSetOf())
                            }.first.add(alb)
                        }
                    }
                }.songList.add(song)

                genreMap.getOrPut(genre) {
                    Genre(genreId, genre, mutableSetOf())
                }.songList.add(song)

                dateMap.getOrPut(year) {
                    Date(year?.toLong() ?: 0, year?.toString(), mutableSetOf())
                }.songList.add(song)
                val fn = handleMediaFolder(path, root)
                fn.addSong(song, albumId)
                if (albumId != null) {
                    coverCache?.putIfAbsent(albumId, Pair(pathFile.parentFile!!, fn))
                }
                handleShallowMediaItem(song, albumId, path, shallowRoot, folderArray)
                folders.add(fldPath)
            }
        }

        // Parse all the lists.
        val allowedCoverExtensions = listOf("jpg", "png", "jpeg", "bmp", "tiff", "tif", "webp")
        val albumList = albumMap.values.onEach {
            it.artists.forEach { mArtist ->
                mArtist?.albumList?.add(it)
            }
            // coverCache == null if !haveImgPerm
            coverCache?.get(it.id)?.let { p ->
                // if this is false, folder contains >1 albums
                if (p.second.albumId == it.id) {
                    var bestScore = 0
                    var bestFile: File? = null
                    try {
                        val files = p.first.listFiles() ?: return@let
                        for (file in files) {
                            if (file.extension !in allowedCoverExtensions)
                                continue
                            var score = 1
                            when (file.extension) {
                                "jpg" -> score += 3
                                "png" -> score += 2
                                "jpeg" -> score += 1
                            }
                            if (file.nameWithoutExtension == "albumart") score += 24
                            else if (file.nameWithoutExtension == "cover") score += 20
                            else if (file.nameWithoutExtension.startsWith("albumart")) score += 16
                            else if (file.nameWithoutExtension.startsWith("cover")) score += 12
                            else if (file.nameWithoutExtension.contains("albumart")) score += 8
                            else if (file.nameWithoutExtension.contains("cover")) score += 4
                            if (bestScore < score) {
                                bestScore = score
                                bestFile = file
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, Log.getStackTraceString(e))
                    }
                    // allow .jpg or .png files with any name, but only permit more exotic
                    // formats if name contains either cover or albumart
                    if (bestScore >= 3) {
                        bestFile?.let { f -> it.cover = f.toUri() }
                    }
                }
            }
        }.toMutableList<MAlbum>()
        val albumArtistList = albumArtistMap.entries.map { (artist, albumsAndSongs) ->
            MArtist(
                artist?.id,
                artist?.title,
                albumsAndSongs.second,
                albumsAndSongs.first.toMutableList()
            )
        }.toMutableList()
        val genreList = genreMap.values.toMutableList()
        val dateList = dateMap.values.toMutableList()

        val playlistsFinal = playlists.map {
            it.first.also { playlist ->
                println("Playlist : ${playlist.title} with ${it.second.size} songs")
                playlist.songList.addAll(it.second.map { value ->
                    idMap!![value]
                        ?: if (DEBUG_MISSING_SONG) throw NullPointerException(
                            "didn't find song for id $value (playlist ${playlist.title}) in map" +
                                    " with ${idMap.size} entries"
                        )
                        else dummyMediaItem(
                            value, /* song that does not exist? */"didn't find" +
                                    "song for id $value in map with ${idMap.size} entries"
                        )
                })
            }
        }.toMutableList()

        val likedPlaylist = run {
            val liked = playlistsFinal.find { it.title == "Liked" }
            val id = liked?.id ?: context.createPlaylist("Liked")
            liked?.let { playlistsFinal.remove(it) }
            MPlaylist(
                id,
                context.getString(R.string.playlist_favourite),
                liked?.songList ?: mutableSetOf(),
                context.getString(R.string.playlist_favourite_desc),
                liked?.modifiedDate ?: System.currentTimeMillis()
            )
        }
        playlistsFinal.add(0, likedPlaylist)

        folders.addAll(folderFilter)
        return LibraryStoreClass(
            songs,
            albumList,
            albumArtistList,
            artistMap,
            genreList,
            dateList,
            playlistsFinal,
            likedPlaylist,
            root,
            shallowRoot,
            folders
        )
    }

    private fun dummyMediaItem(id: Long, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTitle(title)
                    .build()
            ).build()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun Context.hasImagePermission() =
        checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED

    private fun hasImprovedMediaStore(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private fun hasScopedStorageWithMediaTypes(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun Cursor.getColumnIndexOrNull(columnName: String): Int? =
        getColumnIndex(columnName).let { if (it == -1) null else it }

    fun Context.createPlaylist(title: String): Long {
        val values = ContentValues()
        values.put(
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME,
            title
        )
        return contentResolver.insert(
            @Suppress("DEPRECATION")
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            values
        )?.lastPathSegment!!.toLong()
    }

    fun Context.editPlaylist(id: Long, title: String) {
        val values = ContentValues()
        values.put(
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME,
            title
        )
        contentResolver.update(
            @Suppress("DEPRECATION")
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            values,
            @Suppress("DEPRECATION")
            MediaStore.Audio.Playlists._ID + "=?",
            arrayOf(id.toString())
        )
    }

    fun Context.deletePlaylist(id: Long) {
        contentResolver.delete(
            @Suppress("DEPRECATION")
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists._ID + "=?",
            arrayOf(id.toString())
        )
    }

    fun Context.addSongToPlaylist(playlistId: Long, songId: Long, index: Int) {
        val values = ContentValues()
        values.put(
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.PLAY_ORDER,
            index + 1
        )
        values.put(
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID,
            songId
        )
        contentResolver.insert(
            @Suppress("DEPRECATION")
            MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
            values
        )
    }

    fun Context.removeSongFromPlaylist(playlistId: Long, index: Int) {
        println("Removing : $index from $playlistId")
        contentResolver.delete(
            @Suppress("DEPRECATION")
            MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.PLAY_ORDER + "=?",
            arrayOf((index + 1).toString())
        )
    }

    fun Context.moveSongInPlaylist(playlistId: Long, from: Int, to: Int) {
        val uri = @Suppress("DEPRECATION")
        MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)

        val cursor = contentResolver.query(
            uri,
            arrayOf(@Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID),
            null,
            null,
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC"
        )
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID
            )
            val ids = mutableListOf<Long>()
            while (it.moveToNext()) {
                ids.add(it.getLong(idColumn))
            }
            ids.add(to, ids.removeAt(from))
            contentResolver.delete(uri, null, null)
            ids.forEachIndexed { index, id ->
                val values = ContentValues()
                values.put(
                    @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.PLAY_ORDER,
                    index + 1
                )
                values.put(
                    @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    id
                )
                contentResolver.insert(uri, values)
            }
        }
    }

    fun <E> List<E>.searchBy(query: String, block: (E) -> List<String?>) = map { item ->
        val titles = block(item).mapNotNull { it }.ifEmpty { return@map (0 to item) }
        val distance = titles.map { it to wagnerFischer(it, query) }.minBy { it.second }
        val bonus = if (distance.first.contains(query, true)) -20 else 0
        (distance.second + bonus) to item
    }.filter { it.first <= 0 }.sortedBy { it.first }


    // taken from https://gist.github.com/jmarchesini/e330088e03daa394cf03ddedb8956fbe
    private fun wagnerFischer(s: String, t: String): Int {
        val m = s.length
        val n = t.length

        if (s == t) return 0
        if (s.isEmpty()) return n
        if (t.isEmpty()) return m

        val d = Array(m + 1) { IntArray(n + 1) { 0 } }

        (1..m).forEach { i ->
            d[i][0] = i
        }

        (1..n).forEach { j ->
            d[0][j] = j
        }

        (1..n).forEach { j ->
            (1..m).forEach { i ->
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                val delCost = d[i - 1][j] + 1
                val addCost = d[i][j - 1] + 1
                val subCost = d[i - 1][j - 1] + cost

                d[i][j] = minOf(delCost, addCost, subCost)
            }
        }

        return d[m][n]
    }

    fun String?.splitArtists() = this?.split(",", "&", " and ")
        ?.mapNotNull { it.trim().takeIf { s -> s.isNotBlank() } }
        ?: listOf(null)

    fun String.id() = this.lowercase().hashCode().toLong()
}