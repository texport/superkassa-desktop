package kz.mybrain.superkassa.desktop.app.log

/**
 * Что вырезается из журнала до записи.
 *
 * Журнал владелец пересылает в поддержку целиком, поэтому тайное не
 * попадает в него ни на каком уровне — включая отладочный. Вырезается
 * по имени поля, а не по месту вызова: тело запроса собирается где угодно,
 * и полагаться на то, что пишущий вспомнит про пин, нельзя.
 *
 * Пин кассира, токен кассы, пароль и подпись ЭЦП дают право на фискальные
 * команды: попавшие в пересланный файл, они равны выданному доступу.
 * ИИН, телефон и почта покупателя — персональные данные, и в разборе
 * отказа они не нужны.
 */
private val SECRET_WORDS = listOf(
    "pin",
    "password",
    "token",
    "secret",
    "authorization",
    "signature",
    "sign",
    "cms",
    "cert",
    "iin",
    "phone",
    "email",
    "buyer",
    "customer",
    "client"
)

/** Чем заменяется вырезанное: видно, что поле было, но не видно значения. */
const val HIDDEN: String = "•••"

/** Поле записи JSON: `"имя": значение`. */
private val JSON_PAIR = Regex("""["]([A-Za-z0-9_]+)["]\s*:\s*(["](?:\\.|[^"\\])*["]|[^,}\]\s]+)""")

/** Поле в строковом виде объекта запроса: `имя=значение`. */
private val PLAIN_PAIR = Regex("""([A-Za-z0-9_]+)=([^,)]*)""")

/** Доступ в заголовке: `Bearer <токен>`. */
private val BEARER = Regex("""(?i)Bearer\s+\S+""")

/** Сколько знаков тела попадает в журнал: дальше идёт повтор одного и того же. */
private const val BODY_LIMIT = 4000

/**
 * Вырезает тайное из текста перед записью.
 *
 * Разбор JSON здесь не нужен и вреден: тело приходит и обрезанным,
 * и не-JSON вовсе, а вырезать обязано в любом случае.
 */
fun hideSecrets(text: String): String {
    val withoutBearer = BEARER.replace(text, "Bearer $HIDDEN")
    val withoutJson = JSON_PAIR.replace(withoutBearer) { match ->
        val name = match.groupValues[1]
        if (name.isSecretName()) "\"$name\": \"$HIDDEN\"" else match.value
    }
    return PLAIN_PAIR.replace(withoutJson) { match ->
        val name = match.groupValues[1]
        if (name.isSecretName()) "$name=$HIDDEN" else match.value
    }
}

/**
 * Готовит тело запроса или ответа к записи: вырезает тайное и обрезает
 * до читаемой длины.
 */
fun bodyForLog(body: String?): String? {
    val text = body?.takeIf { it.isNotBlank() } ?: return null
    val hidden = hideSecrets(text)
    return if (hidden.length <= BODY_LIMIT) hidden else hidden.take(BODY_LIMIT) + "…"
}

/**
 * Тайное ли это поле.
 *
 * Сравнение по вхождению, а не по равенству: одно и то же значение ездит
 * под именами `token`, `ofdToken` и `accessToken`, и перечислять их все
 * означает пропустить следующее.
 */
private fun String.isSecretName(): Boolean {
    val name = lowercase()
    return SECRET_WORDS.any { name.contains(it) }
}
