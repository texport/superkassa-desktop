package kz.mybrain.superkassa.presentation.shell

import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts
import kz.mybrain.superkassa.presentation.settings.SettingsPorts

/**
 * Порты областей приложения — по одному набору на область.
 *
 * Касса своего набора не заводит: ей хватает общих служб [AppContainer].
 * Область, которой понадобился свой набор, дописывает его сюда одним
 * полем и одной строкой в каждой точке сборки.
 *
 * @property journal доставка чека покупателю — для журнала документов.
 * @property settings настройки, печать, обновление и журнал отладки.
 * @property analytics аналитика кабинета и службы карт.
 * @property cabinet кабинет БФД: точки, кассы, заявления, подпись;
 *   `null` — на этой платформе кабинета нет.
 * @property setup мастер подключения кассы: память пройденного и кабинет;
 *   `null` — на этой платформе мастера нет.
 */
data class AreaPorts(
    val journal: JournalPorts,
    val settings: SettingsPorts,
    val analytics: AnalyticsPorts,
    val cabinet: CabinetPorts? = null,
    val setup: SetupPorts? = null
)
