package kz.mybrain.superkassa.desktop.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Kkm

/** Что открыто на входе: список касс или одна из дверей рядом с ним. */
enum class Door { Kkms, Register, Cabinet, Settings }

/**
 * Что кассир набрал на входе.
 *
 * Живёт в окне, а не в самом экране: полоса пина стоит в нижнем слоте
 * каркаса, чтобы снекбар отказа её не закрывал, — и набранное нужно
 * и полосе, и списку касс над ней.
 */
class LoginState {

    var pin: String by mutableStateOf("")

    /** Касса, выбранная мышью; набор номера этот выбор отменяет. */
    var chosen: Kkm? by mutableStateOf(null)

    var search: String by mutableStateOf("")

    /**
     * Ответил ли узел на первое чтение списка.
     *
     * До ответа список пуст не потому, что касс нет: без этого кассир
     * на запуске читал «касс нет» про узел с десятком касс.
     */
    var answered: Boolean by mutableStateOf(false)

    var door: Door by mutableStateOf(Door.Kkms)

    /** Кассы, подходящие под набранное кассиром. */
    fun shown(session: Session): List<Kkm> =
        session.kkms.filter { it.matches(search, session.displayName(it)) }

    /**
     * Какая касса откроется набранным пином.
     *
     * Порядок важен: набранный кассиром номер сильнее всего остального.
     * Иначе после смены кассира поиск другой кассы ничего не менял —
     * подставлялась прежняя, и кассир входил не туда, куда набрал.
     */
    fun chosenKkm(session: Session): Kkm? = chosen
        ?: shown(session).singleOrNull()
        ?: session.kkms.firstOrNull { it.kkmId == session.rememberedKkmId }
        ?: session.selected

    /**
     * Спрашивать ли сейчас пин.
     *
     * За открытой дверью — заведением кассы, кабинетом, настройками —
     * полосы пина нет: там кассир ничего не вводит, а нижняя полоса окна
     * поверх чужого экрана читается как часть этого экрана.
     */
    fun atPin(session: Session): Boolean = door == Door.Kkms && session.kkms.isNotEmpty()
}
