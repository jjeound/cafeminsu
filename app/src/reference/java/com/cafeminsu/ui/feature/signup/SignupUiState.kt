package com.cafeminsu.ui.feature.signup

data class SignupUiState(
    val nickname: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val charCount: Int get() = nickname.length

    val isNicknameValid: Boolean get() = NicknamePattern.matches(nickname)

    val canSubmit: Boolean get() = isNicknameValid && !isLoading

    companion object {
        const val MaxNicknameLength = 10
        val NicknamePattern = Regex("^[가-힣a-zA-Z0-9]{2,$MaxNicknameLength}$")
    }
}

sealed interface SignupEvent {
    data object NavigateHome : SignupEvent
    data class ShowSnackbar(val message: String) : SignupEvent
}
