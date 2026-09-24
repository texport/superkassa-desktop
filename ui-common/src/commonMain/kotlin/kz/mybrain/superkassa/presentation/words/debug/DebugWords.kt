package kz.mybrain.superkassa.presentation.words.debug

import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kz.mybrain.superkassa.strings.api.debug.DebugTexts

/** Как называется уровень записи на языке читающего. */
fun DebugTexts.name(level: LogLevel): String = when (level) {
    LogLevel.Debug -> levelDebug
    LogLevel.Info -> levelInfo
    LogLevel.Warning -> levelWarning
    LogLevel.Failure -> levelFailure
}

/** Как называется источник записи на языке читающего. */
fun DebugTexts.name(source: LogSource): String = when (source) {
    LogSource.Cabinet -> sourceCabinet
    LogSource.Machine -> sourceMachine
    LogSource.Signature -> sourceSignature
    LogSource.App -> sourceApp
}
