package com.cafeminsu.domain.model

/**
 * 점주가 운영하는 매장. 서버 stores/my 의 {id, name} 에서 매핑하며, id 는 도메인 전반의
 * 식별자 계약(문자열)에 맞춰 String 으로 보관한다(서버 Long → String 변환).
 */
data class OwnerStore(
    val id: String,
    val name: String,
)
