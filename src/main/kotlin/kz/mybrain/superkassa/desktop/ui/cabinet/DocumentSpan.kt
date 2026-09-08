package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentPeriod
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import java.time.LocalDate
import java.time.ZoneId

/**
 * За какой срок смотреть документы.
 *
 * Прежде кабинет отдавал первые пятьдесят строк за всё время, и владелец,
 * пришедший посмотреть сегодняшнюю выручку, читал позапрошлый месяц.
 *
 * Сроки названы теми словами, которыми о них спрашивают: сегодня, неделя,
 * месяц. Полей ввода дат нет намеренно — в кабинете смотрят свежее,
 * а разбор давнего случая начинается с номера чека, а не с календаря.
 *
 * Границы считаются от начала суток рабочего места, а не «минус столько-то
 * часов»: смена начинается утром, и «неделя» обязана включать её целиком.
 */
enum class DocumentSpan(val title: (CabinetTexts) -> String, private val days: Long?) {
    Today({ it.periodToday }, 0),
    Week({ it.periodWeek }, WEEK),
    Month({ it.periodMonth }, MONTH),
    All({ it.periodAll }, null);

    /** Границы срока: у «Всего» их нет вовсе. */
    fun period(zone: ZoneId = ZoneId.systemDefault()): DocumentPeriod {
        val back = days ?: return DocumentPeriod()
        return DocumentPeriod(from = LocalDate.now(zone).minusDays(back).atStartOfDay(zone).toInstant())
    }
}

/**
 * Выбор срока сегментами.
 *
 * У смен срока нет: кабинет отбором по дате их не отдаёт, и ряд сегментов
 * над списком обещал бы отбор, которого не будет.
 */
@Composable
fun DocumentSpanSegments(
    kind: DocumentKind,
    span: DocumentSpan,
    texts: CabinetTexts,
    modifier: Modifier = Modifier,
    onSelect: (DocumentSpan) -> Unit
) {
    if (!kind.dated) return
    ChoiceSegments(
        options = DocumentSpan.entries,
        selected = span,
        label = { it.title(texts) },
        modifier = modifier,
        onSelect = onSelect
    )
}

/** Сколько суток назад отсчитывается неделя, считая сегодняшние. */
private const val WEEK = 6L

/** Столько же для месяца. */
private const val MONTH = 29L
