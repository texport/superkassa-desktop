package kz.mybrain.superkassa.presentation.shell.starting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.kassa.model.StartProblem
import kz.mybrain.superkassa.presentation.common.adaptive.ContentKind
import kz.mybrain.superkassa.presentation.common.adaptive.contentWidth
import kz.mybrain.superkassa.presentation.common.adaptive.windowMargin
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.words.shell.of
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Касса не открылась: что случилось и что делать сейчас.
 *
 * Экран, а не окно с ошибкой поверх пустоты: кассир читает его, как любой
 * другой экран кассы, на своём языке, и первым видит, что делать. Сведения
 * для обслуживания стоят ниже мелким шрифтом и выделяются — их пересылают,
 * а не пересказывают. Действие на экране одно: закрыть кассу.
 */
@Composable
fun StartRefusedScreen(problem: StartProblem, onClose: () -> Unit) {
    val texts = textsOf(LocalLanguage.current).shell
    val words = texts.of(problem.refusal)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(windowMargin).contentWidth(ContentKind.Reading),
            verticalArrangement = Arrangement.spacedBy(Spacing.cardGap, Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = AppIcons.warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Sizes.emptyIcon)
            )
            Text(texts.title, style = MaterialTheme.typography.headlineSmall)
            Text(words.reason, style = MaterialTheme.typography.bodyLarge)
            Text(words.action, style = MaterialTheme.typography.titleMedium)
            SupportDetail("${texts.forSupport}: ${problem.detail}")
            Button(onClick = onClose) { Text(texts.close) }
        }
    }
}

/** Сведения для обслуживания: мелко и выделяемо — их пересылают, а не пересказывают. */
@Composable
private fun SupportDetail(text: String) {
    SelectionContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
