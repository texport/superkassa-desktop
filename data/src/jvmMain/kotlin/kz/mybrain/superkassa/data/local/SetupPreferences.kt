package kz.mybrain.superkassa.data.local

import kotlinx.io.files.Path

/**
 * Пройденное в мастере подключения кассы.
 *
 * Отдельный предмет, потому что живёт не так, как прочие настройки:
 * мастер проходят один раз, его значения перестают что-либо значить
 * после подключения, и имена у них появляются вместе с новыми шагами —
 * поимённого перечня здесь нет и быть не должно.
 *
 * По файлу на значение — как и остальные настройки рабочего места:
 * разбирать один файл со своим форматом ради четырёх строк дороже,
 * чем хранить их порознь.
 */
class SetupPreferences(private val directory: Path) {

    fun value(name: String): String? = readSetting(setupFile(name))

    fun remember(name: String, value: String?) = writeSetting(setupFile(name), value)

    private fun setupFile(name: String) = Path(directory, "setup", name)
}
