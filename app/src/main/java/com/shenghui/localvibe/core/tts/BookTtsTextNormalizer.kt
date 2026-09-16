package com.shenghui.localvibe.core.tts

data class BookTtsText(
    val originalText: String,
    val spokenText: String
)

object BookTtsTextNormalizer {
    fun normalize(text: String): BookTtsText {
        val cleaned = collapseRepeatedPunctuation(
            collapseWhitespace(
                removeInvisibleControls(text)
            )
        )
        return BookTtsText(
            originalText = text,
            spokenText = cleaned
        )
    }

    private fun removeInvisibleControls(text: String): String {
        val builder = StringBuilder(text.length)
        text.forEach { char ->
            when {
                char in removableInvisibleChars -> Unit
                char.isISOControl() && !char.isWhitespace() -> Unit
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    private fun collapseWhitespace(text: String): String {
        val builder = StringBuilder(text.length)
        var lastWasSpace = false
        text.forEach { char ->
            if (char.isWhitespace() || char in nonBreakingSpaces) {
                if (!lastWasSpace) {
                    builder.append(' ')
                    lastWasSpace = true
                }
            } else {
                builder.append(char)
                lastWasSpace = false
            }
        }
        return builder.toString().trim()
    }

    private fun collapseRepeatedPunctuation(text: String): String {
        return repeatedPunctuationRegex.replace(text) { match ->
            match.groupValues[1].repeat(MAX_REPEATED_PUNCTUATION)
        }
    }

    private val removableInvisibleChars = setOf(
        '\uFEFF',
        '\u200B',
        '\u200C',
        '\u200D',
        '\u2060'
    )

    private val nonBreakingSpaces = setOf(
        '\u00A0',
        '\u1680',
        '\u2000',
        '\u2001',
        '\u2002',
        '\u2003',
        '\u2004',
        '\u2005',
        '\u2006',
        '\u2007',
        '\u2008',
        '\u2009',
        '\u200A',
        '\u202F',
        '\u205F',
        '\u3000'
    )

    private const val MAX_REPEATED_PUNCTUATION = 2
    private val repeatedPunctuationRegex = Regex("([。！？!?，,；;：:、])\\1{2,}")
}
