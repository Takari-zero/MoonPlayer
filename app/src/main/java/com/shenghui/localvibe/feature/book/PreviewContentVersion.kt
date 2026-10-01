package com.shenghui.localvibe.feature.book

sealed interface PreviewContentVersion {
    val size: Long?

    @ConsistentCopyVisibility
    data class Metadata internal constructor(
        override val size: Long,
        val modifiedAt: Long
    ) : PreviewContentVersion {
        init {
            require(size >= 0L)
            require(modifiedAt > 0L)
        }
    }

    @ConsistentCopyVisibility
    data class Fingerprint internal constructor(
        override val size: Long,
        val sha256Hex: String
    ) : PreviewContentVersion {
        init {
            require(size >= 0L)
            require(SHA256_HEX.matches(sha256Hex))
        }
    }

    data object Unavailable : PreviewContentVersion {
        override val size: Long? = null
    }

    companion object {
        private val SHA256_HEX = Regex("^[0-9a-f]{64}$")

        fun metadata(size: Long?, modifiedAt: Long?): PreviewContentVersion {
            return if (size != null && size >= 0L && modifiedAt != null && modifiedAt > 0L) {
                Metadata(size, modifiedAt)
            } else {
                Unavailable
            }
        }

        fun fingerprint(size: Long?, sha256Hex: String?): PreviewContentVersion {
            val canonical = sha256Hex?.lowercase()
            return if (size != null && size >= 0L && canonical != null && SHA256_HEX.matches(canonical)) {
                Fingerprint(size, canonical)
            } else {
                Unavailable
            }
        }
    }
}
