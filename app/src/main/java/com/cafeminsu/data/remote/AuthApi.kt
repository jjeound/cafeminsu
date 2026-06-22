package com.cafeminsu.data.remote

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionTokens
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.model.UserRole
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @POST("api/user/kakao-login")
    suspend fun kakaoLogin(
        @Body request: KakaoLoginReq,
    ): KakaoLoginRes

    @POST("api/user/owner-login")
    suspend fun ownerLogin(
        @Body request: OwnerLoginReq,
    ): OwnerLoginRes

    @POST("api/user/refresh")
    suspend fun refresh(
        @Header("Refresh-Token") refreshToken: String,
    ): RefreshRes

    @GET("api/user/profile")
    suspend fun getMyProfile(): UserProfileRes

    @GET("api/user/nickname/check")
    suspend fun checkNickname(
        @Query("nickname") nickname: String,
    ): NicknameCheckRes

    @POST("api/user/signup")
    suspend fun signup(
        @Body request: SignupReq,
    ): SignupRes
}

@JsonClass(generateAdapter = true)
data class KakaoLoginReq(
    val accessToken: String,
)

@JsonClass(generateAdapter = true)
data class KakaoLoginRes(
    val accessToken: String?,
    val refreshToken: String?,
    val isNewUser: Boolean?,
    val nickname: String?,
)

@JsonClass(generateAdapter = true)
data class OwnerLoginReq(
    val loginId: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class OwnerLoginRes(
    val accessToken: String?,
    val refreshToken: String?,
    val nickname: String?,
)

@JsonClass(generateAdapter = true)
data class RefreshRes(
    val accessToken: String?,
)

@JsonClass(generateAdapter = true)
data class UserProfileRes(
    val id: Long?,
    val nickname: String?,
    val profileImageUrl: String?,
    val role: String?,
)

@JsonClass(generateAdapter = true)
data class NicknameCheckRes(
    val available: Boolean?,
)

@JsonClass(generateAdapter = true)
data class SignupReq(
    val nickname: String,
    val profileImageUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class SignupRes(
    val userId: Long?,
    val nickname: String?,
)

data class LoginExchange(
    val tokens: SessionTokens,
    val authState: AuthState.Authenticated,
)

data class OwnerLoginExchange(
    val tokens: SessionTokens,
    val ownerProfile: OwnerProfile,
)

fun KakaoLoginRes.toLoginExchange(): AppResult<LoginExchange> {
    val accessToken = accessToken?.takeIf { it.isNotBlank() }
        ?: return AppResult.Failure(DomainError.Unknown)
    val refreshToken = refreshToken?.takeIf { it.isNotBlank() }
        ?: return AppResult.Failure(DomainError.Unknown)

    return AppResult.Success(
        LoginExchange(
            tokens = SessionTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            ),
            authState = AuthState.Authenticated(
                user = UserProfile(
                    id = DefaultServerUserId,
                    displayName = nickname.toDisplayName(),
                    phoneLast4 = null,
                ),
                role = UserRole.Customer,
                isNewUser = isNewUser == true,
            ),
        ),
    )
}

fun OwnerLoginRes.toOwnerLoginExchange(loginId: String): AppResult<OwnerLoginExchange> {
    val accessToken = accessToken?.takeIf { it.isNotBlank() }
        ?: return AppResult.Failure(DomainError.Unknown)
    val refreshToken = refreshToken?.takeIf { it.isNotBlank() }
        ?: return AppResult.Failure(DomainError.Unknown)
    val storeName = nickname?.trim()?.takeIf { it.isNotEmpty() } ?: loginId

    return AppResult.Success(
        OwnerLoginExchange(
            tokens = SessionTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            ),
            ownerProfile = OwnerProfile(
                id = loginId,
                storeId = loginId,
                storeName = storeName,
                loginId = loginId,
                isStoreOpen = true,
            ),
        ),
    )
}

fun RefreshRes.toAccessToken(): AppResult<String> {
    val token = accessToken?.takeIf { it.isNotBlank() }
        ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(token)
}

fun UserProfileRes.toAuthenticatedState(): AuthState.Authenticated =
    AuthState.Authenticated(
        user = UserProfile(
            id = id?.toString() ?: DefaultServerUserId,
            displayName = nickname.toDisplayName(),
            phoneLast4 = null,
        ),
        role = when (role) {
            "OWNER" -> UserRole.Owner
            else -> UserRole.Customer
        },
        isNewUser = false,
    )

fun NicknameCheckRes.toAvailability(): AppResult<Boolean> {
    val value = available ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(value)
}

fun SignupRes.toAuthenticatedState(): AuthState.Authenticated =
    AuthState.Authenticated(
        user = UserProfile(
            id = userId?.toString() ?: DefaultServerUserId,
            displayName = nickname.toDisplayName(),
            phoneLast4 = null,
        ),
        role = UserRole.Customer,
        isNewUser = false,
    )

private fun String?.toDisplayName(): String =
    this
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "카페민수 사용자"

private const val DefaultServerUserId = "server-user"
