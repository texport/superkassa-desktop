package kz.mybrain.superkassa.presentation.print

import kz.mybrain.superkassa.ScreenModuleRules

/**
 * Устройство области по исходникам: только её пакеты и ни одной соседней области.
 *
 * Область видит общее экранов, дизайн-систему, тексты и домен; чужую
 * область домена — только объявленную в правилах областей.
 */
class ModuleSourcesTest : ScreenModuleRules(
    own = listOf("presentation.print"),
    allowed = listOf("presentation.common", "presentation.words", "domain", "designsystem", "strings.api")
)
