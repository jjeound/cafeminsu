package com.ssafy.cafeminsu.core.model.sales

sealed interface PayoutStatus {
    data object Pending : PayoutStatus

    data class Scheduled(val dateLabel: String) : PayoutStatus
}
