package dev.brahmkshatriya.echo.app.ui.player

/** Fixed demo playlist. Album, artist and artwork URLs are taken from the
 * supplied Top 100 Album Cover Art JSON (Rolling Stone cover list).
 * Track titles are representative songs from each album. */
internal data class ExampleSong(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
)

internal data class ExampleQueueGroup(val title: String, val size: Int)

internal val exampleQueueGroups = listOf(
    ExampleQueueGroup("Alternative & Indie", 6),
    ExampleQueueGroup("Rock Classics", 6),
    ExampleQueueGroup("Hip-Hop & R&B", 6),
    ExampleQueueGroup("Funk, Pop & Soul", 6),
)

internal val exampleSongs = listOf(
    // Alternative & Indie
    ExampleSong("Disorder", "Joy Division", "Unknown Pleasures",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Joy-Division-Unknown-Pleasures.jpg?w=1000"),
    ExampleSong("Smells Like Teen Spirit", "Nirvana", "Nevermind",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Nirvana-Nevermind.jpg?w=1000"),
    ExampleSong("Sunday Morning", "The Velvet Underground", "The Velvet Underground & Nico",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Velvet-Undergound-Velvet-Underground-and-Nico.jpg?w=1000"),
    ExampleSong("Violet", "Hole", "Live Through This",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Hole-Live-Through-This.jpg?w=1000"),
    ExampleSong("Starman", "David Bowie", "The Rise and Fall of Ziggy Stardust and the Spiders from Mars",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/David-Bowie-Ziggy.jpg?w=1000"),
    ExampleSong("Once in a Lifetime", "Talking Heads", "Remain in Light",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Talking-Heads-Remain-In-Light.jpg?w=1000"),
    // Rock Classics
    ExampleSong("Come Together", "The Beatles", "Abbey Road",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Beatles-Abbey-Road.jpg?w=1000"),
    ExampleSong("Time", "Pink Floyd", "The Dark Side of the Moon",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Pink-Floyd-Dark-Side-of-the-Moon.jpg?w=1000"),
    ExampleSong("London Calling", "The Clash", "London Calling",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/The-Clash-London-Calling.jpg?w=1000"),
    ExampleSong("Baba O’Riley", "The Who", "Who's Next",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/1971-Whos-Next.jpg?w=1000"),
    ExampleSong("Dreams", "Fleetwood Mac", "Rumours",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Fleetwood-Mac-Rumours.jpg?w=1000"),
    ExampleSong("Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/led-zeppelin-iv.jpg?w=1000"),
    // Hip-Hop & R&B
    ExampleSong("Juicy", "The Notorious B.I.G.", "Ready to Die",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Notorious-BIG-ready-to-die.jpg?w=1000"),
    ExampleSong("Alright", "Kendrick Lamar", "To Pimp a Butterfly",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Kendrick-Lamar-To-Pimp-a-Butterfly.jpg?w=1000"),
    ExampleSong("Formation", "Beyoncé", "Lemonade",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Beyonce-Lemonade.jpg?w=1000"),
    ExampleSong("N.Y. State of Mind", "Nas", "Illmatic",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Nas-Illmatic.jpg?w=1000"),
    ExampleSong("Ms. Jackson", "Outkast", "Stankonia",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Outkast-Stankonia.jpg?w=1000"),
    ExampleSong("Kill Bill", "SZA", "SOS",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/SZA-SOS.jpg?w=1000"),
    // Funk, Pop & Soul
    ExampleSong("Family Affair", "Sly and the Family Stone", "There's a Riot Goin' On",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Sly-and-the-Family-Stone-Theres-A-riot-Goin-On.jpg?w=1000"),
    ExampleSong("Maggot Brain", "Funkadelic", "Maggot Brain",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Funkadelic-Maggot-Brain-.jpg?w=1000"),
    ExampleSong("Girls Just Want to Have Fun", "Cyndi Lauper", "She's So Unusual",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Cyndi-Lauper-Shes-So-Unusual-.jpg?w=1000"),
    ExampleSong("It’s Too Late", "Carole King", "Tapestry",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Carole-King-Tapestry.jpg?w=1000"),
    ExampleSong("When You Were Mine", "Prince", "Dirty Mind",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Prince-Dirty-Mind.jpg?w=1000"),
    ExampleSong("Rio", "Duran Duran", "Rio",
        "https://www.rollingstone.com/wp-content/uploads/2024/06/Duran-Duran-Rio.jpg?w=1000"),
)

internal fun exampleSong(index: Int): ExampleSong? = exampleSongs.getOrNull(index)
