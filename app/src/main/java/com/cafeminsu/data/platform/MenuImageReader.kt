package com.cafeminsu.data.platform

/**
 * 점주가 고른 로컬 이미지(content:// 또는 file:// URI)를 업로드용 바이트로 읽어 온다.
 * 프레임워크에 의존하지 않는 데이터 계약이며, 실제 구현은 [AndroidMenuImageReader] 가 담당한다.
 */
interface MenuImageReader {
    /**
     * [localUri] 를 읽어 [MenuImageData] 로 반환한다.
     * 권한 없음·열기 실패·지원하지 않는 형식 등으로 읽을 수 없으면 예외 대신 null 을 반환한다(크래시 금지).
     */
    suspend fun read(localUri: String): MenuImageData?
}

/** 업로드할 이미지의 원본 바이트와 메타데이터. */
data class MenuImageData(
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String,
) {
    // ByteArray 를 가지므로 equals/hashCode 를 콘텐츠 기준으로 재정의한다.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MenuImageData) return false
        return bytes.contentEquals(other.bytes) &&
            mimeType == other.mimeType &&
            fileName == other.fileName
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}
