package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.OfdServiceInfoResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.setup.port.SetupMemory
import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.domain.workplace.port.LookMemory
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Касса процесса для проверок: касса, кассир, смена и документы в типах ядра.
 *
 * Значения те же, что у снимков окна (`KassaScene` проверок экранов):
 * снимок экрана и проверка сценария говорят об одной и той же кассе.
 */
object CoreScene {
    const val PIN = "1234"

    fun kkm(
        state: String = "ACTIVE",
        kgd: String? = "000000200042",
        name: String? = "Касса у входа",
        id: String = "kkm-1",
        blockReasonCode: Int? = null
    ) = KkmResponse(
        kkmId = id,
        createdAt = 0,
        updatedAt = 0,
        mode = "REGISTRATION",
        state = state,
        name = name,
        kkmKgdId = kgd,
        factoryNumber = "SK-000042",
        ofdServiceInfo = ORG,
        blockReasonCode = blockReasonCode
    )

    fun cashier(admin: Boolean = true) =
        UserResponse(userId = "u-1", name = "Айгүл Сәрсенова", role = if (admin) UserRole.ADMIN else UserRole.CASHIER)

    fun openShift(number: Long = 7, openedAt: Long = System.currentTimeMillis()) = ShiftResponse(
        id = "shift-$number",
        kkmId = "kkm-1",
        shiftNo = number,
        status = ShiftStatus.OPEN,
        openedAt = openedAt
    )

    fun document(id: String, type: String = "SALE", amount: Long? = 150_000, status: String? = "ONLINE_OK") =
        FiscalDocumentResponse(
            id = id,
            cashboxId = "kkm-1",
            shiftId = "shift-7",
            docType = type,
            docNo = null,
            printedDocumentNumber = null,
            shiftNo = 7,
            createdAt = 0,
            totalAmount = amount,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = status,
            ofdErrorCode = null,
            deliveredAt = null
        )

    /** Список касс, как его отдаёт касса: одна страница. */
    fun page(kkms: List<KkmResponse>) = KkmListResponse(items = kkms, total = kkms.size)

    private val ORG = OfdServiceInfoResponse(
        orgTitle = "ТОО «Пример»",
        orgAddress = "Алматы, Абая 150",
        orgAddressKz = "Алматы, Абай 150",
        orgIinOrBin = "000000000000",
        orgOked = "47111",
        geoLatitude = 0,
        geoLongitude = 0,
        geoSource = "MANUAL"
    )
}

/** Память рабочего места без диска: у каждой проверки своя. */
class MemoryWorkplace(
    override var rememberedKkmId: String? = null,
    names: Map<String, String> = emptyMap(),
    domains: Map<String, String> = emptyMap(),
    override var collapsedPanels: Set<String> = emptySet()
) : WorkplaceMemory {
    /** Свои названия касс; настройки пишут сюда же, откуда читает окно. */
    val names: MutableMap<String, String> = names.toMutableMap()

    /** Отрасли касс; настройки пишут сюда же, откуда читает продажа. */
    val domains: MutableMap<String, String> = domains.toMutableMap()

    override fun localName(kkmId: String): String? = names[kkmId]

    override fun domain(kkmId: String): String? = domains[kkmId]
}

/** Вид окна без диска: у каждой проверки свой. */
class MemoryLook(override var look: LookChoice = LookChoice()) : LookMemory

/** Журнал, которого нет: проверкам он не нужен, а файл рабочей машины трогать нельзя. */
object SilentJournal : Journal {
    override fun info(text: String) = Unit

    override fun warn(text: String) = Unit

    override fun failure(text: String) = Unit
}

/** Пройденное мастера в памяти: у каждой проверки своё. */
class MemorySetup : SetupMemory {
    private val values = mutableMapOf<String, String>()

    override fun setupValue(name: String): String? = values[name]

    override fun setupValue(name: String, value: String?) {
        if (value == null) values.remove(name) else values[name] = value
    }
}
