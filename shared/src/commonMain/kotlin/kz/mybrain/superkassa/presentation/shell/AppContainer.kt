package kz.mybrain.superkassa.presentation.shell

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.strings.common.Language

/**
 * Всё, что экранам нужно снаружи: порты `domain`, собранные из `data`.
 *
 * Собирается один раз — в точке сборки платформенного приложения, `Assembly.kt`
 * настольной кассы и `SuperkassaApp` Android, — и отдаётся каркасу окна.
 * Модель экрана контейнер не получает: фабрика модели области берёт отсюда
 * нужные порты, собирает из них сценарии и отдаёт модели только их
 * и [talk]. Так модель не видит ни кассы, ни пина, ни чужих портов.
 *
 * Общие службы лежат здесь, порты областей — в [areas], по набору на
 * область. Область, которой понадобился новый порт, дописывает его в свой
 * набор, а не сюда: контейнер не растёт с каждой областью, а правки
 * областей не пересекаются.
 *
 * @property kassa касса в процессе приложения.
 * @property signIn кто за кассой: одно на приложение место входа.
 * @property memory что рабочее место помнит между запусками.
 * @property look вид окна: язык, тема, шрифт и свёрнутые части — один на окно.
 * @property talk строка сообщений окна, журнал и язык кассира — чем модели говорят с ним.
 * @property areas порты областей — по набору на область.
 */
class AppContainer(
    val kassa: Kassa,
    val signIn: SignIn,
    val memory: WorkplaceMemory,
    val look: WorkplaceLook,
    val talk: Talk,
    val areas: AreaPorts
) {
    /** Строка сообщений окна. */
    val notices: Notices get() = talk.notices

    val journal: Journal get() = talk.journal

    /** Язык кассира сейчас. */
    val language: () -> Language get() = talk.language
}
