package com.cafeminsu.data.remote

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionTokens
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.model.UserRole
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/user/kakao-login")
    suspend fun kakaoLogin(
        @Body request: KakaoLoginReq,
    ): BaseResponse<KakaoLoginRes>

    @POST("api/user/refresh")
    suspend fun refresh(
        @Header("Refresh-Token") refreshToken: String,
    ): BaseResponse<RefreshRes>

    @GET("api/user/profile")
    suspend fun getMyProfile(): BaseResponse<UserProfileRes>
}

@JsonClass(generateAdapter = true)
data class BaseResponse<T>(
    val isSuccess: Boolean?,
    val code: Int?,
    val message: String?,
    val result: T?,
)

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

data class LoginExchange(
    val tokens: SessionTokens,
    val authState: AuthState.Authenticated,
)

fun <T, R> BaseResponse<T>.unwrap(
    mapper: (T) -> AppResult<R>,
): AppResult<R> {
    if (isSuccess != true) {
        return AppResult.Failure(code.toDomainErrorOrUnknown())
    }

    val body = result ?: return AppResult.Failure(DomainError.Unknown)
    return mapper(body)
}

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
    )

private fun Int?.toDomainErrorOrUnknown(): DomainError =
    this?.toDomainError() ?: DomainError.Unknown

private fun String?.toDisplayName(): String =
    this
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "카페민수 사용자"

private const val DefaultServerUserId = "server-user"
