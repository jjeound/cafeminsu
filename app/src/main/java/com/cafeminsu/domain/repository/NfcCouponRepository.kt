package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.NfcCoupon

interface NfcCouponRepository {
    /**
     * 매장 NFC 태그에서 읽은 [tagCode] 로 쿠폰 발급을 요청한다.
     *
     * 발급은 금전성 액션이므로 낙관적 처리 금지 — 서버 확정(성공 응답) 후에만 [NfcCoupon] 을 반환한다.
     */
    suspend fun claim(tagCode: String): AppResult<NfcCoupon>
}
