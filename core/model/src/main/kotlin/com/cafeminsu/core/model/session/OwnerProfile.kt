package com.cafeminsu.core.model.session

data class OwnerProfile(
    val id: String,
    val storeId: String,
    val storeName: String,
    val loginId: String,
    val isStoreOpen: Boolean,
)
