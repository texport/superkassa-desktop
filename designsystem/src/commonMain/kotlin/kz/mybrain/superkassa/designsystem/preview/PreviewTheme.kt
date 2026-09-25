package kz.mybrain.superkassa.designsystem.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WindowClassRoot
import kz.mybrain.superkassa.designsystem.strings.ProvideStrings
import kz.mybrain.superkassa.designsystem.theme.SuperkassaTheme
import kz.mybrain.superkassa.strings.api.Language

/**
 * Обёртка превью: тема кассы, надписи языка и класс окна — как в окне кассы.
 *
 * Без неё превью берёт умолчания Compose: чужие цвета, надписи языка
 * по умолчанию и класс стартового окна кассы вместо того, в котором
 * превью нарисовано. Тема следует системе — так тёмное превью Android
 * Studio показывает тёмную кассу; класс окна меряется по месту превью.
 *
 * @param language язык надписей: по умолчанию русский — им читают превью.
 */
@Composable
fun PreviewTheme(language: Language = Language.Ru, content: @Composable () -> Unit) {
    SuperkassaTheme {
        ProvideStrings(language) {
            WindowClassRoot {
                Surface(modifier = Modifier.fillMaxSize(), content = content)
            }
        }
    }
}
