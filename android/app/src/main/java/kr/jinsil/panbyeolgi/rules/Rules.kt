package kr.jinsil.panbyeolgi.rules

import java.text.Normalizer
import java.util.Locale

// 규칙 기반 판별 엔진. public/js/rules.js를 그대로 옮긴 것으로, 같은 글에 같은 결과를 내야 한다.
// 규칙을 고칠 때는 rules.js를 먼저 고치고 tools/export-rules-fixtures.mjs로 비교용 예문을 다시 만든다.

enum class Level(val key: String) {
    SAFE("safe"),
    CAUTION("caution"),
    DANGER("danger");

    companion object {
        fun fromKey(key: String?): Level? = entries.firstOrNull { it.key == key }
    }
}

enum class Mode(val key: String) {
    SCAM("scam"),
    SHARE("share");

    companion object {
        fun fromKey(key: String?): Mode = if (key == SHARE.key) SHARE else SCAM
    }
}

enum class Category(val key: String) {
    SCAM("scam"),
    MISINFO("misinfo");

    val other: Category get() = if (this == SCAM) MISINFO else SCAM

    companion object {
        fun fromKey(key: String?): Category? = entries.firstOrNull { it.key == key }
    }
}

data class Signal(val id: String, val weight: Int, val reason: String)

data class Evaluation(val score: Int, val level: Level, val signals: List<Signal>)

data class Analysis(val links: List<String>, val scam: Evaluation, val misinfo: Evaluation) {
    operator fun get(category: Category): Evaluation = if (category == Category.SCAM) scam else misinfo
}

/** 서버 AI 판별 결과 (src/ai.js의 응답 형식). 레벨은 검증 전 원래 글자 그대로 받는다. */
data class AiResult(
    val scamLevel: String?,
    val misinfoLevel: String?,
    val headline: String?,
    val reasons: List<String>,
    val advice: String?,
    val factCheck: String?,
)

data class Verdict(
    val mode: Mode,
    val category: Category,
    val level: Level,
    val headline: String,
    val reasons: List<String>,
    val advice: String,
    val factCheck: String,
    val links: List<String>,
    val usedAI: Boolean,
)

private class Rule(val id: String, val weight: Int, val re: Regex, val reason: String)

object Rules {
    private val SHORTENERS = setOf(
        "bit.ly", "han.gl", "me2.kr", "url.kr", "vo.la", "t.ly", "tinyurl.com", "buly.kr",
        "c11.kr", "ko.gl", "goo.gl", "is.gd", "cutt.ly", "shorturl.at", "rb.gy", "t.co", "naver.me", "zrr.kr",
    )
    private val RISKY_TLDS = setOf("xyz", "top", "site", "shop", "click", "icu", "cyou", "buzz", "live", "vip", "cc")

    private val URL_RE = jsRegex(
        """(https?:\/\/[^\s<>"']+|www\.[^\s<>"']+|\b[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:kr|com|net|org|me|ly|gl|io|co|xyz|top|site|shop|click|icu|cyou|buzz|live|vip|cc|at|gd)(?![a-z0-9-])(?:\/[^\s<>"']*)?)""",
        "gi",
    )
    private val TRAILING_PUNCT = jsRegex("""[.,)\]]+$""")
    private val SCHEME = jsRegex("""^https?:\/\/""", "i")
    private val WWW = jsRegex("""^www\.""", "i")
    private val HOST_END = jsRegex("""[/?#]""")
    private val IP_HOST = jsRegex("""^\d{1,3}(\.\d{1,3}){3}(:\d+)?$""")

    // 사기(보이스피싱·스미싱·메신저 피싱) 신호
    private val SCAM_RULES = listOf(
        Rule(
            "family-impersonation", 35,
            jsRegex("""(엄마|아빠|어머니|아버지|할머니|할아버지).{0,30}(폰|핸드폰|휴대폰|액정).{0,15}(고장|깨|잃어|분실|수리|망가)""", "s"),
            "자녀나 가족인 척 \"폰이 고장 났다\"며 다른 번호로 연락하는 수법과 비슷해요.",
        ),
        Rule(
            "gift-card", 30,
            jsRegex("""(문화상품권|구글\s?기프트|기프트\s?카드|상품권|기프티콘).{0,25}(사|구매|구입|보내|결제|번호)""", "s"),
            "상품권을 사서 번호를 보내 달라는 요구는 대표적인 사기 수법이에요.",
        ),
        Rule(
            "urgent-money", 25,
            jsRegex("""(대신|급하게|급히|빨리|지금\s?당장).{0,15}(결제|송금|이체|입금|돈)""", "s"),
            "급하게 돈을 보내거나 대신 결제해 달라고 재촉하고 있어요.",
        ),
        Rule(
            "id-card-request", 35,
            jsRegex("""(신분증|주민등록증|주민번호|운전면허증).{0,20}(사진|찍어|보내|알려)""", "s"),
            "신분증이나 주민번호를 보내 달라고 해요. 절대 보내면 안 돼요.",
        ),
        Rule(
            "secret-info-request", 25,
            jsRegex("""(비밀번호|OTP|보안카드|인증번호|카드\s?번호|CVC|계좌\s?번호).{0,15}(알려|보내|입력|불러)""", "is"),
            "비밀번호·인증번호 같은 중요한 정보를 요구해요.",
        ),
        Rule(
            "authority-impersonation", 40,
            jsRegex("""(검찰|검사|수사관|금융감독원|금감원|경찰청|사이버\s?수사대|법원|국세청).{0,40}(계좌|자산|이체|연루|구속|피해|범죄|수사|명의)""", "s"),
            "검찰·금감원 같은 기관인 척하며 계좌나 범죄 이야기를 해요. 기관은 문자·전화로 돈을 요구하지 않아요.",
        ),
        Rule(
            "safe-account", 40,
            jsRegex("""(안전\s?계좌|국가\s?안전\s?계좌|보호\s?계좌)"""),
            "\"안전계좌\"로 돈을 옮기라는 것은 100% 사기예요.",
        ),
        Rule(
            "app-install", 35,
            jsRegex("""((앱|어플|프로그램).{0,10}(설치|깔아|다운)|\.apk\b|팀뷰어|애니데스크|anydesk|teamviewer|원격\s?(제어|지원|조종))""", "is"),
            "앱 설치나 원격 조종을 요구해요. 휴대폰 정보를 빼가려는 수법일 수 있어요.",
        ),
        Rule(
            "loan", 25,
            jsRegex("""((저금리|대환|정부\s?지원|서민).{0,15}(대출|자금)|대출.{0,10}(승인|가능|한도|진행))""", "s"),
            "저금리 대출을 권하는 문자는 사기인 경우가 많아요.",
        ),
        Rule(
            "delivery-lure", 15,
            jsRegex("""(택배|배송|우체국|대한통운|한진|로젠|롯데택배|CJ).{0,30}(주소|미확인|반송|보관|확인|불가|지연)""", "is"),
            "택배 주소 확인을 핑계로 링크를 누르게 하는 문자와 비슷해요.",
        ),
        Rule(
            "event-lure", 15,
            jsRegex("""(부고|부음|청첩장|모바일\s?초대장|돌잔치|모바일\s?초대)"""),
            "부고·청첩장을 핑계로 링크를 누르게 하는 문자일 수 있어요.",
        ),
        Rule(
            "payment-alert", 30,
            jsRegex("""((국제|해외)\s?발신.{0,40}(결제|승인)|해외.{0,6}결제.{0,15}(승인|완료)|(결제|승인).{0,20}(본인이?\s?아닐|본인\s?아니))""", "s"),
            "\"해외 결제가 됐다\"며 놀라게 해서 전화를 걸게 하는 수법과 비슷해요.",
        ),
        Rule(
            "not-me-call", 25,
            jsRegex("""본인이?\s?(아닐|아니).{0,20}(문의|연락|전화|신고)""", "s"),
            "\"본인이 아니면 연락하라\"는 문구로 전화를 유도해요.",
        ),
        Rule(
            "fine-refund-lure", 15,
            jsRegex("""(과태료|범칙금|환급금|미납|건강검진\s?결과|보험금\s?(지급|청구)|지원금\s?(신청|지급))"""),
            "과태료·환급금·지원금을 핑계로 링크를 누르게 하는 문자일 수 있어요.",
        ),
        Rule(
            "investment", 25,
            jsRegex("""(리딩방|수익\s?보장|원금\s?보장|고수익|급등주|코인\s?(투자|추천)|무조건\s?수익)"""),
            "수익을 보장한다는 투자 권유는 사기일 가능성이 높아요.",
        ),
        Rule(
            "pressure", 10,
            jsRegex("""(즉시|오늘\s?중|24시간\s?(이내|안에)|정지\s?(예정|됩니다)|압류|체포|구속)"""),
            "겁을 주거나 서두르게 만드는 표현이 있어요.",
        ),
        Rule(
            "foreign-number", 15,
            jsRegex("""(국제\s?(발신|전화)|\+(?!82)\d{1,3}[\s-]?\d{2,}|(^|\D)00[1-9]\d{0,2}[\s-]\d)"""),
            "해외에서 보낸 번호예요.",
        ),
    )

    // 허위정보(가짜뉴스·근거 없는 건강정보) 신호
    private val MISINFO_RULES = listOf(
        Rule(
            "share-pressure", 30,
            jsRegex("""(퍼뜨려|퍼트려|퍼날라|널리\s?알려|공유\s?(해|부탁)|단톡방에|지인들?(에게|한테)\s?(꼭\s?)?(알려|보내|공유)|많이\s?(보내|알려)|돌려\s?주세요)"""),
            "\"널리 퍼뜨려 달라\"고 재촉하는 글은 가짜 정보인 경우가 많아요.",
        ),
        Rule(
            "hidden-truth", 30,
            jsRegex("""(언론(이|에서|에)?\s?(절대\s?)?(안|못|숨기|보도하지|말하지)|방송(에|에서)\s?(절대\s?)?(안|못)\s?나오|정부가\s?숨기|아무도\s?모르는|충격(적인|적)?\s?(진실|사실)|의사(들)?(이|가)?\s?(절대\s?)?(말하지|알려주지)\s?않는)"""),
            "\"언론이 숨기는 진실\" 같은 표현은 근거 없는 글에서 자주 쓰여요.",
        ),
        Rule(
            "sensational", 15,
            jsRegex("""(충격|경악|긴급\s?속보|\[속보\]|발칵|난리\s?났|절대\s?(먹지|하지|사지)\s?마)"""),
            "자극적인 표현으로 놀라게 하고 있어요.",
        ),
        Rule(
            "miracle-cure", 30,
            jsRegex("""((암|당뇨|치매|고혈압|코로나|관절염|중풍).{0,25}(완치|낫는|나아|사라|특효|기적|잡는|뿌리\s?뽑)|(\d+\s?일|하루|일주일|한\s?달)\s?만에.{0,15}(완치|빠지|좋아|낫))""", "s"),
            "\"먹기만 하면 병이 낫는다\"는 식의 건강 정보는 대부분 사실이 아니에요.",
        ),
        Rule(
            "vague-source", 15,
            jsRegex("""(카더라|아는\s?사람(이|한테)|지인(이|의)\s?(말|얘기)|라고\s?(합니다|하네요|하더라)|들었습니다|받은\s?글)"""),
            "누가 말했는지 출처가 분명하지 않아요.",
        ),
        Rule(
            "exclamations", 10,
            jsRegex("""[!！]{3,}"""),
            "느낌표를 많이 써서 감정을 자극하고 있어요.",
        ),
    )

    fun findLinks(text: String): List<String> =
        URL_RE.findAll(text).map { TRAILING_PUNCT.replace(it.value, "") }.distinct().toList()

    private fun linkSignals(links: List<String>): List<Signal> {
        if (links.isEmpty()) return emptyList()
        val signals = mutableListOf(Signal("link", 15, "누르라고 하는 인터넷 주소(링크)가 들어 있어요."))
        val hosts = links.map { link ->
            WWW.replace(SCHEME.replace(link, ""), "").split(HOST_END)[0].lowercase(Locale.ROOT)
        }
        if (hosts.any { it in SHORTENERS }) {
            signals += Signal("short-link", 25, "진짜 주소를 숨기는 \"단축 주소\"가 쓰였어요.")
        }
        if (hosts.any { IP_HOST.containsMatchIn(it) }) {
            signals += Signal("ip-link", 30, "정상 업체라면 쓰지 않는 숫자로 된 주소예요.")
        }
        if (hosts.any { it.split('.').last() in RISKY_TLDS }) {
            signals += Signal("risky-domain", 25, "사기에 자주 쓰이는 낯선 주소예요.")
        }
        return signals
    }

    fun scoreToLevel(score: Int): Level = when {
        score >= 50 -> Level.DANGER
        score >= 20 -> Level.CAUTION
        else -> Level.SAFE
    }

    private fun evaluate(text: String, rules: List<Rule>, extra: List<Signal> = emptyList()): Evaluation {
        val signals = rules.filter { it.re.containsMatchIn(text) }.map { Signal(it.id, it.weight, it.reason) } + extra
        val score = minOf(100, signals.sumOf { it.weight })
        return Evaluation(score, scoreToLevel(score), signals)
    }

    fun analyzeText(text: String?): Analysis {
        val normalized = Normalizer.normalize(text.orEmpty(), Normalizer.Form.NFC)
        val links = findLinks(normalized)
        return Analysis(
            links = links,
            scam = evaluate(normalized, SCAM_RULES, linkSignals(links)),
            misinfo = evaluate(normalized, MISINFO_RULES),
        )
    }

    private val HEADLINES = mapOf(
        Category.SCAM to mapOf(
            Level.DANGER to "사기일 가능성이 높아요!",
            Level.CAUTION to "조심하세요. 의심스러운 부분이 있어요",
            Level.SAFE to "특별히 위험한 점은 못 찾았어요",
        ),
        Category.MISINFO to mapOf(
            Level.DANGER to "사실이 아닐 가능성이 높아요!",
            Level.CAUTION to "확인되지 않은 내용일 수 있어요",
            Level.SAFE to "특별히 이상한 점은 못 찾았어요",
        ),
    )

    private val ADVICE = mapOf(
        Category.SCAM to mapOf(
            Level.DANGER to "링크를 누르지 마세요. 전화하지 마세요. 돈이나 개인정보를 절대 보내지 마세요. 이미 보냈다면 바로 112에 전화하세요.",
            Level.CAUTION to "보낸 사람에게 원래 알던 번호로 직접 전화해서 확인하세요. 링크는 누르지 마세요.",
            Level.SAFE to "그래도 돈이나 개인정보를 달라고 하면 바로 의심하세요.",
        ),
        Category.MISINFO to mapOf(
            Level.DANGER to "다른 분께 보내지 마세요. 사실이 아닌 글을 보내면 받는 분이 피해를 볼 수 있어요.",
            Level.CAUTION to "보내기 전에 뉴스나 공식 기관에서 같은 내용이 나오는지 확인해 보세요.",
            Level.SAFE to "그래도 보내기 전에 누가 쓴 글인지 한 번 더 확인해 주세요.",
        ),
    )

    fun defaultAdvice(category: Category, level: Level): String = ADVICE.getValue(category).getValue(level)

    private fun maxLevel(a: Level, b: Level): Level = if (a.ordinal >= b.ordinal) a else b

    /**
     * 규칙 결과와 (있다면) AI 결과를 합쳐 화면에 보여줄 판정을 만든다.
     * 어르신 보호가 목적이므로 둘 중 더 위험한 쪽을 따른다.
     */
    fun buildVerdict(mode: Mode, rules: Analysis, ai: AiResult? = null): Verdict {
        val primary = if (mode == Mode.SHARE) Category.MISINFO else Category.SCAM
        val secondary = primary.other

        fun aiLevel(category: Category): Level =
            Level.fromKey(if (category == Category.SCAM) ai?.scamLevel else ai?.misinfoLevel) ?: Level.SAFE

        val levels = mapOf(
            Category.SCAM to maxLevel(rules.scam.level, aiLevel(Category.SCAM)),
            Category.MISINFO to maxLevel(rules.misinfo.level, aiLevel(Category.MISINFO)),
        )
        val category = if (levels.getValue(secondary).ordinal > levels.getValue(primary).ordinal) secondary else primary
        val level = levels.getValue(category)

        val ruleReasons = (rules[category].signals + rules[category.other].signals)
            .sortedByDescending { it.weight }
            .map { it.reason }
        val aiReasons = ai?.reasons.orEmpty().filter { it.isNotBlank() }
        val reasons = (aiReasons + ruleReasons).distinct().take(4)

        val aiAgrees = ai != null && maxLevel(aiLevel(Category.SCAM), aiLevel(Category.MISINFO)) == level
        var advice = ai?.advice?.takeIf { aiAgrees && it.isNotEmpty() } ?: defaultAdvice(category, level)
        if (mode == Mode.SHARE && level != Level.SAFE && category == Category.SCAM) {
            advice = "다른 분께 보내지 마세요. $advice"
        }

        return Verdict(
            mode = mode,
            category = category,
            level = level,
            headline = ai?.headline?.takeIf { aiAgrees && it.isNotEmpty() } ?: HEADLINES.getValue(category).getValue(level),
            reasons = reasons,
            advice = advice,
            factCheck = ai?.factCheck.orEmpty(),
            links = rules.links,
            usedAI = ai != null,
        )
    }
}
