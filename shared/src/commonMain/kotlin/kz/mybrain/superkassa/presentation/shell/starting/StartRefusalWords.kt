package kz.mybrain.superkassa.presentation.shell.starting

import kz.mybrain.superkassa.domain.kassa.model.StartRefusal
import kz.mybrain.superkassa.strings.api.shell.StartTexts
import kz.mybrain.superkassa.strings.api.shell.StartWords

/** Что случилось и что делать — по причине. */
fun StartTexts.of(refusal: StartRefusal): StartWords = when (refusal) {
    StartRefusal.NodeRunning -> nodeRunning
    StartRefusal.KassaRunning -> kassaRunning
    StartRefusal.BothDatabases -> bothDatabases
    StartRefusal.NodeDataUnfit -> nodeDataUnfit
    StartRefusal.KassaNotOpened -> kassaNotOpened
}
