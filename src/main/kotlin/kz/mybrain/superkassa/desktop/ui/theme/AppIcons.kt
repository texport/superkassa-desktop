package kz.mybrain.superkassa.desktop.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Значки кассы.
 *
 * Значок заводится здесь один раз и берётся по смыслу действия, а не по
 * имени в наборе Material: если корзину когда-нибудь заменят на другую
 * картинку, менять придётся одну строку, а не каждый экран, где она
 * встретилась.
 */
object AppIcons {
    val dashboard: ImageVector = Icons.Filled.Dashboard
    val sale: ImageVector = Icons.Filled.ShoppingCart
    val returns: ImageVector = Icons.AutoMirrored.Filled.Undo
    val cash: ImageVector = Icons.Filled.AccountBalanceWallet
    val history: ImageVector = Icons.Filled.History
    val queue: ImageVector = Icons.Filled.QueuePlayNext
    val users: ImageVector = Icons.Filled.People
    val settings: ImageVector = Icons.Filled.Settings

    /** Перечитать состояние кассы. */
    val refresh: ImageVector = Icons.Filled.Refresh

    /** Печать документа. */
    val print: ImageVector = Icons.Filled.Print

    /** Сама касса: в списке выбора и в пустом состоянии входа. */
    val kkm: ImageVector = Icons.Filled.PointOfSale

    /** Язык интерфейса. */
    val language: ImageVector = Icons.Filled.Language

    /** Отметка выбранного значения в меню. */
    val chosen: ImageVector = Icons.Filled.Check

    /** Свернуть и развернуть рельс разделов. */
    val menu: ImageVector = Icons.Filled.Menu

    /** Просмотр печатной формы на экране. */
    val preview: ImageVector = Icons.Outlined.Visibility

    /** Сохранение печатной формы в файл. */
    val save: ImageVector = Icons.Filled.Download

    /** Увеличить и уменьшить ленту в просмотре. */
    val zoomIn: ImageVector = Icons.Filled.ZoomIn
    val zoomOut: ImageVector = Icons.Filled.ZoomOut

    /** Закрыть окно просмотра. */
    val close: ImageVector = Icons.Filled.Close

    /** Объяснение раздела в подсказке. */
    val info: ImageVector = Icons.Outlined.Info

    /** Заведение новой кассы. */
    val newKkm: ImageVector = Icons.Filled.AddBusiness

    /** Возврат на предыдущий экран. */
    val back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack

    /** Развернуть свёрнутую часть карточки. */
    val expand: ImageVector = Icons.Filled.ExpandMore

    /** Свернуть развёрнутую часть карточки. */
    val collapse: ImageVector = Icons.Filled.ExpandLess

    /** Добавить строку в набор: ещё одну оплату чека. */
    val add: ImageVector = Icons.Filled.Add

    /** Личный кабинет ОФД: дела владельца, а не кассира. */
    val cabinet: ImageVector = Icons.Filled.Business

    /** Место на карте: где стоит торговая точка. */
    val place: ImageVector = Icons.Filled.Place
}
