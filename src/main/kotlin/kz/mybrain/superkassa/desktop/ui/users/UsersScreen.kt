package kz.mybrain.superkassa.desktop.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.KkmUser
import kz.mybrain.superkassa.desktop.server.changeUserPin
import kz.mybrain.superkassa.desktop.server.removeUser
import kz.mybrain.superkassa.desktop.server.users
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.CashierTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Кассиры кассы: список, заведение, смена пина, удаление.
 *
 * Правила пинов не собраны в отдельную памятку, а стоят там, где кассир
 * с ними сталкивается: длина — подсказкой под полем пина, роли — под
 * выбором роли, запрет на удаление последнего — в строке того кассира,
 * которого не дают удалить.
 */
@Composable
fun UsersScreen(session: Session) {
    val texts = LocalStrings.current
    val money = moneyTexts(session.language)
    val scope = rememberCoroutineScope()
    val loaded = remember { mutableStateListOf<KkmUser>() }

    /**
     * Перечитывает список кассиров.
     *
     * [changedPin] — пин, только что заданный в этом же экране. Если список
     * прежним пином больше не читается, значит кассир сменил пин самому себе:
     * узел старого уже не знает. Тогда работа продолжается новым пином, а не
     * обрывается отказом на удавшейся операции.
     */
    suspend fun reload(changedPin: String? = null) {
        val kkm = session.selected ?: return
        val list = session.guard(texts.users.title) { session.client.users(kkm.kkmId, session.pin) }
            ?: changedPin?.let { fresh ->
                session.guard(texts.users.title) { session.client.users(kkm.kkmId, fresh) }
                    ?.also { session.adoptPin(fresh) }
            }
            ?: return
        loaded.clear()
        loaded.addAll(list)
    }

    LaunchedEffect(session.selected?.kkmId, session.pin) { reload() }

    ScrollableColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        spacing = Spacing.normal
    ) {
        Text(texts.users.title, style = MaterialTheme.typography.headlineSmall)

        AddCashier(session, money) { reload() }

        Text(money.cashiers.listTitle, style = MaterialTheme.typography.titleMedium)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            if (loaded.isEmpty()) {
                NoCashiers(money.cashiers)
                return@OutlinedCard
            }
            loaded.forEachIndexed { index, user ->
                if (index > 0) {
                    HorizontalDivider()
                }
                UserRow(
                    money = money,
                    roleTitle = session.titleOf(Dictionary.UserRoles, user.role),
                    user = user,
                    deletable = !UserRules.lastOfRole(loaded, user),
                    onChangePin = { newPin ->
                        changePin(session, texts, user, newPin) { reload(changedPin = newPin) }
                    },
                    onRemove = { scope.launch { removeCashier(session, texts, user) { reload() } } }
                )
            }
        }
    }
}

/** Пустой список: значок, что здесь будет, и с чего начать. */
@Composable
private fun NoCashiers(money: CashierTexts) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(money.empty, style = MaterialTheme.typography.titleSmall)
        Text(
            text = money.emptyHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Задаёт кассиру новый пин.
 *
 * Список перечитывается до объявления итога: перечитывание снимает
 * сообщение за собой, и объяви мы «пин изменён» раньше — оно погасло бы
 * вместе с ответом на список.
 */
private suspend fun changePin(
    session: Session,
    texts: AppStrings,
    user: KkmUser,
    newPin: String,
    reload: suspend () -> Unit
): Boolean {
    val kkm = session.selected ?: return false
    val done = session.guard(texts.users.change) {
        session.client.changeUserPin(kkm.kkmId, user.identifier, newPin, session.pin)
    } != null
    if (done) {
        reload()
        session.report("${user.name} — ${texts.users.changed}")
    }
    return done
}

/** Удаляет кассира. Имя запоминается до перечитывания списка. */
private suspend fun removeCashier(
    session: Session,
    texts: AppStrings,
    user: KkmUser,
    reload: suspend () -> Unit
) {
    val kkm = session.selected ?: return
    session.guard(texts.users.delete) {
        session.client.removeUser(kkm.kkmId, user.identifier, session.pin)
    } ?: return
    val removed = user.name
    reload()
    session.report("$removed — ${texts.users.deleted}")
}
