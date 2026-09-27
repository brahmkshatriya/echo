package dev.brahmkshatriya.echo.app.platform

private object WasmPlatform : Platform {
    override val name: String = "WebAssembly"
}

actual fun getPlatform(): Platform = WasmPlatform
