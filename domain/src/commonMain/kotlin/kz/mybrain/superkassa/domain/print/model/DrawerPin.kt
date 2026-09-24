package kz.mybrain.superkassa.domain.print.model

import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Пин, которым касса [kkmId] рисует форму.
 *
 * Пин вошедшего кассира берётся у входа и через экраны не проходит; без
 * входа форму рисуют пином, который владелец ввёл ради неё.
 *
 * @param entered пин, введённый ради формы; `null` — не вводили.
 * @return пин; `null` — его нет, и его надо спросить.
 */
internal fun SignedKkm.drawerPin(kkmId: String, entered: String?): String? =
    seat()?.takeIf { it.kkmId == kkmId }?.pin ?: entered?.takeIf { it.isNotBlank() }

/** Пин кассы [kkmId] надо спросить у владельца: кассир за ней не сидит. */
internal fun SignedKkm.asksPin(kkmId: String): Boolean = seat()?.kkmId != kkmId
