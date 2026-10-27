package kr.jinsil.panbyeolgi.rules

// public/js/rules.js의 정규식을 글자 그대로 옮겨 쓰기 위한 변환기.
// 자바(JVM 단위 테스트)와 안드로이드(ICU)는 \s \d \b . $ 의 뜻이 자바스크립트와 조금씩 달라서,
// 자바스크립트와 똑같이 동작하도록 명시적인 문자 묶음으로 바꾼다.

// 자바스크립트 \s 에 해당하는 공백 문자들 (줄바꿈, 줄 바꿈 없는 공백, 전각 공백 등)
private const val JS_SPACE_CHARS = "\\t\\n\\x0B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF"
private const val JS_WORD = "[A-Za-z0-9_]"
private const val JS_WORD_BOUNDARY = "(?:(?<=$JS_WORD)(?!$JS_WORD)|(?<!$JS_WORD)(?=$JS_WORD))"
// s 플래그가 없을 때 자바스크립트의 . 은 줄바꿈 문자만 빼고 모두 맞춘다.
private const val JS_DOT = "[^\\n\\r\\u2028\\u2029]"

/** 자바스크립트 정규식 [source]와 플래그(i, s)를 같은 뜻의 Kotlin [Regex]로 만든다. */
fun jsRegex(source: String, flags: String = ""): Regex {
    val dotAll = 's' in flags
    val out = StringBuilder()
    var inClass = false
    var i = 0
    while (i < source.length) {
        val c = source[i]
        if (c == '\\' && i + 1 < source.length) {
            val next = source[i + 1]
            out.append(
                when {
                    next == 's' -> if (inClass) JS_SPACE_CHARS else "[$JS_SPACE_CHARS]"
                    next == 'd' -> if (inClass) "0-9" else "[0-9]"
                    next == 'D' && !inClass -> "[^0-9]"
                    next == 'b' && !inClass -> JS_WORD_BOUNDARY
                    else -> "\\$next"
                },
            )
            i += 2
            continue
        }
        when {
            inClass && c == ']' -> {
                inClass = false
                out.append(c)
            }
            inClass && c == '[' -> out.append("\\[")
            !inClass && c == '[' -> {
                inClass = true
                out.append(c)
            }
            !inClass && c == '.' -> out.append(if (dotAll) "." else JS_DOT)
            !inClass && c == '$' -> out.append("\\z")
            else -> out.append(c)
        }
        i++
    }
    val options = buildSet {
        if ('i' in flags) add(RegexOption.IGNORE_CASE)
        if (dotAll) add(RegexOption.DOT_MATCHES_ALL)
    }
    return Regex(out.toString(), options)
}
