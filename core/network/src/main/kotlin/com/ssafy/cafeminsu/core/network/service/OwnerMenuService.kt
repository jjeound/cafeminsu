package com.ssafy.cafeminsu.core.network.service

import com.ssafy.cafeminsu.core.network.model.request.menu.MenuAvailabilityRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuCreateRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuOptionRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuOptionUpdateRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuUpdateRequest
import com.ssafy.cafeminsu.core.network.model.response.menu.MenuCreateResponse
import com.ssafy.cafeminsu.core.network.model.response.menu.MenuOptionCreateResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST

interface OwnerMenuService {
    @POST("api/stores/{storeId}/menus")
    suspend fun createMenu(
        @Path("storeId") storeId: Long,
        @Body request: MenuCreateRequest,
    ): MenuCreateResponse

    @PATCH("api/menus/{menuId}")
    suspend fun updateMenu(
        @Path("menuId") menuId: Long,
        @Body request: MenuUpdateRequest,
    )

    @PATCH("api/menus/{menuId}/availability")
    suspend fun updateAvailability(
        @Path("menuId") menuId: Long,
        @Body request: MenuAvailabilityRequest,
    )

    @DELETE("api/menus/{menuId}")
    suspend fun deleteMenu(@Path("menuId") menuId: Long)

    @POST("api/menus/{menuId}/options")
    suspend fun addOption(
        @Path("menuId") menuId: Long,
        @Body request: MenuOptionRequest,
    ): MenuOptionCreateResponse

    @PATCH("api/menu-options/{optionId}")
    suspend fun updateOption(
        @Path("optionId") optionId: Long,
        @Body request: MenuOptionUpdateRequest,
    )

    @DELETE("api/menu-options/{optionId}")
    suspend fun deleteOption(@Path("optionId") optionId: Long)
}
