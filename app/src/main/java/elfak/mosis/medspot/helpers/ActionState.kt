package elfak.mosis.medspot.helpers

sealed class ActionState {
    object Idle: ActionState()
    object Success: ActionState()
    class ActionError(val message: String? = null): ActionState()
}