package kz.mybrain.superkassa.presentation.setup

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import kz.mybrain.superkassa.domain.setup.model.EnrollmentPlan
import kz.mybrain.superkassa.domain.setup.model.KkmSetupDraft
import kz.mybrain.superkassa.domain.setup.model.OfdContours
import kz.mybrain.superkassa.domain.users.model.UserRules
import kz.mybrain.superkassa.strings.api.setup.SetupTexts

/**
 * Мастер подключения кассы, каким его видит владелец.
 *
 * @property draft пройденное, как его запомнил мастер.
 * @property startingOver спрошено, забыть ли пройденное.
 * @property contours контуры БФД со слов кассы; пусто — касса их не назвала,
 *   и заводить кассу некуда.
 * @property kkms кассы, уже заведённые на этом рабочем месте: по ним
 *   подставляется контур и видно, пройден ли последний шаг.
 * @property viaCabinet последний шаг пути через кабинет: контур и пин.
 * @property byHand ручной путь: контур, идентификатор, токен и пин.
 */
data class SetupUiState(
    val way: SetupWay = SetupWay.ViaCabinet,
    val draft: KkmSetupDraft = KkmSetupDraft(),
    val startingOver: Boolean = false,
    val contours: List<OfdEnvironmentResponse> = emptyList(),
    val kkms: List<KkmResponse> = emptyList(),
    val gettingFactory: Boolean = false,
    val viaCabinet: KkmForm = KkmForm(),
    val byHand: KkmForm = KkmForm()
) {
    /** Форма того пути, которым идёт владелец. */
    val form: KkmForm get() = if (way == SetupWay.ByHand) byHand else viaCabinet

    /** То же состояние с [form] на месте формы того пути, которым идёт владелец. */
    fun withForm(form: KkmForm): SetupUiState =
        if (way == SetupWay.ByHand) copy(byHand = form) else copy(viaCabinet = form)

    /**
     * Контур, в который уйдёт касса.
     *
     * Пока владелец не выбрал сам, подставлен контур этого рабочего места —
     * тот, с которым работают его кассы. Прежде здесь стояло первое значение
     * справочника, то есть случайный контур.
     */
    val contour: String
        get() = form.contour.ifEmpty { OfdContours.ofWorkplace(kkms, contours.map { it.code }) }

    /**
     * Что заводится: через кабинет идентификатор и название переносит мастер
     * из пройденного, вручную идентификатор набирает владелец.
     */
    val plan: EnrollmentPlan
        get() = if (way == SetupWay.ByHand) {
            EnrollmentPlan(contour, form.systemId, form.adminPin)
        } else {
            EnrollmentPlan(contour, draft.systemId, form.adminPin, draft.name)
        }

    /**
     * Последний шаг пройден: касса с идентификатором из черновика уже здесь.
     *
     * Сверка идёт только тогда, когда идентификатор есть: без черновика он
     * пуст, и «пусто равно пусто» помечало шаг пройденным у любого, у кого
     * есть касса без сведений о БФД.
     */
    val connected: Boolean
        get() = draft.systemId?.let { known -> kkms.any { it.ofdSystemId == known } } == true

    /**
     * Можно заводить кассу.
     *
     * Контур обязателен наравне с пином: пока касса контуров не назвала,
     * подставлять нечего, а кнопка оживала от одного пина и заводила кассу
     * в пустой контур — с уже выданным на неё токеном кабинета.
     */
    val ready: Boolean
        get() {
            val filled = form.systemId.isNotBlank() && form.token.isNotBlank()
            return !form.busy && contour.isNotBlank() && form.pinConfirmed && (way == SetupWay.ViaCabinet || filled)
        }
}

/**
 * Что набрано для заведения кассы.
 *
 * Идентификатор и токен набирают только на ручном пути: через кабинет
 * их переносит сам мастер. Пин — пин администратора будущей кассы; его
 * набирают дважды: стандартного пина у кассы нет, и опечатка в единственном
 * пине закрыла бы кассу от владельца.
 */
data class KkmForm(
    val contour: String = "",
    val systemId: String = "",
    val token: String = "",
    val adminPin: String = "",
    val adminPinRepeat: String = "",
    val busy: Boolean = false
) {
    /** Пин годен и набран дважды одинаково. */
    val pinConfirmed: Boolean get() = UserRules.pinAccepted(adminPin) && adminPin == adminPinRepeat

    /** Повтор набран и с пином расходится: об этом говорится под полем повтора. */
    val pinsDiffer: Boolean get() = adminPinRepeat.isNotEmpty() && adminPinRepeat != adminPin

    override fun toString(): String = "KkmForm(contour=$contour, systemId=$systemId, busy=$busy)"
}

/**
 * Каким путём подключают кассу.
 *
 * Через кабинет — когда владелец с ключом ЭЦП сидит за этим же
 * компьютером. Вручную — когда кассу в БФД завёл сервисник или бухгалтер,
 * и на руках только идентификатор и токен: заставлять в этом случае
 * проходить кабинет значит требовать чужой ключ.
 */
enum class SetupWay(val title: (SetupTexts) -> String) {
    ViaCabinet({ it.viaCabinet }),
    ByHand({ it.manually })
}
