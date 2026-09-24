package kz.mybrain.superkassa.presentation.users.signin

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kkm.model.displayName
import kz.mybrain.superkassa.domain.kkm.model.matches

/** Что открыто на входе: список касс или одна из дверей рядом с ним. */
enum class Door { Kkms, Register, Cabinet, Settings }

/**
 * Экран входа, каким его видит кассир.
 *
 * @property kkms кассы, заведённые на этом рабочем месте.
 * @property localNames свои названия касс рабочего места: ключ — касса.
 * @property answered ответила ли касса на первое чтение списка; до ответа
 *   пустой список — не «касс нет», а «ещё не знаем».
 * @property listRead прочитан ли список: пустой прочитанный и непрочитанный —
 *   разные беды.
 * @property pickedId касса, выбранная мышью; набор номера этот выбор отменяет.
 * @property rememberedId касса, за которой работали в прошлый раз.
 * @property selectedId касса, оставшаяся за местом после ухода кассира.
 * @property entering пин ушёл на проверку, ответа ещё нет.
 */
data class LoginUiState(
    val kkms: List<KkmResponse> = emptyList(),
    val localNames: Map<String, String> = emptyMap(),
    val answered: Boolean = false,
    val listRead: Boolean = false,
    val search: String = "",
    val pickedId: String? = null,
    val rememberedId: String? = null,
    val selectedId: String? = null,
    val pin: String = "",
    val door: Door = Door.Kkms,
    val entering: Boolean = false
) {
    /** Кассы, подходящие под набранное кассиром. */
    val shown: List<KkmResponse>
        get() = kkms.filter { it.matches(search, localNames[it.kkmId]) }

    /**
     * Какая касса откроется набранным пином.
     *
     * Порядок важен: выбор мышью, затем набранный номер, если под него
     * подходит одна касса, затем касса прошлого раза. Иначе после смены
     * кассира поиск другой кассы ничего не менял — подставлялась прежняя,
     * и кассир входил не туда, куда набрал.
     */
    val chosen: KkmResponse?
        get() = kkms.firstOrNull { it.kkmId == pickedId }
            ?: shown.singleOrNull()
            ?: kkms.firstOrNull { it.kkmId == rememberedId }
            ?: kkms.firstOrNull { it.kkmId == selectedId }

    /**
     * Спрашивать ли сейчас пин.
     *
     * За открытой дверью — заведением кассы, кабинетом, настройками —
     * полосы пина нет: там кассир ничего не вводит.
     */
    val atPin: Boolean get() = door == Door.Kkms && kkms.isNotEmpty()

    /** Как зовут кассу на этом рабочем месте. */
    fun nameOf(kkm: KkmResponse): String = kkm.displayName(localNames[kkm.kkmId])
}
