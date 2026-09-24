package kz.mybrain.superkassa.presentation.kassa.refund.component

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.document.model.number
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.domain.kassa.model.refund.ReturnKind
import kz.mybrain.superkassa.presentation.common.dialog.ConfirmActionDialog
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.format.fill
import kz.mybrain.superkassa.presentation.kassa.refund.RefundActions
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.action
import kz.mybrain.superkassa.presentation.strings.kassa.checkout.checkoutTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs

/**
 * Вопрос перед возвратом: сколько и по какому чеку.
 *
 * Внесение и изъятие денег спрашивают сумму вслух, а возврат уходил
 * по первому нажатию — хотя он так же выдаёт деньги из ящика и так же
 * не отменяется. Кнопка вопроса — та же, что на экране: «Вернуть».
 */
@Composable
internal fun RefundConfirm(kind: ReturnKind, draft: RefundDraft, busy: Boolean, actions: RefundActions) {
    val texts = checkoutTexts(LocalLanguage.current)
    val sum = Money.formatTiyn(draft.readyTiyn)
    val question = if (kind == ReturnKind.Sell) texts.confirmGive else texts.confirmTake
    val number = draft.basis.number?.toString() ?: Glyphs.DASH
    ConfirmActionDialog(
        icon = AppIcons.returns,
        what = question.fill(sum),
        explain = texts.confirmBasis.fill(number, Money.formatTiyn(draft.total)),
        action = kind.action(LocalStrings.current.returns),
        cancel = texts.confirmCancel,
        busy = busy,
        onCancel = actions::cancel,
        onConfirm = actions::confirm
    )
}
