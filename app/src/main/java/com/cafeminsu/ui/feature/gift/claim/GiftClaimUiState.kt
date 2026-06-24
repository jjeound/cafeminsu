package com.cafeminsu.ui.feature.gift.claim

/**
 * 선물 등록(claim) 화면 상태. 코드 입력 폼 + 처리중(submitting) + 에러 헬퍼.
 * 성공 시점은 일회성 [GiftClaimEvent.Claimed] 로 화면에 전달한다(내 기프티콘 이동).
 */
data class GiftClaimUiState(
    val code: String = "",
    val submitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSubmit: Boolean = code.isNotBlank() && !submitting
}

sealed interface GiftClaimEvent {
    /** 등록 성공 — 안내 후 내 기프티콘으로 이동. claimCode/민감값은 싣지 않는다. */
    data class Claimed(val message: String) : GiftClaimEvent
}
