package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.CashRegisterModel
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlaceRef
import kz.mybrain.superkassa.desktop.ui.cabinet.CompanyPage
import kz.mybrain.superkassa.desktop.ui.cabinet.RegisterPassport
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
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

    private fun stage(company: CabinetReply) = CabinetStage { path ->
        if (path == "/api/company") company else CabinetReply("{}")
    }

    private val longRegister = CabinetRegister(
        id = "r-1",
        kkmId = 5_000_021,
        internalName = "Касса у входа в торговый зал, вторая справа от стеллажа с водой",
        status = "REGISTERED",
        registrationNumber = "000000010001",
        factoryNumber = "SN-ECC-1727584930112-REV-B",
        manufactureYear = 2026,
        modelView = CashRegisterModel(
            "0x0065000086cb",
            "«ПОРТ FPG-350 ФKZ» с фискальным накопителем и встроенным термопринтером"
        ),
        retailPlaceView = RetailPlaceRef(
            "p-1",
            "Магазин «Продукты у дома» на пересечении Абая и Розыбакиева, помещение 14Б"
        )
    )

    @Composable
    private fun Passport(stage: CabinetStage, register: CabinetRegister) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
            RegisterPassport(stage.session, stage.cabinet, stage.texts, register, null) {}
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
        val wide = shot("look-long-wide", width = WIDE, height = TALL) {
            Passport(stage(CabinetReply("{}")), longRegister)
        }
        val narrow = shot("look-long-narrow", width = NARROW, height = TALL) {
            Passport(stage(CabinetReply("{}")), longRegister)
        }

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
        val notDeployed = shot("look-company-404", width = WIDE, height = TALL) {
            val stage = stage(refusal("NOT_FOUND", "No handler for GET /api/company", HttpStatusCode.NotFound))
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                CompanyPage(stage.session, stage.cabinet, stage.texts)
            }
        }
        val broken = shot("look-company-500", width = WIDE, height = TALL) {
            val stage = stage(refusal("INTERNAL", "Unexpected error", HttpStatusCode.InternalServerError))
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                CompanyPage(stage.session, stage.cabinet, stage.texts)
            }
        }

        assertTrue(notDeployed.isNotEmpty() && broken.isNotEmpty())
    }

    private companion object {
        /** Колонка карточки кассы в рабочем месте владельца и самая узкая из его окон. */
        const val WIDE = 760
        const val NARROW = 420
        const val TALL = 820
    }
}
