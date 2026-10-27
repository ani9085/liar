package kr.jinsil.panbyeolgi.service

/** 모르는 번호의 종류. 070·해외 번호는 경고 문구를 강하게 한다. 신고된 사기 번호 목록은 아직 없다. */
enum class CallRisk(val title: String, val lead: String) {
    FOREIGN("해외에서 온 전화예요", "모르는 해외 번호는 받지 않는 게 안전해요."),
    INTERNET_PHONE("사기 의심 전화예요", "070 인터넷 전화는 사기에 자주 쓰여요."),
    HIDDEN("번호를 숨긴 전화예요", "번호를 숨기고 거는 전화는 조심하세요."),
    UNKNOWN("모르는 번호예요", "");

    val strong: Boolean get() = this == FOREIGN || this == INTERNET_PHONE

    companion object {
        fun classify(number: String?): CallRisk {
            val raw = number.orEmpty().filter { it.isDigit() || it == '+' }
            if (raw.isEmpty()) return HIDDEN
            if (raw.startsWith("+")) {
                return if (raw.startsWith("+82")) domestic("0" + raw.removePrefix("+82")) else FOREIGN
            }
            // 001, 002, 005, 006, 008, 00700 같은 국제전화 식별번호
            if (raw.length > 3 && raw.startsWith("00") && raw[2] in '1'..'9') return FOREIGN
            return domestic(raw)
        }

        private fun domestic(number: String): CallRisk = if (number.startsWith("070")) INTERNET_PHONE else UNKNOWN
    }
}
