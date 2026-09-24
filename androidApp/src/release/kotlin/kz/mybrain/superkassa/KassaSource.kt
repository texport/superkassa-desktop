package kz.mybrain.superkassa

import android.content.Context
import io.github.texport.superkassa.embedded.api.Superkassa
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.embedded.api.createSuperkassa
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa

/**
 * Откуда берётся касса процесса в выпуске: ядро на каталоге данных
 * приложения и настоящий БФД.
 *
 * Отладочная сборка кладёт на это место свою кассу — на тестовом БФД;
 * в выпуск её код и оснастка ядра не попадают.
 */
internal object KassaSource {

    /** Открывает кассу; блокирует — ядро сверяет часы с эталоном в сети. */
    fun open(context: Context): Superkassa = createSuperkassa(SuperkassaPlatform(context), EmbeddedKassa.config())
}
