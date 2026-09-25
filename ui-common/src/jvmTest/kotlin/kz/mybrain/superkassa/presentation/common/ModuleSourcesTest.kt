package kz.mybrain.superkassa.presentation.common

import kz.mybrain.superkassa.ScreenModuleRules

/**
 * Устройство общего экранов по исходникам: только общие полки и ни одной области.
 *
 * Общее знает домен, дизайн-систему и тексты, но не видит ни одной
 * области приложения: области берут общее отсюда, а не наоборот.
 */
class ModuleSourcesTest : ScreenModuleRules(
    own = listOf("presentation.common", "presentation.words"),
    allowed = listOf("navigation", "domain", "designsystem", "strings.api")
)
