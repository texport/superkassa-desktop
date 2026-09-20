package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.Oked
import kz.mybrain.superkassa.desktop.server.cabinet.OkedEntry
import kz.mybrain.superkassa.desktop.server.cabinet.okedByCode

/**
 * Наименование вида деятельности на языке владельца.
 *
 * Компания хранит одно наименование — то, на каком языке вид завели,
 * и казахский экран показывал из-за этого русскую строку классификатора.
 * Наименование берётся по коду из классификатора и переводится при показе;
 * сохранённое не трогается: в заявление уходит то, что приняла ИСНА.
 *
 * Классификатор спрашивается по разу на код и запоминается: список видов
 * перечитывается после каждого сохранения, а сам классификатор не меняется.
 */
@Composable
internal fun rememberOkedTitles(
    session: Session,
    cabinet: CabinetSession,
    okeds: List<Oked>
): (Oked) -> String {
    val known = remember { mutableStateMapOf<String, OkedEntry>() }
    val codes = okeds.map { it.code }
    LaunchedEffect(codes, cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        codes.filterNot { known.containsKey(it) }.forEach { code ->
            cabinet.quiet(NAMING) { cabinet.client.okedByCode(token, code) }
                ?.let { known[code] = it }
        }
    }
    return { oked -> known[oked.code]?.let { titleOf(session, it) } ?: saved(oked) }
}

/** Сохранённое наименование, а при пустом — код: строка без имени нечитаема. */
private fun saved(oked: Oked): String = oked.name?.takeIf { it.isNotBlank() } ?: oked.code

private const val NAMING = "наименование вида деятельности"
