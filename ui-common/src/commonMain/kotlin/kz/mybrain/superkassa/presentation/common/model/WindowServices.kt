package kz.mybrain.superkassa.presentation.common.model

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Общие службы окна — то, о чём спрашивает каждая область.
 *
 * Фабрика модели области получает их вместе со своим набором портов и
 * собирает из них сценарии; модели достаются только сценарии и [talk].
 * Порты других областей сюда не входят: область, которой понадобился
 * порт, получает его своим набором, а не находит у соседа.
 *
 * @property kassa касса в процессе приложения.
 * @property signIn кто за кассой: одно на приложение место входа.
 * @property memory что рабочее место помнит между запусками.
 * @property look вид окна: язык, тема, шрифт и свёрнутые части — один на окно.
 * @property talk строка сообщений окна, журнал и язык кассира — чем модели говорят с ним.
 */
class WindowServices(
    val kassa: Kassa,
    val signIn: SignIn,
    val memory: WorkplaceMemory,
    val look: WorkplaceLook,
    val talk: Talk
)
