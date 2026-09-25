package kz.mybrain.superkassa.presentation.cabinet

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.SignedCabinet
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.cabinet.register.RegisterUiState
import kz.mybrain.superkassa.presentation.cabinet.register.RegisterView
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Чтение хозяйства кабинета без модели окна: вошедший владелец, списки
 * и состояние, в которое они ложатся. Модель читала бы хозяйство сама,
 * как только владелец вошёл, — проверке списков это мешало бы.
 */
internal class CabinetListsRig(client: CabinetWire, services: WindowServices = CoreScene.services(FakeCore())) {
    val screen = MutableStateFlow(CabinetUiState())

    // Порты без модели окна: модель, увидев вошедшего, читала бы хозяйство
    // сама, в своём потоке, — и её страницы смешивались с проверяемыми.
    private val ports = SignedCabinet(client).also { it.enter() }.ports
    val lists = CabinetLists(services.talk, cabinetCases(services, ports), CabinetWork(services.talk), screen)

    val state: CabinetUiState get() = screen.value
}

/** Касса в карточке так, как её видит модель: и кассы этой машины, если [here] заведена здесь. */
internal fun viewOf(register: CabinetRegister, here: KkmResponse? = null) =
    RegisterView(register, register, RegisterUiState(card = register, kkms = listOfNotNull(here), kkmsRead = true))
