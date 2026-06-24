package com.cafeminsu.core.model.auth

data class UserProfile(
    val id: String,
    val displayName: String,
    val phoneLast4: PhoneLast4,
)
