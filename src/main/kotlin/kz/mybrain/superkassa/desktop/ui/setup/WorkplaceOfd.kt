package kz.mybrain.superkassa.desktop.ui.setup

import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.components.OfdTarget

/**
 * ОФД и контур, с которыми работает это рабочее место.
 *
 * Мастер подставлял первым значение справочника узла — чужого ОФД, — и
 * касса заводилась в него с первого же нажатия, если владелец не заметил
 * подмены. А ответ известен: рабочее место стоит в одной торговой точке
 * и шлёт чеки одному ОФД, и это записано у касс, уже заведённых на узле.
 *
 * Расходятся кассы во мнении — берётся тот, которым пользуется больше
 * касс: одна перенесённая с другого контура не должна перевешивать
 * остальные. Касс нет вовсе или их ОФД нет в справочнике — остаётся
 * первый из справочника: подставить нечего, и врать об этом незачем.
 */
fun workplaceOfd(
    kkms: List<Kkm>,
    providers: List<DictionaryEntry>,
    environments: List<DictionaryEntry>
): OfdTarget = OfdTarget(
    provider = mostUsed(kkms.map { it.ofdId }, providers) ?: providers.firstOrNull()?.code.orEmpty(),
    environment = mostUsed(kkms.map { it.ofdEnvironment }, environments)
        ?: environments.firstOrNull()?.code.orEmpty()
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
