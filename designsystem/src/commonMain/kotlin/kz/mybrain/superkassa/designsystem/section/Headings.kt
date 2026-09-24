package kz.mybrain.superkassa.designsystem.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip

/**
 * Заголовки трёх ступеней — по ролям шкалы Material 3.
 *
 * Экран — `headline`, карточка — `title`, часть карточки — меньший
 * `title`; все цветом `onSurface`. Прежде каждый экран набирал заголовок
 * своим `Text` и своей ролью, и «История» стояла иным кеглем, чем
 * «Очередь отправки». Здесь ступени объявлены один раз, и экран
 * выбирает ступень, а не стиль.
 *
 * Отступ под заголовком экрана даёт столбец экрана, а не сам заголовок:
 * иначе он удваивался бы с промежутком столбца.
 */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Название раздела — одним начертанием во всех карточках приложения.
 *
 * Не помещается в строку — переносится на вторую, а не обрывается: рядом
 * с плашкой состояния по-казахски на крупном шрифте оставалось
 * «Техникалық к…», и раздел стоял без имени.
 */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = TITLE_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Название части раздела и объяснение к ней.
 *
 * Внутри карточки бывает своя часть со своим заголовком: токен, адрес
 * по шагам, работа этой кассы на этой машине. Строка «название и значок
 * подсказки» набиралась в каждой из них заново — здесь она объявлена
 * один раз, и подсказка везде встаёт на одно место.
 */
@Composable
fun SubsectionTitle(title: String, info: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        info?.let { InfoTip(it) }
    }
}

/** Строк у названия раздела: две вмещают любое, а короткое остаётся в одну. */
private const val TITLE_LINES = 2
