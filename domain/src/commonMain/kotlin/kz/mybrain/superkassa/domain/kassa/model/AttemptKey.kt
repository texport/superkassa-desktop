package kz.mybrain.superkassa.domain.kassa.model

import kotlin.uuid.Uuid

/**
 * Ключ повтора одной фискальной попытки.
 *
 * Касса отличает повтор от новой операции только по нему: повтор после
 * неизвестного исхода с тем же ключом она отвечает прежним документом
 * и второго не проводит. Поэтому ключ заводится при попытке и живёт с ней,
 * а не создаётся на каждое нажатие.
 *
 * @param kind вид операции в ключе — чек, деньги, возврат: по нему ключ
 *   узнаётся в журнале кассы.
 */
fun attemptKey(kind: String? = null): String =
    listOfNotNull(KEY_PREFIX, kind, Uuid.random().toString()).joinToString("-")

private const val KEY_PREFIX = "desktop"
