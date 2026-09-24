package kz.mybrain.superkassa.strings.impl.kassa

import kz.mybrain.superkassa.strings.api.kassa.BlockReasonTexts

/** Причина блокировки по коду узла; незнакомый код и `null` — общая причина. */
internal fun blockReasonWords(texts: BlockReasonTexts, code: Int?): String = when (code) {
    INVALID_TOKEN -> texts.invalidToken
    SHIFT_TOO_LONG -> texts.shiftTooLong
    AUTONOMOUS_TOO_LONG -> texts.autonomousTooLong
    DEREGISTERED -> texts.deregistered
    DISCONNECTED -> texts.disconnected
    INCORRECT_DATA -> texts.incorrectData
    else -> texts.unknown
}

/** Отказ БФД, перенесённый узлом в свой диапазон: код протокола плюс тысяча. */
private const val INCORRECT_DATA = 1013
private const val INVALID_TOKEN = 1002
private const val DEREGISTERED = 1018
private const val DISCONNECTED = 1019

/** Свои причины узла. */
private const val SHIFT_TOO_LONG = 1011
private const val AUTONOMOUS_TOO_LONG = 2001
