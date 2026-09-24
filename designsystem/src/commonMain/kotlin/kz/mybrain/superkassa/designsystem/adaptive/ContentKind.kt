package kz.mybrain.superkassa.designsystem.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.theme.size.ContentWidths

/**
 * Что за содержимое ограничивается по ширине.
 *
 * @property max предел ширины из [ContentWidths].
 * @property alignment где встаёт содержимое, когда окно шире предела.
 */
enum class ContentKind(val max: Dp, val alignment: Alignment.Horizontal) {

    /**
     * Текст — длинное объяснение, абзац: встаёт от левого края, под
     * заголовком раздела, и глаз начинает строку там же, где заголовок.
     * Формы и карточки так не ограничиваются: они занимают всю ширину
     * раздела и на широком окне встают рядом — см. [CardColumns].
     */
    Reading(ContentWidths.reading, Alignment.Start),

    /** Рабочий экран: встаёт посередине, поля окна расходятся поровну. */
    Workspace(ContentWidths.workspace, Alignment.CenterHorizontally)
}

/**
 * Ширина до предела и не больше.
 *
 * Пока место есть, содержимое занимает его целиком; дальше стоит на месте
 * с полями по сторонам. Заменяет `fillMaxWidth()` у корня блока — внутри
 * блока разметка остаётся прежней.
 */
fun Modifier.contentWidth(kind: ContentKind): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(kind.alignment)
        .widthIn(max = kind.max)
        .fillMaxWidth()
