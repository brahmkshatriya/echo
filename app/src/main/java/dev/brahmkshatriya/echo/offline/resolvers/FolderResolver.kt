package dev.brahmkshatriya.echo.offline.resolvers

import android.content.Context
import android.provider.MediaStore
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.offline.PagedSource.Companion.pagedFlow
import java.io.File

class FolderResolver(private val context: Context, private val trackResolver: TrackResolver) {

    fun getRootFolders(
        list: List<MediaItemsContainer>,
        page: Int,
        pageSize: Int
    ): List<MediaItemsContainer.Container> {
        val folders = mutableListOf<MediaItemsContainer.Container>()
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val selection =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
        val sortOrder = MediaStore.Files.FileColumns.TITLE

        createCursor(
            context.contentResolver,
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            arrayOf(),
            sortOrder,
            true,
            pageSize,
            page * pageSize
        )?.use { cursor ->
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val paths =
                list.mapNotNull { (it as? MediaItemsContainer.Container)?.extra?.get("path") }
            while (cursor.moveToNext()) {
                val data = cursor.getString(dataIndex)
                val file = File(data)
                val parentFile = file.parentFile
                if (parentFile != null && parentFile.path !in paths) {
                    folders.add(
                        MediaItemsContainer.Container(
                            title = parentFile.name,
                            more = pagedFlow { page, pageSize ->
                                getFolderTracks(parentFile, page, pageSize)
                            },
                            extra = mapOf("path" to parentFile.path)
                        )
                    )
                }
            }
        }
        return folders
    }

    private fun getFolderTracks(folder: File, page: Int, pageSize: Int): List<MediaItemsContainer> = run {
            val list = mutableListOf<MediaItemsContainer>()

            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            val selection = MediaStore.Audio.Media.DATA + " like ? "
            val selectionArgs = arrayOf("%" + folder.path + "%")
            val sortOrder = MediaStore.Audio.Media.TITLE

            createCursor(
                context.contentResolver,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
                false,
                pageSize,
                page * pageSize
            )?.use { cursor ->
                val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (cursor.moveToNext()) {
                    val data = cursor.getString(dataIndex)
                    val file = File(data)
                    trackResolver.fromFile(file)?.toMediaItem()?.toMediaItemsContainer()?.let {
                        list.add(it)
                    }
                }
            }
            list
        }
}