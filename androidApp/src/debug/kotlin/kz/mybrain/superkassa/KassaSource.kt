package kz.mybrain.superkassa

import android.content.Context
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.embedded.api.Superkassa
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.testing.api.kassa.KassaSetup
import io.github.texport.superkassa.testing.api.kassa.TestBench
import io.github.texport.superkassa.testing.api.kassa.VatMode
import java.io.File

/**
 * Откуда берётся касса процесса в отладочной сборке: настоящее ядро
 * на тестовом БФД, поднятом в процессе приложения.
 *
 * Так приложение проходят на эмуляторе без стенда и без сети: вход,
 * смена, чеки, отчёты. Документы такой кассы в БФД не уходят и фискальной
 * силы не имеют. В выпуск этот файл и оснастка ядра не попадают — они
 * подключены только к отладочной сборке.
 *
 * Тестовый БФД живёт в памяти процесса и заведённых прежде касс не помнит,
 * поэтому каталог стенда — свой, отдельный от каталога кассы, — при каждом
 * запуске процесса начинается с чистого листа и кассы заводятся заново.
 * Часы стенда стоят на времени запуска: так устроена оснастка ядра.
 *
 * Пинов по умолчанию у кассы нет; эти заданы здесь, в отладочном коде:
 * администратор [ADMIN_PIN], кассир [CASHIER_PIN].
 */
internal object KassaSource {

    /** Открывает стенд и заводит на нём две кассы: без НДС и плательщика НДС. */
    fun open(context: Context): Superkassa {
        val directory = File(context.noBackupFilesDir, BENCH).apply { deleteRecursively() }
        val bench = TestBench.open(SuperkassaPlatform(context, directory.path))
        bench.registerKassa(KassaSetup(ADMIN_PIN, CASHIER_PIN, VatMode.NotPayer, name = "Касса у входа"))
        bench.registerKassa(KassaSetup(ADMIN_PIN, CASHIER_PIN, VatMode.Payer(VatGroup.VAT_16), name = "Касса с НДС"))
        return bench.superkassa
    }

    private const val BENCH = "bench"
    private const val ADMIN_PIN = "7391"
    private const val CASHIER_PIN = "4826"
}
