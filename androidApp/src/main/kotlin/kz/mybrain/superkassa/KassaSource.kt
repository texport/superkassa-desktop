package kz.mybrain.superkassa

import android.content.Context
import io.github.texport.superkassa.embedded.api.Superkassa
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.embedded.api.createSuperkassa
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import java.io.File

/**
 * Откуда берётся касса процесса: ядро на каталоге данных приложения
 * и настоящий БФД — в любой сборке.
 *
 * Отладочная сборка работает так же: касса и кабинет смотрят в один
 * и тот же стенд, и токен, выданный кабинетом, касса предъявляет тому
 * же БФД, что его выдал.
 */
internal object KassaSource {

    /** Открывает кассу; блокирует — ядро сверяет часы с эталоном в сети. */
    fun open(context: Context): Superkassa =
        createSuperkassa(SuperkassaPlatform(context, directory(context).path), EmbeddedKassa.config())

    /** Каталог данных кассы — во внутренней памяти приложения. */
    fun directory(context: Context): File = File(context.filesDir, DIRECTORY)

    private const val DIRECTORY = "superkassa"
}
