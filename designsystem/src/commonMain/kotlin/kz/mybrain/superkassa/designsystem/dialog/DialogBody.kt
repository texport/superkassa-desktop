package kz.mybrain.superkassa.designsystem.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.keyboard.scrolledByKeys
import kz.mybrain.superkassa.designsystem.list.ColumnScrollbar
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Тело диалога, которое прокручивается, когда не помещается.
 *
 * Material ограничивает диалог высотой окна, а его текст не прокручивает:
 * в окне 960×640 на крупной ступени подтверждение снятия кассы по-казахски
 * обрывалось на середине строки, и дочитать последствие было нечем.
 * Здесь лишнее уезжает под видимую полосу, а кнопки диалога остаются
 * на месте.
 *
 * Полоса стоит по высоте тела и не растягивает его: короткий вопрос
 * остаётся коротким окном. Клавиатура прокручивает тело только тогда,
 * когда есть что прокручивать: иначе тело вставало бы лишней остановкой
 * обхода между полем и кнопками.
 *
 * @param spacing промежуток между частями тела.
 */
@Composable
fun DialogBody(spacing: Dp = Spacing.fieldGap, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    val keys = if (scroll.maxValue > 0) Modifier.scrolledByKeys(scroll) { scroll.viewportSize } else Modifier
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .then(keys)
                .padding(end = Spacing.scrollbarGutter),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
        ColumnScrollbar(scroll, Modifier.matchParentSize().wrapContentWidth(Alignment.End))
    }
}

/**
 * Заголовок диалога со значком — по центру, как и сам значок.
 *
 * Material ставит строку заголовка под значком посередине, но строки
 * внутри неё выравнивает по левому краю: короткий вопрос выходил
 * по центру, а длинный, в две-три строки, — слева под центрованным
 * значком. Выравнивание задаётся самому тексту, и заголовок стоит
 * одинаково при любой длине.
 */
@Composable
fun DialogTitle(text: String) {
    Text(text = text, textAlign = TextAlign.Center)
}
