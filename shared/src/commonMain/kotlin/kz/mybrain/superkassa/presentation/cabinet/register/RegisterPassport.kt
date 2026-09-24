package kz.mybrain.superkassa.presentation.cabinet.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.presentation.cabinet.CabinetStatusChip
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.component.registerTitle
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.section.DetailLine
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Паспорт кассы: то, что о ней записано в кабинете, и что здесь же меняется.
 *
 * Название стоит крупно и отдельно от реквизитов: прежде оно шло тем же
 * кеглем, что и пять строк под ним, и карточка кассы открывалась столбцом
 * из шести одинаковых строк без единой опоры для глаза.
 *
 * Работа на этой машине стоит сразу под реквизитами: владелец открывает
 * паспорт, чтобы понять, встанет ли за эту кассу кассир здесь, — и ответ
 * должен стоять рядом с тем, по чему кассу опознают.
 *
 * Правка реквизитов и выдача токена стояли отдельными разделами ниже.
 * Раздела «Правка» больше нет: правятся ровно те строки, что в паспорте
 * и записаны, и место им рядом с ними. Токен выдаётся отсюда же — за ним
 * приходят один раз, сразу после создания кассы, и отдельный раздел
 * ради одной кнопки владельцу приходилось ещё найти.
 */
@Composable
fun RegisterPassport(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    view: RegisterView,
    model: RegisterViewModel
) {
    val register = view.card
    val window by cabinet.cabinet.state.collectAsScreenState()
    SectionCard(
        title = texts.passport,
        info = texts.hints.passport,
        trailing = { CabinetStatusChip(register.status, texts) }
    ) {
        PassportFacts(texts, register)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RegisterOnThisMachine(cabinet, texts, view)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RegisterEditCard(texts, register, window.busy, model)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RegisterTokenBlock(cabinet.cabinet, texts, register, view.state.here, window.busy)
    }
}

/** Название кассы крупно и реквизиты, по которым её опознают. */
@Composable
private fun PassportFacts(texts: CabinetTexts, register: CabinetRegister) {
    // Две строки, а не одна: название кассе даёт владелец, и у сети
    // оно длиннее строки — «Касса 3, Магазин «Достык Плаза» отдел 12».
    Text(
        text = registerTitle(register),
        style = MaterialTheme.typography.headlineSmall,
        maxLines = TITLE_LINES,
        overflow = TextOverflow.Ellipsis
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        DetailLine(texts.registrationNumber, register.registrationNumber)
        DetailLine(texts.factoryNumber, register.factoryNumber)
        DetailLine(texts.model, register.model?.name ?: register.model?.modelCode)
        DetailLine(texts.manufactureYear, register.manufactureYear.takeIf { it > 0 }?.toString())
        DetailLine(texts.place, register.retailPlace?.name)
    }
}

/** Сколько строк отводится названию кассы в заголовке паспорта. */
private const val TITLE_LINES = 2
