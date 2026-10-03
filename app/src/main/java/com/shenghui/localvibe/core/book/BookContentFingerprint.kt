package com.shenghui.localvibe.core.book

data class BookContentFingerprint(
    val size: Long,
    val sha256Hex: String
) {
    init {
        require(size >= 0L)
        require(SHA256_HEX.matches(sha256Hex))
    }

    private companion object {
        val SHA256_HEX = Regex("^[0-9a-f]{64}$")
    }
}

data class TxtBookReadResult(
    val paragraphs: List<String>,
    val fingerprint: BookContentFingerprint
)
