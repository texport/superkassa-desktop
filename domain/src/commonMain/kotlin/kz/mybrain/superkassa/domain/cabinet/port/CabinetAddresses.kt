package kz.mybrain.superkassa.domain.cabinet.port

import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress

/** Адресный регистр: адрес собирается шагами — регион, пункты, улица, дом. */
interface CabinetAddresses {
    suspend fun regions(query: String): List<AddressSuggestion>

    suspend fun localities(parentId: Long, query: String): List<AddressSuggestion>

    /** Есть ли под пунктом вложенные пункты: у Караганды под городом лежат районы. */
    suspend fun nested(localityId: Long): List<AddressSuggestion>

    suspend fun streets(localityId: Long, query: String): List<AddressSuggestion>

    suspend fun buildings(streetId: Long, number: String): List<AddressSuggestion>

    /** Адрес, подтверждённый регистром по коду РКА. */
    suspend fun resolve(rka: String): RegisterAddress
}
