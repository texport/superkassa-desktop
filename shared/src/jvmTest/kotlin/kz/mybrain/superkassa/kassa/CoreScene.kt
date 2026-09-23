package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.OfdServiceInfoResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.data.node.Kkm
import kz.mybrain.superkassa.domain.journal.Journal
import kz.mybrain.superkassa.domain.signin.SignIn
import kz.mybrain.superkassa.domain.workplace.WorkplaceMemory
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.messages.Notices
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.strings.Language

/**
 * Касса процесса для проверок: касса, кассир, смена и документы в типах ядра.
 *
 * Значения те же, что у снимков на узле ([kz.mybrain.superkassa.KassaScene]):
 * снимок экрана на ядре и снимок на узле рисуют одну и ту же кассу.
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

    /** Касса снимка на узле в типах ядра: те же номера, название и организация. */
    fun of(kkm: Kkm) = kkm(
        state = kkm.state ?: "ACTIVE",
        kgd = kkm.kkmKgdId,
        name = kkm.name,
        id = kkm.kkmId,
        blockReasonCode = kkm.blockReasonCode
    ).let {
        it.copy(
            autonomousSince = kkm.autonomousSince,
            offlineQueueCount = kkm.offlineQueueCount ?: 0,
            factoryNumber = kkm.factoryNumber,
            ofdServiceInfo = ORG.copy(orgTitle = kkm.orgTitle.orEmpty(), orgAddress = kkm.orgAddress)
        )
    }

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

    /** Зависимости экранов поверх [core]; вход и строка сообщений — свои. */
    fun app(
        core: FakeCore,
        signIn: SignIn = SignIn(),
        notices: Notices = Notices(),
        memory: WorkplaceMemory = MemoryWorkplace()
    ) = AppContainer(
        kassa = core.kassa(),
        signIn = signIn,
        notices = notices,
        memory = memory,
        journal = SilentJournal,
        language = { Language.Ru }
    )

    /** Список касс, как его отдаёт касса: одна страница. */
    fun page(kkms: List<KkmResponse>) = KkmListResponse(items = kkms, total = kkms.size)

    /** Зависимости экранов, делящие вход и строку сообщений с [session]. */
    fun app(session: Session, core: FakeCore = FakeCore()) = app(core, session.signIn, session.notices)

    private val ORG = OfdServiceInfoResponse(
        orgTitle = "ТОО «Пример»",
        orgAddress = "Алматы, Абая 150",
        orgAddressKz = "Алматы, Абай 150",
        orgInn = "000000000000",
        orgOkved = "47111",
        geoLatitude = 0,
        geoLongitude = 0,
        geoSource = "MANUAL"
    )
}

/** Память рабочего места без диска: у каждой проверки своя. */
class MemoryWorkplace(
    override var rememberedKkmId: String? = null,
    private val names: Map<String, String> = emptyMap()
) : WorkplaceMemory {
    override fun localName(kkmId: String): String? = names[kkmId]
}

/** Журнал, которого нет: проверкам он не нужен, а файл рабочей машины трогать нельзя. */
object SilentJournal : Journal {
    override fun info(text: String) = Unit

    override fun warn(text: String) = Unit

    override fun failure(text: String) = Unit
}
