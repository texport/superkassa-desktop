package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.ui.cabinet.CompanyPage
import kz.mybrain.superkassa.desktop.ui.cabinet.OkedPicker
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки раздела «Компания»: реквизиты и виды деятельности.
 *
 * Реквизиты компании владелец не правит — их выдал КГД, — а виды
 * деятельности ведёт сам, и от основного зависит, что уйдёт
 * в регистрационное заявление. Смотрят здесь на три вещи: видно ли,
 * какой вид основной; объясняет ли себя пустой список; и остаётся ли
 * на экране причина, по которой сохранение не удалось.
 */
class CabinetCompanyShots {

    private fun stage(okeds: String) = CabinetStage { path ->
        when {
            path == "/api/company" -> CabinetReply(profile(okeds))
            path.startsWith("/api/reference/okeds/") -> classifier(path.substringAfterLast('/'))
            else -> CabinetReply("{}")
        }
    }

    private fun profile(okeds: String) =
        """{"id":"c-1","bin":"230140000000","name":"ТОО «Азик и Ко»","okeds":[$okeds]}"""

    /**
     * Позиция классификатора по коду — с наименованием той длины, какая
     * в нём и записана: формулировки ОКЭД занимают целую строку, и именно
     * на них проверяется, не обрезана ли строка списка.
     */
    private fun classifier(code: String) = CabinetReply(
        """{"code":"$code","name":"${CLASSIFIER[code] ?: code}","nameKz":"Бөлшек сауда","level":"CLASS"}"""
    )

    private fun page(name: String, okeds: String, problem: CabinetProblem? = null): ByteArray {
        val stage = stage(okeds)
        return shot(name) {
            val body = @androidx.compose.runtime.Composable {
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                    CompanyPage(stage.session, stage.cabinet, stage.texts)
                }
            }
            if (problem == null) body() else WithCabinetMessage(problem) { body() }
        }
    }

    @Test
    fun `реквизиты пришли, и виды деятельности читаются с одним основным`() {
        val many = page(
            "company-okeds",
            """{"code":"47.11","name":"Розничная торговля продуктами питания","primary":true},
               {"code":"56.10","name":"Деятельность ресторанов и предоставление услуг по доставке продуктов питания",
                "primary":false},
               {"code":"47.73","name":"Розничная торговля фармацевтическими товарами","primary":false}"""
        )
        val single = page("company-oked-single", """{"code":"47.11","name":"Розничная торговля","primary":true}""")

        assertTrue(many.isNotEmpty() && single.isNotEmpty())
        assertTrue(!many.contentEquals(single), "список из трёх видов и список из одного выглядят одинаково")
    }

    @Test
    fun `компания без видов деятельности объясняет пустоту`() {
        val empty = page("company-okeds-empty", "")
        assertTrue(empty.isNotEmpty())
    }

    /**
     * Отказ сохранения остаётся на экране, а список — набранным владельцем.
     *
     * Кабинет отвергает набор без основного вида и набор с неизвестным
     * кодом; владелец обязан увидеть причину и не потерять при этом
     * то, что успел собрать.
     */
    @Test
    fun `отказ кабинета при сохранении видов деятельности виден владельцу`() {
        val refused = page(
            "company-okeds-refused",
            """{"code":"47.11","name":"Розничная торговля продуктами питания","primary":false}""",
            CabinetProblem.Refused("PRIMARY_OKED_REQUIRED", "Exactly one primary OKED is required")
        )
        assertTrue(refused.isNotEmpty())
    }

    /**
     * Поиск в классификаторе, который ничего не нашёл.
     *
     * Состояние воспроизводится только набором: пустая строка отдаёт
     * начало классификатора, и подсказка под полем в этом случае другая.
     */
    @Test
    fun `вид деятельности не найден поиском`() {
        val stage = CabinetStage { path ->
            if (path.startsWith("/api/reference/okeds")) CabinetReply("""{"items":[],"total":0}""")
            else CabinetReply("{}")
        }
        RenderProbe(width = PICKER_WIDTH, height = PICKER_HEIGHT, content = {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                OkedPicker(stage.session, stage.cabinet, stage.texts, known = emptyList()) {}
            }
        }).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(FIELD_X, FIELD_Y))
            probe.type("аптека")
            var frame = probe.frame()
            repeat(SETTLE) { frame = probe.frame() }
            File("/tmp/cabinet-company-oked-not-found.png").writeBytes(frame)
            assertTrue(frame.isNotEmpty())
        }
    }

    private companion object {
        /** Формулировки ОКЭД как они записаны в НК РК 03-2019. */
        val CLASSIFIER = mapOf(
            "47.11" to "Розничная торговля в неспециализированных магазинах преимущественно " +
                "продуктами питания, напитками и табачными изделиями",
            "56.10" to "Деятельность ресторанов и предоставление услуг по доставке продуктов питания",
            "47.73" to "Розничная торговля фармацевтическими товарами в специализированных магазинах"
        )

        const val PICKER_WIDTH = 760
        const val PICKER_HEIGHT = 240
        const val FIELD_X = 200f
        const val FIELD_Y = 45f
        const val SETTLE = 30
    }
}
