package kz.mybrain.superkassa.presentation.cabinet.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.CashRegisterModel
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceRef
import kz.mybrain.superkassa.presentation.cabinet.company.CompanyScreen
import kz.mybrain.superkassa.refusal
import kz.mybrain.superkassa.shot
import kz.mybrain.superkassa.strings.api.Language
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Общее по кабинету: длинные названия, узкое окно и отказ вместо раздела.
 *
 * Длина здесь не выдумана: название модели в классификаторе КГД занимает
 * строку целиком, адрес точки — полторы, а своё название кассе владелец
 * даёт какое хочет. Узкое окно — то, в котором владелец держит кабинет
 * рядом с кассовым окном.
 */
class CabinetLookShots {

    private fun stage(company: StubReply) = CabinetStage { path ->
        if (path == "/api/company") company else StubReply("{}")
    }

    private val longRegister = CabinetRegister(
        id = "r-1",
        kkmId = 5_000_021,
        internalName = "Касса у входа в торговый зал, вторая справа от стеллажа с водой",
        status = "REGISTERED",
        registrationNumber = "000000010001",
        factoryNumber = "SN-ECC-1727584930112-REV-B",
        manufactureYear = 2026,
        model = CashRegisterModel(
            "0x0065000086cb",
            "«ПОРТ FPG-350 ФKZ» с фискальным накопителем и встроенным термопринтером"
        ),
        retailPlace = RetailPlaceRef(
            "p-1",
            "Магазин «Продукты у дома» на пересечении Абая и Розыбакиева, помещение 14Б"
        )
    )

    @Composable
    private fun Passport(stage: CabinetStage, register: CabinetRegister) {
        stage.Window {
            val model = registerViewModel(stage.cabinet.cabinet)
            val view = RegisterView(register, register, RegisterUiState(card = register))
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
                RegisterPassport(stage.cabinet, stage.texts, view, model)
            }
        }
    }

    /**
     * Длинные названия и узкая колонка.
     *
     * Проверяется не то, что они помещаются, — они не помещаются, — а то,
     * что строка обрезается по краю карточки, а не уезжает за него и
     * не наползает на плашку состояния справа.
     */
    @Test
    fun `длинные названия и узкая колонка не рвут паспорт кассы`() {
        val first = stage(StubReply("{}"))
        val wide = shot("look-long-wide", width = WIDE, height = TALL) { Passport(first, longRegister) }
        val second = stage(StubReply("{}"))
        val narrow = shot("look-long-narrow", width = NARROW, height = TALL) { Passport(second, longRegister) }

        assertTrue(wide.isNotEmpty() && narrow.isNotEmpty())
        assertTrue(!wide.contentEquals(narrow), "узкая колонка раскладывается так же, как широкая")
    }

    /**
     * Кабинет ответил, что такого раздела не знает.
     *
     * Так выглядит невыложенная служба: 404 на карточку компании.
     * Раздел обязан сказать об этом, а не остаться с кружком ожидания
     * без конца — по снимку видно, что именно стоит на месте реквизитов.
     */
    @Test
    fun `отказ кабинета вместо раздела компании`() {
        val absent = stage(refusal("NOT_FOUND", "No handler for GET /api/company", HttpStatusCode.NotFound))
        val notDeployed = shot("look-company-404", width = WIDE, height = TALL) { Company(absent) }
        val failing = stage(refusal("INTERNAL", "Unexpected error", HttpStatusCode.InternalServerError))
        val broken = shot("look-company-500", width = WIDE, height = TALL) { Company(failing) }

        assertTrue(notDeployed.isNotEmpty() && broken.isNotEmpty())
    }

    @Composable
    private fun Company(stage: CabinetStage) = stage.Window {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
            CompanyScreen(stage.cabinet.cabinet, Language.Ru, stage.texts)
        }
    }

    private companion object {
        /** Колонка карточки кассы в рабочем месте владельца и самая узкая из его окон. */
        const val WIDE = 760
        const val NARROW = 420
        const val TALL = 820
    }
}
