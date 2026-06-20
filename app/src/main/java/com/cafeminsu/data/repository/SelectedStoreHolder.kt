package com.cafeminsu.data.repository

import com.cafeminsu.domain.model.Store
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class SelectedStoreHolder @Inject constructor() {
    private val selectedStore = MutableStateFlow<Store?>(null)

    fun observe(): StateFlow<Store?> = selectedStore

    fun current(): Store? = selectedStore.value

    fun select(store: Store) {
        selectedStore.value = store
    }
}
