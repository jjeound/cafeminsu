package com.cafeminsu.ui.feature.home

import androidx.lifecycle.ViewModel
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
@HiltViewModel
class HomeViewModel @Inject constructor(
    menuRepository: MenuRepository,
    sessionRepository: SessionRepository,
) : ViewModel()
