package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterState
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister,
    state: RegisterState?,
    onChanged: () -> Unit
) {
    SectionCard(
        title = texts.passport,
        trailing = { CabinetStatusChip(register.status, texts) }
    ) {
        Text(
            text = registerTitle(register),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            DetailLine(texts.registrationNumber, register.registrationNumber)
            DetailLine(texts.factoryNumber, register.factoryNumber)
            DetailLine(texts.model, register.model?.name ?: register.model?.modelCode)
            DetailLine(texts.manufactureYear, register.manufactureYear.takeIf { it > 0 }?.toString())
            DetailLine(texts.place, register.retailPlace?.name)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RegisterOnThisMachine(session, cabinet, texts, register, state)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RegisterEditCard(cabinet, texts, register, onChanged)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RegisterTokenBlock(cabinet, texts, register)
    }
}
