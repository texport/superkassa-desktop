package kz.mybrain.superkassa.presentation.print.preview

import kz.mybrain.superkassa.domain.print.model.ShareWay

/**
 * Печатная форма, открытая поверх разделов, и пин ради неё.
 *
 * @property image нарисованная лента; `null` — показывать нечего.
 * @property drawing касса сейчас рисует форму: окно открыто, картинки
 *   ещё нет. Без него нажатие секунду-другую не отзывалось ничем.
 * @property trouble касса формы не нарисовала: окно остаётся открытым
 *   и само говорит, что случилось.
 * @property pinFor название кассы, чей пин спрашивается; `null` — пин
 *   не спрашивают. Пин у касс разный, и владелец должен видеть, к какой.
 * @property savingName как назовётся файл, если открытую форму сохранят.
 * @property shareWays пути, которыми машина делится чеком; пусто — кнопки нет.
 */
internal data class PrintUiState(
    val image: ByteArray? = null,
    val drawing: Boolean = false,
    val trouble: PrintTrouble? = null,
    val pinFor: String? = null,
    val savingName: String? = null,
    val shareWays: List<ShareWay> = emptyList()
)

/**
 * Отказ кассы по печатной форме.
 *
 * Слова берутся у отказавшего: своя формулировка поверх чужого отказа
 * скрыла бы причину. Их может не быть вовсе — касса не ответила, — тогда
 * в окне стоит одно название беды.
 */
internal data class PrintTrouble(val words: String?)
