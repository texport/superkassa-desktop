package kz.mybrain.superkassa.domain.signin.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer

/**
 * Из чего кассир выбирает кассу на входе.
 *
 * @property answer что касса ответила на список: отказ объявляет модель
 *   входа, а прочитанный прежде список остаётся в [kkms].
 * @property kkms кассы рабочего места — новые или прочитанные прежде.
 * @property localNames свои названия касс на этой машине, по номеру кассы.
 * @property rememberedId касса, за которой работали в прошлый раз.
 */
data class KkmChoice(
    val answer: Answer<List<KkmResponse>>,
    val kkms: List<KkmResponse>,
    val localNames: Map<String, String>,
    val rememberedId: String?
)
