package dev.brahmkshatriya.echo.download

sealed class TaskAction {
    sealed class All : TaskAction() {
        data object RemoveAll : All()
        data object PauseAll : All()
        data object ResumeAll : All()
    }

    data class Remove(val ids: List<Long>) : TaskAction()
    data class Pause(val ids: List<Long>) : TaskAction()
    data class Resume(val ids: List<Long>) : TaskAction()
}