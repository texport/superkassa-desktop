package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Кассы компании: список слева, выбранная касса справа.
 *
 * Две колонки, а не переход на отдельный экран: владелец сравнивает кассы
 * между собой — эта на учёте, у той заявление в ИСНА, — и возврат к списку
 * после каждой кассы сбивал бы сравнение.
 */
@Composable
fun RegistersPage(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    var chosen by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cabinet.token) { cabinet.refreshRegisters() }

    // Своего зазора у ряда нет: его даёт поле под полосу прокрутки
    // в колонке слева, и равно оно отступу от края экрана. Второй зазор
    // поверх делал полосу пустоты вдвое шире, чем поля вокруг.
    Row(modifier = Modifier.fillMaxSize()) {
        RegisterList(session, cabinet, texts, chosen) { chosen = it }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val register = cabinet.registers.firstOrNull { it.id == chosen }
        if (register == null) {
            EmptyState(
                icon = AppIcons.kkm,
                title = texts.chooseRegister,
                hint = texts.chooseRegisterHint,
                modifier = Modifier.weight(1f)
            )
        } else {
            RegisterDetails(cabinet, texts, register, modifier = Modifier.weight(1f))
        }
    }
}

/** Колонка со списком касс и формой заведения под ним. */
@Composable
private fun RegisterList(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    chosen: String?,
    onChoose: (String) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.width(Sizes.registerColumn).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        // Строка о пустом списке стоит внутри прокрутки, а не над ней:
        // сверху она отрывалась от кнопки заведения на всю высоту окна.
        ScrollableColumn(modifier = Modifier.weight(1f), spacing = Spacing.tight, gutter = Spacing.screen) {
            SectionCard(title = texts.registers) {
                if (cabinet.registers.isEmpty()) {
                    EmptyState(AppIcons.kkm, texts.registersEmpty, texts.registersEmptyHint)
                }
                cabinet.registers.forEach { register ->
                    RegisterRow(register, texts, register.id == chosen) { onChoose(register.id) }
                }
            }
        }
        // Под списком стоит кнопка, а не форма: пять полей в узкой колонке
        // отжимали список наверх и рвали его вёрстку, а заводят кассу
        // раз в жизни.
        FilledTonalButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Text(texts.addRegister)
        }
    }
    if (adding) {
        AddRegisterDialog(session, cabinet, texts, onDismiss = { adding = false }) { created ->
            onChoose(created.id)
        }
    }
}

/**
 * Строка кассы в списке.
 *
 * Своё название владельца стоит первым, а номер КГД и точка под ним:
 * «Касса у входа» владелец узнаёт быстрее, чем `KGD-2000302`, но номер
 * нужен для разговора с ОФД. Состояние — плашкой справа, а не третьей
 * строкой: в столбце из десятка касс глаз ищет отличающуюся.
 */
@Composable
private fun RegisterRow(
    register: CabinetRegister,
    texts: CabinetTexts,
    selected: Boolean,
    onSelect: () -> Unit
) {
    RecordRow(
        title = registerTitle(register),
        subtitle = listOfNotNull(register.registrationNumber, register.retailPlace?.name)
            .joinToString(" · "),
        selected = selected,
        onClick = onSelect,
        trailing = { CabinetStatusChip(register.status, texts) }
    )
}

/**
 * Как назвать кассу.
 *
 * Своё название владельца, а без него — номер учёта, а без него —
 * заводской: только что заведённая касса не имеет ни того, ни другого,
 * и безымянной строки в списке быть не должно.
 */
fun registerTitle(register: CabinetRegister): String =
    register.internalName?.takeIf { it.isNotBlank() }
        ?: register.registrationNumber
        ?: register.factoryNumber.orEmpty()
