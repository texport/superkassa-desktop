package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.ui.theme.Accent
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.Look
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
import kz.mybrain.superkassa.desktop.ui.theme.Typeface

/**
 * Вид рабочего места глазами сеанса: оформление, шрифт, свёрнутые части
 * и размер окна.
 *
 * Отдельно от [Session]: сеанс отвечает за кассу, кассира и узел, а вид
 * окна — за рабочее место, и с каждой новой настройкой оформления сеанс
 * рос на две строки, пока не перестал умещаться в один экран.
 */
val Session.appearance: Appearance get() = settings.appearance
val Session.look: Look get() = settings.look
val Session.railCollapsed: Boolean get() = settings.railCollapsed
val Session.placesCollapsed: Boolean get() = settings.placesCollapsed
val Session.rememberedWindowSize: Pair<Int, Int>? get() = settings.windowSize

fun Session.switchAppearance(chosen: Appearance) = settings.switchAppearance(chosen)
fun Session.chooseAccent(chosen: Accent) = settings.chooseAccent(chosen)
fun Session.chooseTypeface(chosen: Typeface) = settings.chooseTypeface(chosen)
fun Session.chooseTextScale(chosen: TextScale) = settings.chooseTextScale(chosen)
fun Session.toggleRail() = settings.toggleRail()
fun Session.togglePlaces() = settings.togglePlaces()
fun Session.rememberWindowSize(width: Int, height: Int) = settings.rememberWindowSize(width, height)
