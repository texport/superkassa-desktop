package kz.mybrain.superkassa.presentation.cabinet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts

/**
 * Чтение хозяйства компании: точки, кассы и блокировки.
 *
 * Отдельно от модели кабинета: чтение списков — три обращения, где беда
 * одного не отменяет остальных, и каждое кладёт прочитанное в общее
 * состояние окна по-своему.
 */
internal class CabinetLists(
    private val talk: Talk,
    private val useCases: CabinetCases,
    private val work: CabinetWork,
    private val screen: MutableStateFlow<CabinetUiState>
) {

    suspend fun readAll() {
        readPlaces()
        readRegisters()
        readBlocked()
    }

    /**
     * Перечитывает точки компании — все, а не первую страницу.
     *
     * Список выкладывается страницами по мере чтения: у сети их сорок, и ждать
     * последнюю, глядя в пустую колонку, владельцу незачем. Выкладывается,
     * пока прочитанное не короче показанного: перечитывание иначе сбрасывало
     * список до первой полусотни на глазах у владельца. Укоротившийся список
     * принимается в конце чтения — целиком, каким его отдал кабинет.
     *
     * @return удалось ли прочитать: по одному опустевшему списку колонка
     *   не отличает хозяйство без точек от молчащего кабинета.
     */
    suspend fun readPlaces(): Boolean {
        val reply = work.run("read places") {
            useCases.readPlaces { part, total ->
                screen.update { it.copy(placesTotal = total.toInt(), places = longer(part, it.places)) }
            }
        }
        val trouble = reply.problem?.let { cabinetMessage(it, cabinetTexts(talk.language())).words() }
        screen.update { now ->
            now.copy(places = reply.value ?: now.places, placesRead = true, placesTrouble = trouble)
        }
        return reply is CabinetReply.Done
    }

    /** Перечитывает кассы компании — тем же правилом, что и точки. */
    suspend fun readRegisters() {
        val all = work.run("read registers") {
            useCases.readRegisters { part, _ -> screen.update { it.copy(registers = longer(part, it.registers)) } }
        }.value ?: return
        screen.update { it.copy(registers = all) }
        adoptName(all)
    }

    /**
     * Перечитывает блокировки касс — молча: их владелец не запрашивал,
     * и отказ по ним не должен закрывать собой список точек.
     */
    suspend fun readBlocked() {
        val blocked = work.quiet("read blocked registers") { useCases.readBlocked() } ?: return
        screen.update { it.copy(blocked = blocked) }
    }

    /**
     * Название, данное кассе владельцем в кабинете, — кассе этой машины
     * (см. `NameKkmFromCabinet`). Отказ кассы чтению списка не мешает:
     * он уходит в журнал.
     */
    private suspend fun adoptName(registers: List<CabinetRegister>) {
        when (val answer = useCases.nameKkm(registers)) {
            null, is Answer.Done -> Unit
            is Answer.Refused -> talk.journal.warn("cabinet name not kept: refused ${answer.code}")
            is Answer.Failed -> talk.journal.warn("cabinet name not kept: ${answer.reason}")
        }
    }

    /** Ставит в список только что заведённую точку, если её там ещё нет. */
    fun placeAdded(place: RetailPlace) = screen.update { now ->
        if (now.places.any { it.id == place.id }) now else now.copy(places = now.places + place)
    }

    /**
     * Заменяет в списке одну перечитанную кассу, а только что заведённую —
     * ставит в него; без обращения к кабинету.
     */
    fun registerChanged(register: CabinetRegister) = screen.update { now ->
        val known = now.registers.any { it.id == register.id }
        val replaced = now.registers.map { if (it.id == register.id) register else it }
        now.copy(registers = if (known) replaced else now.registers + register)
    }

    /** Прочитанное выкладывается, только пока оно не короче показанного. */
    private fun <T> longer(part: List<T>, shown: List<T>): List<T> = if (part.size >= shown.size) part else shown
}
