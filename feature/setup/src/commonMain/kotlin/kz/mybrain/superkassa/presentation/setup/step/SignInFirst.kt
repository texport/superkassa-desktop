package kz.mybrain.superkassa.presentation.setup.step

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Вход в кабинет по ЭЦП на шаге, которому нужен кабинет.
 *
 * Кнопка — та же, что на двери кабинета: со сроком ожидания подписи
 * и отменой. Как подписывать — NCALayer на компьютере или иначе на
 * телефоне, — решает кабинет окна, а не мастер.
 */
@Composable
internal fun SignInFirst(cabinet: CabinetSteps) {
    Text(
        text = textsOf(LocalLanguage.current).setup.signInFirst,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    cabinet.SignIn()
}
