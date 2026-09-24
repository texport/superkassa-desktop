package kz.mybrain.superkassa.data.releases

import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kz.mybrain.superkassa.domain.update.port.Releases
import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kotlin.time.Instant

/**
 * Выпуски кассы на Android.
 *
 * Приложение на Android обновляет магазин приложений, а не касса:
 * установщиков под Android среди выпусков нет, и спрашивать о них
 * незачем. Карточки выпусков на Android нет ([ownReleases]), проверка
 * по расписанию выключена и не включается, а версия приложения стоит
 * в сведениях о кассе. Красное «сервер выпусков недоступен» на карточке
 * звало кассира чинить то, что не сломано.
 */
class StoreReleases : Releases, UpdateMemory {

    override val ownReleases: Boolean = false

    override suspend fun latest(): ReleaseAnswer = ReleaseAnswer.Unreachable(FROM_STORE)

    override suspend fun fetch(installer: Installer): Fetched = Fetched.Failed(FROM_STORE)

    override fun openFile(file: String): Boolean = false

    override fun openPage(url: String): Boolean = false

    override var automatic: Boolean
        get() = false
        set(value) = Unit

    override var lastChecked: Instant? = null

    private companion object {
        const val FROM_STORE = "android updates come from the store"
    }
}
