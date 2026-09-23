package kz.mybrain.superkassa

import io.github.texport.superkassa.embedded.api.Superkassa
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.embedded.api.createSuperkassa
import io.github.texport.superkassa.importnode.api.NodeImportResult
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.data.local.DataHome
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.data.log.AppJournal
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.data.log.LogLevel
import kz.mybrain.superkassa.data.log.LogSource
import kz.mybrain.superkassa.data.node.NodeHandover
import kz.mybrain.superkassa.data.node.ServerClient
import kz.mybrain.superkassa.domain.signin.SignIn
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.messages.Notices
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.strings.CommonStrings
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.strings.stringsOf
import javax.swing.JOptionPane

/**
 * Точка сборки настольной кассы — единственное место, знающее все три слоя.
 *
 * Здесь реализации `data` становятся портами `domain` и уходят экранам
 * в [AppContainer]. Область, заведшая новый порт, дописывает его строку
 * в [assemble] — под своей областью, как и в самом контейнере.
 */

/** Собранное приложение: сеанс разделов на узле и зависимости переведённых. */
internal class Assembly(val session: Session, val app: AppContainer)

/** Собирает зависимости экранов: порты `domain` из реализаций `data`. */
internal fun assemble(kassa: Superkassa, preferences: Preferences): Assembly {
    val signIn = SignIn()
    val notices = Notices()
    // Адрес узла читается из настроек при каждом обращении: его меняют
    // с экрана входа, и перезапуск ради этого не нужен.
    val session = Session(ServerClient(address = { preferences.nodeUrl }), preferences, signIn, notices)
    val app = AppContainer(
        // Общее.
        kassa = EmbeddedKassa(kassa.api),
        signIn = signIn,
        notices = notices,
        memory = preferences,
        journal = AppJournal(),
        language = { session.language },
        // Касса: продажа, возврат, деньги.

        // Журнал: история, очередь, печать.

        // Настройки и кассиры.

        // Кабинет и мастер заведения кассы.

        // Аналитика.

    )
    return Assembly(session, app)
}

/**
 * Поднимает кассу на каталоге данных рабочего места.
 *
 * Второй экземпляр на том же каталоге касса не откроет: кассиру говорится
 * об этом окном, а не трассой в консоли, которой он не видит.
 */
internal fun openKassa(preferences: Preferences): Superkassa? {
    val directory = DataHome.kassa()
    return runCatching { createSuperkassa(SuperkassaPlatform(directory.path), EmbeddedKassa.config()) }
        .onFailure { refuse(preferences, "kassa did not open", it) { kassaNotOpened.format(directory.path) } }
        .getOrNull()
}

/**
 * Переносит данные узла в кассу процесса, если перенос включён.
 *
 * Несостоявшийся перенос останавливает запуск: пустая касса выглядела бы
 * потерянными сменами и толкала бы к повторной регистрации.
 */
internal fun handOverNode(preferences: Preferences): Result<NodeImportResult?> =
    runCatching { NodeHandover.run(DataHome.directory(), DataHome.kassa(), preferences.nodeUrl) }
        .onFailure { failure ->
            refuse(preferences, "node data not moved", failure) { nodeHandoverFailed.format(failure.message.orEmpty()) }
        }

/** Отказ запуска: в журнал — что и почему, кассиру — окном его словами. */
private fun refuse(
    preferences: Preferences,
    what: String,
    failure: Throwable,
    words: CommonStrings.() -> String
) {
    AppLog.record(LogSource.Machine, LogLevel.Failure, "$what: ${failure::class.simpleName}")
    val texts = stringsOf(Language.byCode(preferences.language)).common
    JOptionPane.showMessageDialog(null, texts.words(), APP_NAME, JOptionPane.ERROR_MESSAGE)
}
