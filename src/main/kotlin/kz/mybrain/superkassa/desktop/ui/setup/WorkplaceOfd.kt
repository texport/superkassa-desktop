package kz.mybrain.superkassa.desktop.ui.setup

import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.components.OfdTarget
import kz.mybrain.superkassa.desktop.ui.components.environmentRaised

/**
 * Контур БФД, с которым работает это рабочее место.
 *
 * Мастер подставлял первым значение справочника узла и попадал на контур,
 * которого владелец не выбирал. А ответ известен: рабочее место стоит
 * в одной торговой точке и шлёт чеки в один контур, и это записано
 * у касс, уже заведённых на узле.
 *
 * Расходятся кассы во мнении — берётся тот контур, которым пользуется
 * больше касс: одна перенесённая не должна перевешивать остальные.
 * Касс нет вовсе или их контура нет в справочнике — остаётся первый
 * поднятый: подставить нечего, а погашенный выбором не станет.
 */
fun workplaceOfd(kkms: List<Kkm>, environments: List<DictionaryEntry>): OfdTarget = OfdTarget(
    environment = mostUsed(kkms.map { it.ofdEnvironment }, environments)
        ?: environments.firstOrNull { it.environmentRaised() }?.code.orEmpty()
)

/**
 * Значение, которым пользуется больше всего касс.
 *
 * Берутся только коды, которые узел называет в справочнике: код, которого
 * в справочнике нет, поле выбора показать не сможет и встанет пустым.
 */
private fun mostUsed(codes: List<String?>, known: List<DictionaryEntry>): String? = codes
    .filterNotNull()
    .filter { code -> known.any { it.code == code } }
    .groupingBy { it }
    .eachCount()
    .maxByOrNull { it.value }
    ?.key
