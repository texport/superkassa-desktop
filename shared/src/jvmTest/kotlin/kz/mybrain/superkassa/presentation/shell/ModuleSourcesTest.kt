package kz.mybrain.superkassa.presentation.shell

import kz.mybrain.superkassa.ScreenModuleRules

/**
 * Устройство каркаса по исходникам: только каркас окна, без кода областей.
 *
 * Каркас видит все области экранов, общее экранов, дизайн-систему, тексты
 * и домен; слой данных ему не виден — адаптеры собирают точки сборки
 * платформенных приложений. Файл области, положенный сюда, обходил бы
 * границу её модуля, и эта проверка его не пропускает.
 */
class ModuleSourcesTest : ScreenModuleRules(
    own = listOf("presentation.shell"),
    allowed = listOf("presentation", "navigation", "domain", "designsystem", "strings.api")
)
