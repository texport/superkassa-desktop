package kz.mybrain.superkassa.presentation.cabinet.register

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegisterState
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationAction

/**
 * Выбранная касса кабинета: карточка, состояние, след заявлений и то,
 * как она соотносится с кассами этой машины.
 *
 * @property card карточка кассы; в ней есть то, чего нет в строке списка, —
 *   признак регистрационной карты и последнее действие. `null` — не открыта.
 * @property state состояние по учёту кабинета и по снимку БФД; `null` —
 *   кабинет его не отдал.
 * @property open какие разделы карточки раскрыты.
 * @property kkms кассы этой машины — какими их знает касса в процессе.
 * @property kkmsRead касса ответила списком: до ответа «здесь не заведена»
 *   было бы неправдой.
 */
internal data class RegisterUiState(
    val card: CabinetRegister? = null,
    val state: RegisterState? = null,
    val actions: List<RegistrationAction> = emptyList(),
    val open: Set<RegisterBlock> = OPENED_AT_START,
    val kkms: List<KkmResponse> = emptyList(),
    val kkmsRead: Boolean = false
) {
    /** Касса этой машины, заведённая под этой кассой кабинета; `null` — здесь её нет. */
    val here: KkmResponse? get() = card?.let { (nodeWork(it, kkms) as? NodeWork.Here)?.kkm }

    companion object {
        /**
         * Что раскрыто при открытии кассы.
         *
         * Состояние и заявления: за ними в карточку и приходят. Остальное —
         * заголовками, чтобы карточка помещалась на экран целиком.
         */
        val OPENED_AT_START: Set<RegisterBlock> = setOf(RegisterBlock.Technical, RegisterBlock.Applications)
    }
}

/** Разделы карточки кассы. */
internal enum class RegisterBlock { Technical, Applications, Card, Journal }
