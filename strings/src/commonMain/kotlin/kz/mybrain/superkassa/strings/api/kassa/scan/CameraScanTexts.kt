package kz.mybrain.superkassa.strings.api.kassa.scan

/**
 * Надписи сканера штрихкода камерой устройства.
 *
 * Разрешение на камеру объясняется по месту, до системного вопроса:
 * кассир должен знать, зачем кассе камера и что со снимками не делается.
 */
data class CameraScanTexts(
    /** Значок камеры в поле штрихкода. */
    val scan: String,
    val title: String,
    val close: String,
    /** Под видоискателем: что делать и что код найдётся сам. */
    val hint: String,
    /** Зачем кассе камера — до системного вопроса о разрешении. */
    val why: String,
    val allow: String,
    /** В разрешении отказано насовсем: где его включить. */
    val denied: String,
    val settings: String
)
