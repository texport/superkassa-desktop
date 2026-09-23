package kz.mybrain.superkassa.data.node

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse

/**
 * Касса процесса глазами разделов, которые ещё говорят с узлом.
 *
 * Временный мост на время перевода: вход и главный экран работают с ядром,
 * а шапка, продажа, журнал и печать пока читают сеанс в типах узла. Узел
 * отдавал те же поля под теми же именами — он и был этим ядром за сетью, —
 * поэтому перевод идёт разбором, а не поле за полем. Уходит вместе с `data/node`.
 */
object CoreBridge {

    fun kkm(kkm: KkmResponse): Kkm = recast(ServerClient.lenientJson.encodeToString(KkmResponse.serializer(), kkm))

    fun document(document: FiscalDocumentResponse): Document =
        recast(ServerClient.lenientJson.encodeToString(FiscalDocumentResponse.serializer(), document))

    fun user(user: UserResponse): KkmUser = KkmUser(userId = user.userId, name = user.name, role = user.role.name)

    private inline fun <reified T> recast(json: String): T = ServerClient.lenientJson.decodeFromString(json)
}
