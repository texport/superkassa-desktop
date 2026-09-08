package kz.mybrain.superkassa.desktop.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDoor
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.LanguagePicker
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.components.underFieldLabel
import kz.mybrain.superkassa.desktop.ui.setup.ConnectKkmScreen
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors
import kz.mybrain.superkassa.desktop.ui.users.UserRules

/**
 * Вход в кассу.
 *
 * Кассир выбирает свою кассу и вводит пин один раз за смену — дальше пин
 * не спрашивается ни на одном экране. Касса запоминается: на рабочем месте
 * она не меняется, и утром достаточно ввести пин.
 *
 * Раскладка та же, что у списка с действием по Material 3: перечень
 * прокручивается, а пин и кнопка стоят в отдельной поверхности внизу
 * и никуда не уезжают, сколько бы касс ни было в списке.
 */
@Composable
fun LoginScreen(session: Session, cabinet: CabinetSession) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Kkm?>(null) }
    var search by remember { mutableStateOf("") }
    // Заведение кассы открывается прямо отсюда: пока не заведена первая
    // касса, войти некуда, а настройки живут за входом.
    var registering by remember { mutableStateOf(false) }
    // Кабинет открывается до выбора кассы и без пина: пока первая касса
    // не заведена, пина кассира не существует вовсе, а завести кассу
    // можно только начав с кабинета.
    var atCabinet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { session.refreshKkms() }

    // Подстановка считается тут же, а не в отложенном эффекте: список
    // приходит с узла позже первой отрисовки, и эффект успевал отработать
    // на пустом списке — кассир видел «касса не выбрана» при запомненной.
    val shown = session.kkms.filter { it.matches(search, session.displayName(it)) }

    // Когда поиск оставил ровно одну кассу, она и есть выбранная: кассир
    // набирает номер своей кассы и сразу пин, не тянясь к мыши.
    // Порядок важен: набранный кассиром номер сильнее всего остального.
    // Иначе после смены кассира поиск другой кассы ничего не менял —
    // подставлялась прежняя, и кассир входил не туда, куда набрал.
    val chosenKkm = chosen
        ?: shown.singleOrNull()
        ?: session.kkms.firstOrNull { it.kkmId == session.rememberedKkmId }
        ?: session.selected

    /**
     * Вход: касса и пин запоминаются в сеансе.
     *
     * Перечитывание состояния делает каркас — он живёт всё время работы,
     * а этот экран исчезает в тот же миг, и запущенное здесь обновление
     * обрывалось бы на полпути.
     */
    fun enter() {
        val kkm = chosenKkm ?: return
        scope.launch { session.signIn(kkm, pin) }
    }

    val reload = { scope.launch { session.refreshKkms() } }
    if (registering) {
        ConnectKkmScreen(session, cabinet) { registering = false }
        return
    }
    if (atCabinet) {
        CabinetDoor(session, cabinet) { atCabinet = false }
        return
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.width(Sizes.loginColumn).fillMaxSize()) {
            LoginHeader(session)
            if (session.kkms.isEmpty()) {
                EmptyKkms(
                    onReload = { reload() },
                    onCabinet = { atCabinet = true },
                    onRegister = { registering = true }
                )
                return@Column
            }
            // Набранный номер отменяет прежний выбор мышью: он сделан позже,
            // а значит и означает намерение кассира. Иначе набор «2000042»
            // после клика по другой кассе не менял ничего, и кассир входил
            // не туда, куда набрал.
            SearchField(search) {
                search = it
                chosen = null
            }
            KkmList(
                kkms = shown,
                nameOf = { session.displayName(it) },
                chosenId = chosenKkm?.kkmId,
                rememberedId = session.rememberedKkmId,
                modifier = Modifier.weight(1f)
            ) { chosen = it }
            // Две двери рядом: кассир входит пином ниже, владелец —
            // своей ЭЦП в кабинет. Обе со значками и в рамке: текстовыми
            // вподбор они терялись, а кабинет для нового владельца —
            // единственный вход, пока нет ни кассы, ни компании.
            Row(
                modifier = Modifier.padding(horizontal = Spacing.roomy),
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                DoorButton(AppIcons.newKkm, texts.sections.register) { registering = true }
                DoorButton(AppIcons.cabinet, texts.sections.cabinet) { atCabinet = true }
            }
            SignInBar(
                pin = pin,
                nameOf = { session.displayName(it) },
                onPin = { pin = UserRules.digitsOf(it) },
                chosen = chosenKkm,
                onEnter = ::enter,
                onReload = { reload() }
            )
        }
    }
}

/** Заголовок экрана: название, язык и состояние связи с узлом. */
@Composable
private fun LoginHeader(session: Session) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(Spacing.roomy)
    ) {
        Text(
            texts.login.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f)
        )
        LanguagePicker(session)
        Chip(
            text = if (session.nodeAvailable) texts.common.nodeOnline else texts.common.nodeOffline,
            color = if (session.nodeAvailable) StatusColors.delivered else StatusColors.refused
        )
    }
}

/**
 * Список касс пуст.
 *
 * Чаще всего это недоступный узел, а не отсутствие касс, и кассир должен
 * видеть разницу. Кнопка повтора обязана остаться на экране: без неё
 * пробовать снова нечем, кроме перезапуска кассы.
 */
@Composable
private fun EmptyKkms(onReload: () -> Unit, onCabinet: () -> Unit, onRegister: () -> Unit) {
    val texts = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        EmptyState(
            icon = AppIcons.kkm,
            title = texts.login.noKkmChosen,
            hint = texts.login.noKkms
        )
        // Завести кассу — главное действие пустого экрана: без кассы
        // повторять запрос списка можно бесконечно.
        Button(onClick = onRegister) { Text(texts.sections.register) }
        // Вторая дверь стоит и здесь: у владельца, который только начал,
        // нет ни кассы, ни компании, ни точки — и всё это заводится
        // в кабинете, а не в кассе.
        DoorButton(AppIcons.cabinet, texts.sections.cabinet, onCabinet)
        TextButton(onClick = onReload) { Text(texts.login.reload) }
    }
}

/**
 * Пин и вход.
 *
 * Отдельная поверхность с тональной подложкой: по Material 3 действие,
 * закреплённое у нижнего края, отделяется от прокручиваемого списка
 * не отступом, а собственным уровнем поверхности.
 */
@Composable
private fun SignInBar(
    pin: String,
    nameOf: (Kkm) -> String,
    onPin: (String) -> Unit,
    chosen: Kkm?,
    onEnter: () -> Unit,
    onReload: () -> Unit
) {
    val texts = LocalStrings.current
    val ready = chosen != null && UserRules.pinEnterable(pin)

    // Кассир набирает пин и жмёт Enter, не тянясь к мыши: на настольной
    // кассе клавиатурное действие поля важнее, чем в мобильном Material,
    // и одним `imeAction` его не получить.
    val submit = {
        if (ready) {
            onEnter()
            true
        } else {
            false
        }
    }
    // Блок пина стоит в тех же полях, что и список: разъехавшиеся по
    // ширине карточки на одном экране читаются как разные разделы.
    Surface(
        tonalElevation = Sizes.barElevation,
        shape = RoundedCornerShape(Sizes.largeCorner),
        modifier = Modifier.fillMaxWidth().padding(Spacing.roomy)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy).onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && event.key in ENTER_KEYS && submit()
            },
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = onPin,
                label = { Text(texts.common.pin) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                modifier = Modifier.fieldWidth(texts.common.pin, Sizes.fieldPin)
            )
            Column(modifier = Modifier.weight(1f).underFieldLabel()) {
                Text(
                    // Одно название без слова «Касса» перед ним: у кассы,
                    // названной «Касса у входа», получалось «Касса Касса
                    // у входа».
                    text = chosen?.let(nameOf) ?: texts.login.noKkmChosen,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (chosen == null) {
                    Text(
                        texts.login.pickHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onReload, modifier = Modifier.underFieldLabel()) {
                Icon(AppIcons.refresh, contentDescription = texts.login.reload)
            }
            FieldButton(texts.login.enter, FieldButtonKind.Filled, enabled = ready, onClick = onEnter)
        }
    }
}

/** Обычный Enter и Enter на цифровой части клавиатуры — одно и то же. */
private val ENTER_KEYS = setOf(Key.Enter, Key.NumPadEnter)

/**
 * Дверь с экрана входа: заведение кассы и кабинет ОФД.
 *
 * Рамка и значок, а не текст вподбор: это входы для владельца, и они
 * должны читаться как входы, не соперничая при этом с главным действием
 * экрана — входом кассира по пину.
 */
@Composable
private fun DoorButton(icon: ImageVector, title: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null)
        Text(text = title, modifier = Modifier.padding(start = Spacing.tight))
    }
}
