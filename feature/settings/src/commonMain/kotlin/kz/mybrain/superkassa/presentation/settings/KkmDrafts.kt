package kz.mybrain.superkassa.presentation.settings

/**
 * Набранное, но не сохранённое — у каждой кассы своё, по её `kkmId`.
 *
 * Налоги, свои строки чека и название у каждой кассы свои, а рабочее
 * место переключается между кассами. Общий черновик показал бы набранное
 * для одной кассы в настройках другой, а сброс при смене кассы терял
 * набранное: владелец уходил к другой кассе и, вернувшись, набирал заново.
 * Черновик живёт в модели окна и только до закрытия окна: на диск
 * попадает то, что сохранили кнопкой.
 */
data class KkmDrafts<T>(private val byKkm: Map<String, T> = emptyMap()) {

    /** Черновик кассы [kkmId]; `null` — касса не выбрана или набранного нет. */
    fun of(kkmId: String?): T? = kkmId?.let(byKkm::get)

    /** Черновик кассы [kkmId] заменён; `null` — забыт. Без кассы черновика нет. */
    fun with(kkmId: String?, draft: T?): KkmDrafts<T> = when {
        kkmId == null -> this
        draft == null -> KkmDrafts(byKkm - kkmId)
        else -> KkmDrafts(byKkm + (kkmId to draft))
    }
}
