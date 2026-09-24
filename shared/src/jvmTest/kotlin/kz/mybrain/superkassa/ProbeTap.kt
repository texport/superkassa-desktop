package kz.mybrain.superkassa

import androidx.compose.ui.geometry.Offset

/** Нажатие в середину первого узла, подходящего под условие. */
internal fun RenderProbe.tap(pick: (ProbeNode) -> Boolean) {
    val node = nodes().first(pick)
    click(node.at + Offset(node.width / 2f, node.height / 2f))
}
