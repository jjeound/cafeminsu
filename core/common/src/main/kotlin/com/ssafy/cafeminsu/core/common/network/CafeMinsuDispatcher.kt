package com.ssafy.cafeminsu.core.common.network

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val cafeMinsuDispatcher: CafeMinsuDispatcher)

enum class CafeMinsuDispatcher {
    Default,
    IO,
}