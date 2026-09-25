package kz.mybrain.superkassa.presentation.settings.ofd

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.settings.usecase.ReadNextRequest
import kz.mybrain.superkassa.domain.settings.usecase.ReadOfdInfo
import kz.mybrain.superkassa.domain.settings.usecase.ReplaceOfdToken
import kz.mybrain.superkassa.domain.settings.usecase.SyncWithBfd
import kz.mybrain.superkassa.domain.shift.usecase.CheckOfdLink
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn

/** Сценарии связи кассы с БФД. */
class OfdCases(kassa: Kassa, signIn: SignIn) {
    val observe = ObserveSignIn(signIn)
    val sync = SyncWithBfd(kassa, signIn)
    val token = ReplaceOfdToken(kassa, signIn)
    val link = CheckOfdLink(kassa, signIn)
    val info = ReadOfdInfo(kassa, signIn)
    val nextRequest = ReadNextRequest(kassa, signIn)
}
