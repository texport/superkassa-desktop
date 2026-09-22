package kz.mybrain.superkassa.desktop.ui.users

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.KkmUser
import kz.mybrain.superkassa.desktop.server.changeUserPin
import kz.mybrain.superkassa.desktop.server.removeUser
import kz.mybrain.superkassa.desktop.server.users
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.ScreenTitle
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
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
    // Узел отдаёт кассиров отдельным обращением: до ответа список пуст,
    // и «кассиров нет» про кассу с пятью кассирами — неправда.
    var answered by remember(session.selected?.kkmId) { mutableStateOf(false) }
    // Отказ узла запоминается: без него список навсегда оставался
    // в ожидании и показывал владельцу пустую рамку без единого слова.
    var unreadable by remember(session.selected?.kkmId) { mutableStateOf(false) }

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
        if (list == null) {
            unreadable = true
            return
        }
        unreadable = false
        loaded.clear()
        loaded.addAll(list)
        answered = true
    }

    LaunchedEffect(session.selected?.kkmId, session.pin) { reload() }

    ScrollableColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        spacing = Spacing.normal
    ) {
        ScreenTitle(texts.users.title)

        AddCashier(session, money) { reload() }

        SectionCard(title = money.cashiers.listTitle, info = money.cashiers.listHint) {
            val state = when {
                loaded.isNotEmpty() -> ScreenState.Ready
                // Отказ узла назван словами и с повтором: пустая касса
                // и касса, о кассирах которой не спросить, — разные беды,
                // и вторая до этого выглядела пустой рамкой навсегда.
                unreadable -> ScreenState.Trouble(
                    title = money.cashiers.unreadable,
                    hint = money.cashiers.unreadableHint,
                    onRetry = { scope.launch { reload() } }
                )

                !answered -> ScreenState.Working
                else -> ScreenState.Empty(AppIcons.cashiers, money.cashiers.empty, money.cashiers.emptyHint)
            }
            ScreenSlot(state, dense = true) {
                loaded.forEachIndexed { index, user ->
                    if (index > 0) {
                        HorizontalDivider()
                    }
                    // Свой ли это кассир: от этого зависит и обещание
                    // продолжить работу новым пином, и то, каким пином
                    // экран перечитывает список.
                    val own = UserRules.same(session.whoami, user)
                    UserRow(
                        money = money,
                        roleTitle = roleTitle(session, texts.users, user.role),
                        user = user,
                        own = own,
                        deletable = !UserRules.lastOfRole(loaded, user),
                        onChangePin = { newPin ->
                            changePin(session, texts, user, newPin) {
                                reload(changedPin = newPin.takeIf { own })
                            }
                        },
                        onRemove = { scope.launch { removeCashier(session, texts, user) { reload() } } }
                    )
                }
            }
        }
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
