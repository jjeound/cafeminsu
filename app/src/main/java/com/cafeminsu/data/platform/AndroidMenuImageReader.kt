package com.cafeminsu.data.platform

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ContentResolver] 를 통해 로컬 이미지 URI 를 바이트로 읽는다.
 * 열기 실패·권한 없음·미지원 형식 등에서는 예외를 전파하지 않고 null 을 반환한다(크래시 금지).
 */
@Singleton
class AndroidMenuImageReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : MenuImageReader {
    override suspend fun read(localUri: String): MenuImageData? =
        runCatching {
            val uri = Uri.parse(localUri)
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.isEmpty()) return null
            val mimeType = resolver.getType(uri) ?: DefaultMimeType
            MenuImageData(
                bytes = bytes,
                mimeType = mimeType,
                fileName = fileNameFor(mimeType),
            )
        }.getOrNull()

    private fun fileNameFor(mimeType: String): String {
        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        return "menu_image.$extension"
    }

    private companion object {
        const val DefaultMimeType = "image/jpeg"
    }
}
