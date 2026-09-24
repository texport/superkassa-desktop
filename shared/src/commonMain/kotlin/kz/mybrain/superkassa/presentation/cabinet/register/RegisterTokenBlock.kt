package kz.mybrain.superkassa.presentation.cabinet.register

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.common.button.BusyButton
import kz.mybrain.superkassa.presentation.common.button.FieldButtonKind
import kz.mybrain.superkassa.presentation.common.section.DetailLine
import kz.mybrain.superkassa.presentation.common.section.SubsectionTitle
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts

/**
 * Технический токен кассы.
 *
 * Выдаётся по требованию владельца и показывается один раз: это ключ,
 * которым касса подписывает запросы, и место ему в настройках кассы,
 * а не в журнале кабинета.
 *
 * Кнопка гаснет там, где кабинет токен не выдаст: по черновику и по кассе
 * с поданным заявлением. Прежде она нажималась всегда, и владелец получал
 * отказ сервера — по-английски и кодом.
 *
 * Выданное значение обёрнуто в область выделения: его переносят в другое
 * приложение, а переписывать десять цифр с экрана руками — верный способ
 * ошибиться в одной.
 *
 * Кассе, которая работает на этой же машине, новый токен вписывается сразу.
 * Прежде владелец одним нажатием ломал свою кассу: БФД отзывал прежний
 * токен, касса о новом не знала и на первом же чеке получала «неверный
 * токен» и блокировку — а приложение говорило лишь «Токен выдан». Когда касса
 * здесь, но кассир вошёл в другую, вписать за него нельзя — тогда об этом
 * сказано словами.
 */
@Composable
fun RegisterTokenBlock(
    cabinet: CabinetViewModel,
    texts: CabinetTexts,
    register: CabinetRegister,
    here: KkmResponse?,
    busy: Boolean
) {
    val model = tokenViewModel(cabinet)
    val issued by model.state.collectAsState()
    val seat by cabinet.useCases.observe().collectAsState()
    val allowed = tokenAllowed(register)
    // Вписать токен можно только в ту кассу, в которую вошли: пин
    // принадлежит кассе, и чужой к этой не подойдёт.
    val mine = here?.takeIf { seat.signedIn && it.kkmId == seat.kkm?.kkmId }
    SubsectionTitle(texts.token, texts.hints.token)
    // Почему кнопка погасла — строкой: это состояние кассы, а не объяснение
    // раздела, и владелец должен видеть его не открывая подсказку.
    if (!allowed) Explanation(texts.tokenOnlyRegistered)
    if (here != null && mine == null) Explanation(texts.tokenNeedsNode)
    issued?.takeIf { it.registerId == register.id }?.let { value ->
        SelectionContainer {
            DetailLine(texts.tokenIssued, value.token.toString())
        }
    }
    // Кнопка тональная, а не залитая: залитая на экране одна, и это подача
    // заявления — то, ради чего карточку кассы и открывают. Токен выдают
    // один раз, и две залитые кнопки подряд не говорили, какую нажимать.
    BusyButton(
        text = texts.issueToken,
        busy = busy,
        enabled = allowed,
        kind = FieldButtonKind.Tonal,
        onClick = { model.issue(register.id, mine) }
    )
}

/** Состояние кассы словами: почему кнопка погасла или что ещё сделать. */
@Composable
private fun Explanation(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
