package com.cafeminsu.core.model.session

sealed interface PhoneLast4 {
    data object Unavailable : PhoneLast4

    data class Available(val value: String) : PhoneLast4
}
