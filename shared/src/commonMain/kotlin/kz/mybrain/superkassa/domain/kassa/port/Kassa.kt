package kz.mybrain.superkassa.domain.kassa.port

import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import kz.mybrain.superkassa.domain.kassa.model.ask

/**
 * Касса в процессе приложения, вызываемая из корутин.
 *
 * Фасад ядра [SuperkassaApi] — каноническая модель кассы, и своего
 * двойника с теми же семьюдесятью методами у приложения нет: экран зовёт
 * нужный метод фасада сам, через [call]. Порт отвечает на один вопрос —
 * где этот вызов выполняется. Фасад блокирующий: база, файлы и обмен
 * с ОФД идут в потоке вызова, и с главного потока его звать нельзя.
 *
 * Отказ ядра по существу приходит его же исключением `SuperkassaException`
 * с кодом и словами на трёх языках; разобрать итог помогает [ask].
 */
interface Kassa {

    /** Выполняет [request] над фасадом ядра вне главного потока. */
    suspend fun <T> call(request: (SuperkassaApi) -> T): T
}
