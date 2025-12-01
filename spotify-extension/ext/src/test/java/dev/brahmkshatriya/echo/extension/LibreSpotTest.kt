package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.extension.spotify.SpotifyApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class, ExperimentalStdlibApi::class)
class LibreSpotTest {
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() = Dispatchers.setMain(mainThreadSurrogate)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    private val cookie = "sp_dc="
    private val spotifyApi = SpotifyApi(File(""))

    @Test
    fun testings() {
        runBlocking {
            val fileId = "6f842a801d0460c56c6fef4368f9ea9026c13db2"
            val key = "a2fb75db3889ee6af1549e1a5fb61674"

        }
    }
}