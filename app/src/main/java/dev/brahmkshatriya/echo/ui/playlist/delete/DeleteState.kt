package dev.brahmkshatriya.echo.ui.playlist.delete

sealed class DeleteState {
    data object Initial : DeleteState()
    data object Deleting : DeleteState()
    data class Deleted(val result: Result<Unit>) : DeleteState()
}