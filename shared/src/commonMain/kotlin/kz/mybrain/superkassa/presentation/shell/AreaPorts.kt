package kz.mybrain.superkassa.presentation.shell

import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.domain.debug.port.DebugPorts
import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.domain.print.port.PrintPorts
import kz.mybrain.superkassa.domain.settings.port.SettingsPorts
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.domain.update.port.UpdatePorts
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts

/**
 * Порты областей приложения — по одному набору на область.
 *
 * Область, которой понадобился свой набор, дописывает его сюда одним
 * полем и одной строкой в каждой точке сборки.
 *
 * @property kassa настройки доставки чека — для продажи и возврата: кассир
 *   выбирает только те виды контакта покупателя, чей канал настроен.
 * @property journal доставка чека покупателю — для журнала документов.
 * @property settings настройки кассы на этой машине и самой машины.
 * @property print принтер и диск машины для печатной формы и выбор принтера кассы.
 * @property update служба выпусков и память о проверке.
 * @property debug журнал рабочего места.
 * @property analytics аналитика кабинета и службы карт.
 * @property cabinet кабинет БФД: точки, кассы, заявления, подпись;
 *   `null` — на этой платформе кабинета нет.
 * @property setup мастер подключения кассы: память пройденного и кабинет;
 *   `null` — на этой платформе мастера нет.
 */
data class AreaPorts(
    val kassa: KassaPorts,
    val journal: JournalPorts,
    val settings: SettingsPorts,
    val print: PrintPorts,
    val update: UpdatePorts,
    val debug: DebugPorts,
    val analytics: AnalyticsPorts,
    val cabinet: CabinetPorts? = null,
    val setup: SetupPorts? = null
)
