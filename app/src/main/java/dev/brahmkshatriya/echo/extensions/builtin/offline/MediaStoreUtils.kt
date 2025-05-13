package dev.brahmkshatriya.echo.extensions.builtin.offline

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Date.Companion.toDate
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toUriImageHolder
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.EXTENSION_ID
import java.io.File

/**
 * Blatantly kanged from [Gramophone](https://github.com/AkaneTan/Gramophone/blob/beta/app/src/main/kotlin/org/akanework/gramophone/logic/utils/MediaStoreUtils.kt)
 */
object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"

    interface Item {
        val id: Long?
        val title: String?
        val songList: MutableSet<Track>
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
        override val songList: MutableSet<Track>
    }

    data class AlbumImpl(
        override val id: Long?,
        override val title: String?,
        override val artists: List<MArtist?>,
        override val albumYear: Int?,
        override var cover: Uri?,
        override val songList: MutableSet<Track>
    ) : MAlbum

    /**
     * [MArtist] stores Artist metadata.
     */
    data class MArtist(
        override val id: Long?,
        override val title: String?,
        override val songList: MutableSet<Track>,
        val albumList: MutableList<MAlbum>,
    ) : Item

    /**
     * [Genre] stores Genre metadata.
     */
    data class Genre(
        override val id: Long?,
        override val title: String?,
        override val songList: MutableSet<Track>,
    ) : Item

    /**
     * [Date] stores Date metadata.
     */
    data class Date(
        override val id: Long,
        override val title: String?,
        override val songList: MutableSet<Track>,
    ) : Item

    /**
     * [MPlaylist] stores playlist information.
     */
    open class MPlaylist(
        override val id: Long,
        override val title: String?,
        override val songList: MutableSet<Track>,
        val description: String?,
        val modifiedDate: Long
    ) : Item


    /**
     * [LibraryStoreClass] collects above metadata classes
     * together for more convenient reading/writing.
     */
    @Suppress("unused")
    class LibraryStoreClass(
        val songList: MutableList<Track>,
        val albumList: MutableList<MAlbum>,
        val artistMap: MutableMap<Long?, MArtist>,
        val genreList: MutableList<Genre>,
        val dateList: MutableList<Date>,
        val playlistList: MutableList<MPlaylist>,
        val likedPlaylist: MPlaylist?,
        val folderStructure: FileNode,
        val shallowFolder: FileNode,
        val folders: Set<String>
    )

    class FileNode(
        val folderName: String
    ) {
        val folderList = hashMapOf<String, FileNode>()
        val songList = mutableListOf<Track>()
        var albumId: Long? = null
            private set

        fun addSong(item: Track, id: Long?) {
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

    private fun handleShallowTrack(
        mediaItem: Track,
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

    private val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0" +
            listOf(
                "audio/x-wav",
                "audio/ogg",
                "audio/aac",
                "audio/midi"
            ).joinToString("") { " or ${MediaStore.Audio.Media.MIME_TYPE} = '$it'" }

    private val projection = arrayListOf(
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
            add(MediaStore.Audio.Media.DATE_TAKEN)
            add(MediaStore.Audio.Media.DISC_NUMBER)
        }
    }.toTypedArray()

    private fun playlistContent(
        context: Context,
        playlists: MutableList<Pair<MPlaylist, MutableList<Long>>>
    ): Boolean {
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
        return foundPlaylistContent
    }

    private fun Cursor.parseSongQuery(
        limitValue: Int,
        folderFilter: Set<String>,
        blacklistKeywords: List<String>,
        context: Context,
        songs: MutableList<Track>,
        foundPlaylistContent: Boolean,
        idMap: MutableMap<Long, Track>?,
        likedAudios: List<Long>,
        block: (Track) -> Unit
    ) = use { cursor ->
        // Get columns from mediaStore.
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumArtistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
        val trackNumberColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val genreColumn = if (hasImprovedMediaStore())
            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE) else null
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val addDateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

        while (cursor.moveToNext()) {
            val path = cursor.getStringOrNull(pathColumn) ?: continue
            val duration = cursor.getLongOrNull(durationColumn)
            val fldPath = File(path).parentFile?.absolutePath
            val skip = (duration != null && duration < limitValue * 1000)
                    || folderFilter.contains(fldPath)
                    || blacklistKeywords.any { fldPath?.contains(it, true) == true }
            // We need to add blacklisted songs to idMap as they can be referenced by playlist
            if (skip && !foundPlaylistContent) continue
            val id = cursor.getLongOrNull(idColumn)!!
            val title = cursor.getStringOrNull(titleColumn)!!
            val artist = cursor.getStringOrNull(artistColumn)
                .let { v -> if (v == "<unknown>") null else v }
            val albumName = cursor.getStringOrNull(albumColumn)
            val albumArtist = cursor.getStringOrNull(albumArtistColumn)
            val trackNumber = cursor.getIntOrNull(trackNumberColumn)
            val year = cursor.getIntOrNull(yearColumn).let { v -> if (v == 0) null else v }
            val albumId = cursor.getLongOrNull(albumIdColumn)
            val genre = genreColumn?.let { col -> cursor.getStringOrNull(col) }
            val addDate = cursor.getLongOrNull(addDateColumn)

            // Since we're using glide, we can get album cover with a uri.
            val imgUri = ContentUris.appendId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon(), id
            ).appendPath("albumart").build()

            val artists = artist.toArtists()
            val album = albumName?.let {
                Album(albumId.toString(), it, null, albumArtist.toArtists())
            }
            val liked = likedAudios.contains(id)
            val song = Track(
                id = id.toString(),
                title = title,
                artists = artists,
                album = album,
                cover = imgUri.toString().toUriImageHolder(),
                duration = duration,
                releaseDate = year?.toDate(),
                isLiked = liked,
                streamables = listOf(Streamable.server(path, 0, path)),
                extras = mapOf(
                    "genre" to (genre ?: context.getString(R.string.unknown)),
                    "addDate" to addDate.toString(),
                    "trackNumber" to trackNumber.toString(),
                    EXTENSION_ID to OfflineExtension.metadata.id
                )
            )

            // Build our metadata maps/lists.
            idMap?.put(id, song)
            // Now that the song can be found by playlists, do NOT register other metadata.
            if (skip) continue
            songs.add(song)
            block(song)
        }
    }

    private val coverUri = "content://media/external/audio/albumart".toUri()

    private fun songAlbumMap(
        song: Track,
        year: Int?,
        albumMap: MutableMap<Long?, AlbumImpl>,
        artistMap: MutableMap<Long?, MArtist>,
        haveImgPerm: Boolean,
    ) {
        val album = song.album ?: return
        val id = album.id.toLong()
        albumMap.getOrPut(id) {
            val cover = if (haveImgPerm) null else ContentUris.withAppendedId(coverUri, id)
            val artists = album.artists.map {
                MArtist(it.id.toLongOrNull(), it.name, mutableSetOf(), mutableListOf())
            }

            AlbumImpl(id, album.title, artists, year, cover, mutableSetOf()).apply {
                artists.forEach {
                    val mArtist = artistMap.getOrPut(it.id) {
                        MArtist(it.id, it.title, mutableSetOf(), mutableListOf())
                    }
                    mArtist.albumList.add(this)
                }
            }
        }.songList.add(song)
    }

    private val allowedCoverExtensions = listOf("jpg", "png", "jpeg", "bmp", "tiff", "tif", "webp")

    private fun albumMapToAlbumList(
        albumMap: MutableMap<Long?, AlbumImpl>,
        coverCache: HashMap<Long, Pair<File, FileNode>>?
    ) = albumMap.values.onEach {
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

    /**
     * [getAllSongs] gets all of your songs from your local disk.
     *
     * @param context
     * @return
     */
    fun getAllSongs(context: Context, settings: Settings): LibraryStoreClass {
        val limitValueSeconds = settings.getInt("limit_value") ?: 10
        val haveImgPerm = if (hasScopedStorage()) context.hasImagePermission() else false
        val folderFilter = settings.getStringSet("blacklist_folders") ?: setOf()
        val blacklistKeywords = settings.getString("blacklist_keywords")?.split(',')
            ?.mapNotNull { it.trim().takeIf { s -> s.isNotBlank() } }.orEmpty()
        // Initialize list and maps.
        val coverCache = if (haveImgPerm) hashMapOf<Long, Pair<File, FileNode>>() else null
        val folders = hashSetOf<String>()
        val folderArray = mutableListOf<String>()
        val root = FileNode("storage")
        val shallowRoot = FileNode("shallow")
        val songs = mutableListOf<Track>()
        val albumMap = mutableMapOf<Long?, AlbumImpl>()
        val artistMap = mutableMapOf<Long?, MArtist>()
        val albumArtistMap =
            hashMapOf<MArtist?, Pair<MutableSet<MAlbum>, MutableSet<Track>>>()

        val genreMap = hashMapOf<String?, Genre>()
        val dateMap = hashMapOf<Int?, Date>()
        val playlists = mutableListOf<Pair<MPlaylist, MutableList<Long>>>()
        val foundPlaylistContent =
            runCatching { playlistContent(context, playlists) }.getOrNull() ?: false
        val idMap = hashMapOf<Long, Track>()
        val likedAudios = playlists.find { it.first.title == "Liked" }?.second ?: emptyList()
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor = context.contentResolver.query(
            uri, projection, selection, null,
            MediaStore.Audio.Media.TITLE + " COLLATE UNICODE ASC",
        )
        cursor?.parseSongQuery(
            limitValueSeconds,
            folderFilter,
            blacklistKeywords,
            context,
            songs,
            foundPlaylistContent,
            idMap,
            likedAudios
        ) { song ->
            song.artists.map {
                val id = it.id.toLongOrNull()
                val mArtist = artistMap.getOrPut(id) {
                    MArtist(id, it.name, mutableSetOf(), mutableListOf())
                }
                mArtist.songList.add(song)
                mArtist
            }

            val year = song.releaseDate?.year
            songAlbumMap(song, year, albumMap, artistMap, haveImgPerm)

            val albumId = song.album?.id?.toLong()
            val path = song.streamables.first().id
            val parent = File(path).parentFile
            parent?.absolutePath?.let { folders.add(it) }

            val fn = handleMediaFolder(path, root)
            fn.addSong(song, song.album?.id?.toLong())
            if (albumId != null && parent != null) {
                coverCache?.putIfAbsent(albumId, Pair(parent, fn))
            }
            handleShallowTrack(song, albumId, path, shallowRoot, folderArray)

            val genre = song.extras["genre"] ?: "Unknown"
            genreMap.getOrPut(genre) {
                Genre(genre.id(), genre, mutableSetOf())
            }.songList.add(song)

            dateMap.getOrPut(year) {
                Date(year?.toLong() ?: 0, year?.toString(), mutableSetOf())
            }.songList.add(song)
        }

        // Parse all the lists.
        val albumList = albumMapToAlbumList(albumMap, coverCache)
        albumArtistMap.entries.forEach { (artist, pair) ->
            val albums = pair.first
            val song = pair.second
            MArtist(artist?.id, artist?.title, song, albums.toMutableList())
        }
        val genreList = genreMap.values.toMutableList()
        val dateList = dateMap.values.toMutableList()

        val playlistsFinal = playlists.map {
            it.first.also { playlist ->
                playlist.songList.addAll(it.second.map { value ->
                    idMap[value] ?: dummyTrack(value, "Unknown [$value]")
                })
            }
        }.toMutableList()

        val likedPlaylist = getLikedPlaylist(context, playlistsFinal)
        likedPlaylist?.let { playlistsFinal.add(0, it) }

        folders.addAll(folderFilter)
        return LibraryStoreClass(
            songs, albumList, artistMap, genreList, dateList, playlistsFinal,
            likedPlaylist, root, shallowRoot, folders
        )
    }

    private fun getLikedPlaylist(
        context: Context,
        playlistsFinal: MutableList<MPlaylist>
    ) = run {
        val liked = playlistsFinal.find { it.title == "Liked" }
        val id = liked?.id ?: context.createPlaylist("Liked")
        liked?.let { playlistsFinal.remove(it) }
        MPlaylist(
            id ?: return@run null,
            context.getString(R.string.playlist_liked),
            liked?.songList ?: mutableSetOf(),
            context.getString(R.string.playlist_liked_desc),
            liked?.modifiedDate ?: (System.currentTimeMillis() / 1000L)
        )
    }

    private fun dummyTrack(id: Long, title: String): Track {
        return Track(id.toString(), title)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun Context.hasImagePermission() =
        checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED

    private fun hasImprovedMediaStore(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private fun hasScopedStorage(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun Context.createPlaylist(title: String): Long? {
        val values = ContentValues()
        values.put(
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME,
            title
        )
        return contentResolver.insert(
            @Suppress("DEPRECATION")
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            values
        )?.lastPathSegment?.toLong()
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
        contentResolver.delete(
            @Suppress("DEPRECATION")
            MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.PLAY_ORDER + "=?",
            arrayOf((index + 1).toString())
        )
    }

    fun Context.moveSongInPlaylist(playlistId: Long, song: Long, from: Int, to: Int) {
        removeSongFromPlaylist(playlistId, from)
        addSongToPlaylist(playlistId, song, to)
    }

    fun <E> List<E>.searchBy(query: String, block: (E) -> List<String?>) = map { item ->
        val qLower = query.lowercase().split(" ").ifEmpty { return@map 5 to item }
        val titles = block(item).flatMap {
            it?.split(" ") ?: listOf()
        }.mapNotNull {
            it.takeIf { it.isNotBlank() }?.lowercase()
        }.ifEmpty { return@map 5 to item }
        val selected = titles.map { t ->
            val distance = qLower.sumOf {
                val distance = wagnerFischer(t, it)
                val bonus = if (t.contains(it)) -t.length else 0
                distance + bonus
            } / qLower.size
            distance to t
        }.minBy { it.first }
        selected.first to item
    }.filter { it.first <= 3 }.sortedBy { it.first }

    // taken from https://gist.github.com/jmarchesini/e330088e03daa394cf03ddedb8956fbe
    private fun wagnerFischer(s: String, t: String): Int {
        val m = s.length
        val n = t.length

        if (s == t) return 0
        if (s.isEmpty()) return n
        if (t.isEmpty()) return m

        val d = Array(m + 1) { IntArray(n + 1) { 0 } }

        for (i in 1..m) {
            d[i][0] = i
        }

        for (j in 1..n) {
            d[0][j] = j
        }

        for (j in 1..n) {
            for (i in 1..m) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                val delCost = d[i - 1][j] + 1
                val addCost = d[i][j - 1] + 1
                val subCost = d[i - 1][j - 1] + cost

                d[i][j] = minOf(delCost, addCost, subCost)
            }
        }
        return d[m][n]
    }

    private fun String?.splitArtists() = this?.split(",", "&", " and ")
        ?.mapNotNull { it.trim().takeIf { s -> s.isNotBlank() } }
        ?: listOf(null)

    private fun String?.toArtists() = takeIf { this != "null" }.splitArtists().map {
        Artist(it?.id().toString(), it ?: "Unknown")
    }

    fun String.id() = this.lowercase().hashCode().toLong()
}