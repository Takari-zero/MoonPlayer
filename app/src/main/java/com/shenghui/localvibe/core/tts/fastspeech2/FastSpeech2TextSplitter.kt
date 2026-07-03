package com.shenghui.localvibe.core.tts.fastspeech2

object FastSpeech2TextSplitter {
    private const val DEFAULT_MAX_CLAUSE_CHARS = 24
    private val delimiters = setOf(
        '\n', '\r',
        '，', '。', '！', '？', '；', '：', '、',
        ',', '.', '!', '?', ';', ':'
    )

    fun split(text: String, maxClauseChars: Int = DEFAULT_MAX_CLAUSE_CHARS): List<String> {
        val limit = maxClauseChars.coerceAtLeast(1)
        val clauses = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            val raw = current.toString().trim()
            current.clear()
            if (raw.isEmpty()) return
            var start = 0
            while (start < raw.length) {
                val end = (start + limit).coerceAtMost(raw.length)
                raw.substring(start, end).trim().takeIf { it.isNotEmpty() }?.let(clauses::add)
                start = end
            }
        }

        text.forEach { char ->
            if (char in delimiters) {
                current.append(char)
                flush()
            } else {
                current.append(char)
                if (current.length >= limit) flush()
            }
        }
        flush()
        return clauses
    }
}

