package kz.mybrain.superkassa

import android.app.Application
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.embedded.api.createSuperkassa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.data.local.AndroidWorkplace
import kz.mybrain.superkassa.data.log.LogcatJournal
import kz.mybrain.superkassa.domain.signin.SignIn
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.messages.Notices
import kz.mybrain.superkassa.presentation.strings.Language

/**
 * Точка сборки кассы на Android — единственное место, знающее все три слоя.
 *
 * Касса живёт столько же, сколько процесс, а не активность: поворот экрана
 * пересоздаёт активность, а второй экземпляр кассы на том же каталоге
 * ядро не откроет. Поднимается она вне главного потока — при открытии
 * ядро сверяет часы с эталоном в сети.
 */
class SuperkassaApp : Application() {
    private val scope: CoroutineScope = MainScope()

    /** Зависимости экранов; готовы, когда касса поднялась. */
    lateinit var container: Deferred<AppContainer>
        private set

    override fun onCreate() {
        super.onCreate()
        container = scope.async(Dispatchers.IO) { assemble() }
    }

    private fun assemble(): AppContainer {
        val kassa = createSuperkassa(SuperkassaPlatform(this), EmbeddedKassa.config())
        return AppContainer(
            // Общее.
            kassa = EmbeddedKassa(kassa.api),
            signIn = SignIn(),
            notices = Notices(),
            memory = AndroidWorkplace(this),
            journal = LogcatJournal(),
            language = { Language.byCode(null) },
            // Касса: продажа, возврат, деньги.

            // Журнал: история, очередь, печать.

            // Настройки и кассиры.

            // Кабинет и мастер заведения кассы.

            // Аналитика.

        )
    }
}
