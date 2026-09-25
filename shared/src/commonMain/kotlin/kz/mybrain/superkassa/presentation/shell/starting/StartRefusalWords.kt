package kz.mybrain.superkassa.presentation.shell.starting

import kz.mybrain.superkassa.domain.kassa.model.StartRefusal
import kz.mybrain.superkassa.strings.api.shell.ShellTexts
import kz.mybrain.superkassa.strings.api.shell.StartFailureTexts

/** Что случилось и что делать — по причине. */
internal fun ShellTexts.of(refusal: StartRefusal): StartFailureTexts = when (refusal) {
    StartRefusal.NodeRunning -> nodeRunning
    StartRefusal.KassaRunning -> kassaRunning
    StartRefusal.BothDatabases -> bothDatabases
    StartRefusal.NodeDataUnfit -> nodeDataUnfit
    StartRefusal.KassaNotOpened -> kassaNotOpened
}
