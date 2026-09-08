package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.LanguagePicker
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Кто вошёл и чем он распоряжается.
 *
 * Прежде это была строка текста в две ступени серого, и владелец
 * с несколькими компаниями не отличал одну карточку от другой, не открывая
 * раздел «Компания». Теперь это опознавательная строка кабинета: название
 * набрано шкалой заголовка, реквизит и вошедший уведены в служебную.
 *
 * Собрана на `ListItem`: у него уже рассчитаны рост, поля и три роли цвета
 * для главного, служебного и действий справа.
 */
@Composable
fun CabinetHeader(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { CabinetBadge() },
        headlineContent = {
            Text(
                text = cabinet.company?.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = ownerLine(cabinet, texts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = { HeaderActions(session, cabinet, texts) }
    )
}

/** Язык кабинета и выход из него. */
@Composable
private fun HeaderActions(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Язык переключается здесь же: кабинет государственный, и владелец
        // вправе вести его по-казахски, не выходя обратно на экран входа.
        LanguagePicker(session)
        TextButton(onClick = { scope.launch { cabinet.signOut() } }) { Text(texts.signOut) }
    }
}

/**
 * Значок кабинета в кружке.
 *
 * Своя разметка вместо готового составного: у Material 3 нет отдельного
 * элемента для опознавательного значка строки — гайдлайн описывает его
 * содержимым `leadingContent` и оставляет вид на усмотрение приложения.
 */
@Composable
private fun CabinetBadge() {
    Box(
        modifier = Modifier
            .size(Sizes.headerIcon)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AppIcons.cabinet,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Реквизит компании и вошедший — одной служебной строкой.
 *
 * Пустые части выпадают, а не оставляют висящие разделители: у только что
 * заведённой компании имени в кабинете может ещё не быть.
 */
private fun ownerLine(cabinet: CabinetSession, texts: CabinetTexts): String = listOf(
    ownerIdentifier(cabinet, texts),
    cabinet.user?.fullName.orEmpty()
).filter { it.isNotBlank() }.joinToString(" · ")
