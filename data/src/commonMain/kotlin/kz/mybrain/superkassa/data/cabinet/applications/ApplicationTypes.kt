package kz.mybrain.superkassa.data.cabinet.applications

import kz.mybrain.superkassa.data.cabinet.toCabinet
import kz.mybrain.superkassa.domain.cabinet.model.CabinetApplication
import kz.mybrain.superkassa.domain.cabinet.model.documents.NewRetailPlace
import kz.mybrain.superkassa.domain.cabinet.model.documents.ReregistrationRequest
import kz.mybrain.superkassa.integrations.bfdcabinet.applications.CabinetApplication as BfdApplication
import kz.mybrain.superkassa.integrations.bfdcabinet.applications.DeregistrationRequest as BfdDeregistration
import kz.mybrain.superkassa.integrations.bfdcabinet.applications.NewRetailPlace as BfdNewPlace
import kz.mybrain.superkassa.integrations.bfdcabinet.applications.ReregistrationRequest as BfdReregistration

/** Заявление предметной области — тем, что уходит в кабинет. */
internal fun CabinetApplication.sent(): BfdApplication = when (this) {
    is CabinetApplication.Registration -> BfdApplication.Registration(registerId)
    is CabinetApplication.Reregistration -> BfdApplication.Reregistration(registerId, request.sent())
    is CabinetApplication.Deregistration ->
        BfdApplication.Deregistration(registerId, BfdDeregistration(request.reason, request.comment))
}

private fun ReregistrationRequest.sent() = BfdReregistration(newRetailPlaceId, newRetailPlace?.sent(), reason)

private fun NewRetailPlace.sent() = BfdNewPlace(name, addressRef, latitude?.toCabinet(), longitude?.toCabinet())
