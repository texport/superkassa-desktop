package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.rename
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Какая касса сейчас в работе.
 *
 * Карточка приподнята над остальными разделами: с неё кассир начинает
 * читать экран и по ней понимает, настройки какой машины перед ним.
 *
 * Название кассы уходит на узел: так её одинаково зовут за всеми
 * рабочими местами и видно это на входе, до того как кассир набрал пин.
 * Прочие сведения приходят от ОФД и здесь не меняются. Переименование
 * нужно там, где в зале три одинаковых кассы и их различают по месту,
 * а не по номеру.
 *
 * Смена кассы уводит на экран входа, а не переключается на месте: иначе
 * кассир меняет кассу, не заметив, и пробивает чек не на той машине.
 */
@Composable
internal fun CurrentKkmCard(session: Session) {
    val texts = LocalStrings.current
    val money = moneyTexts(session.language).kkm
    val kkm = session.selected ?: return
    val scope = rememberCoroutineScope()
    // Поле привязано к кассе: после смены кассы в нём не должно остаться
    // название предыдущей. Набранное переживает уход в другой раздел —
    // экран настроек уходит из состава вместе с ним.
    val field = SettingsDrafts.forKkm(SettingsDrafts.Field.KKM_NAME, kkm.kkmId)
    val chosenName = SettingsDrafts.of(field, session.displayName(kkm).takeIf { it != kkm.title }.orEmpty())

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = texts.settings.currentKkm,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(session.displayName(kkm), style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = { session.switchKkm() }) { Text(texts.settings.changeKkm) }
            }
            Text(
                text = whatItIs(session, kkm, texts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Поле и кнопки одной геометрии и выровнены по верху:
            // строка «поле — кнопка» разной высоты читается как сбой вёрстки.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalArrangement = Arrangement.spacedBy(Spacing.tight),
                itemVerticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = chosenName,
                    onValueChange = { SettingsDrafts.type(field, it) },
                    label = { Text(texts.settings.localName) },
                    trailingIcon = { InfoTip(texts.settings.localNameHint) },
                    singleLine = true,
                    enabled = !session.busy,
                    modifier = Modifier.fieldWidth(texts.settings.localName, Sizes.fieldName)
                )
                // Итог правки объявляет только удачу: отказ узла уже
                // показан его же словами, и «Название сохранено» поверх
                // него сказало бы владельцу неправду.
                //
                // Кнопка гаснет на время обращения: узел переименовывает
                // кассу секунду-другую, и второе нажатие отправляло к нему
                // то же название второй раз.
                FieldButton(texts.settings.save, enabled = !session.busy) {
                    scope.launch {
                        if (!session.rename(kkm, chosenName)) return@launch
                        SettingsDrafts.forget(field)
                        session.report(money.renameSaved)
                    }
                }
                FieldButton(
                    text = money.renameReset,
                    kind = FieldButtonKind.Outlined,
                    enabled = chosenName.isNotBlank() && !session.busy
                ) {
                    scope.launch {
                        if (!session.rename(kkm, null)) return@launch
                        SettingsDrafts.forget(field)
                        session.report(money.renameReset)
                    }
                }
            }
        }
    }
}

/**
 * Организация, заводской номер и адрес — одной строкой под названием.
 *
 * Организация здесь названа всегда: карточка отвечает на вопрос, чья это
 * касса, и пропуск строки читался бы как её отсутствие в карточке, а не
 * как отсутствие сведений у ОФД.
 */
private fun whatItIs(session: Session, kkm: Kkm, texts: AppStrings): String = listOfNotNull(
    kkm.orgTitle ?: texts.settings.orgUnknown,
    kkm.factoryNumber?.let { "${texts.login.factory} $it" },
    kkm.orgAddress.takeIf { it.isNotBlank() }
).joinToString(Glyphs.SEPARATOR).ifBlank { session.displayName(kkm) }
