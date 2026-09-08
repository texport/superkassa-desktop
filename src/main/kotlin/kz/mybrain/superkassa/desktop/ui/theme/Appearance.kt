package kz.mybrain.superkassa.desktop.ui.theme

/**
 * Светлая или тёмная касса.
 *
 * Выбор свой, а не только системный: касса стоит на общей машине, за
 * которой сменяются кассиры, и лезть в настройки операционной системы
 * ради читаемости экрана им нельзя. Зал бывает и залитым солнцем,
 * и тёмным — и это разные требования к экрану на одной и той же машине.
 */
enum class Appearance(val code: String) {

    /** Как в системе — обычный случай. */
    System("system"),

    /** Светлый зал. */
    Light("light"),

    /** Тёмный зал и ночная смена. */
    Dark("dark");

    companion object {
        fun byCode(code: String?): Appearance = entries.firstOrNull { it.code == code } ?: System
    }
}
