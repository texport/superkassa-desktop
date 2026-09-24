package kz.mybrain.superkassa.presentation.analytics.common

import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.presentation.strings.analytics.AnalyticsTexts

/** Заголовок помехи словами владельца. */
fun troubleTitle(trouble: AnalyticsTrouble, texts: AnalyticsTexts): String = when (trouble) {
    AnalyticsTrouble.NotDeployed -> texts.notDeployed
    AnalyticsTrouble.Unreachable -> texts.unreachable
    AnalyticsTrouble.SignedOut -> texts.signedOut
    is AnalyticsTrouble.Refused -> texts.refused
}

/** Что делать с помехой: объяснение под заголовком. */
fun troubleHint(trouble: AnalyticsTrouble, texts: AnalyticsTexts): String = when (trouble) {
    AnalyticsTrouble.NotDeployed -> texts.notDeployedHint
    AnalyticsTrouble.Unreachable -> texts.unreachableHint
    AnalyticsTrouble.SignedOut -> texts.signedOutHint
    is AnalyticsTrouble.Refused -> trouble.text
}
