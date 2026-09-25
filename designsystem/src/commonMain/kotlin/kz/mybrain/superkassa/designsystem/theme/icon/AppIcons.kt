package kz.mybrain.superkassa.designsystem.theme.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.ViewSidebar
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
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

    /** Меню действий, которым не хватило места в шапке. */
    val moreActions: ImageVector = Icons.Filled.MoreVert

    /** Открыть разделы окна: на телефоне — из шапки, шире — из шапки рельса. */
    val menu: ImageVector = Icons.Filled.Menu

    /** Закрыть разделы, открытые поверх окна. */
    val menuOpen: ImageVector = Icons.AutoMirrored.Filled.MenuOpen

    /** Просмотр печатной формы на экране. */
    val preview: ImageVector = Icons.Outlined.Visibility

    /** Сохранение печатной формы в файл. */
    val save: ImageVector = Icons.Filled.Download

    /** Увеличить и уменьшить ленту в просмотре. */
    val zoomIn: ImageVector = Icons.Filled.ZoomIn
    val zoomOut: ImageVector = Icons.Filled.ZoomOut

    /** Карта во всё окно и обратно. */
    val fullscreen: ImageVector = Icons.Filled.Fullscreen
    val fullscreenExit: ImageVector = Icons.Filled.FullscreenExit

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

    /** Убрать вспомогательную панель сбоку: она уходит вправо до сводки. */
    val sidePanelHide: ImageVector = Icons.Filled.ChevronRight

    /** Вернуть вспомогательную панель сбоку. */
    val sidePanelShow: ImageVector = Icons.Filled.ChevronLeft

    /** Свернуть нижний лист до сводки. */
    val bottomSheetHide: ImageVector = Icons.Filled.ExpandMore

    /** Развернуть нижний лист из сводки. */
    val bottomSheetShow: ImageVector = Icons.Filled.ExpandLess

    /** Перелистывание дня в журнале. */
    val earlierDay: ImageVector = Icons.Filled.ChevronLeft
    val laterDay: ImageVector = Icons.Filled.ChevronRight
    val today: ImageVector = Icons.Filled.Today

    /**
     * Порядок строк журнала: от меньшего к большему и обратно.
     *
     * Своя пара, а не значки внесения и изъятия денег: те говорят
     * о движении денег в ящике, и одолженные журналу читались бы как
     * приход и расход, а не как порядок сортировки.
     */
    val ascending: ImageVector = Icons.Filled.ArrowUpward
    val descending: ImageVector = Icons.Filled.ArrowDownward

    /** Пустые состояния: нечего показать, нечего вернуть, нечего отправлять. */
    val noDocuments: ImageVector = Icons.Filled.EventBusy
    val noBasis: ImageVector = Icons.AutoMirrored.Filled.ReceiptLong
    val queueClear: ImageVector = Icons.Filled.CloudDone

    /** Кассиры этой кассы. */
    val cashiers: ImageVector = Icons.Filled.Groups

    /** Место на карте: где стоит торговая точка. */
    val place: ImageVector = Icons.Filled.Place

    /** Где сейчас рабочее место: перевести карту в свой город. */
    val myLocation: ImageVector = Icons.Filled.MyLocation

    /** Найти товар по штрихкоду. */
    val find: ImageVector = Icons.Outlined.Search

    /** Пустая корзина: в чеке ещё ничего не набрано. */
    val emptyBasket: ImageVector = Icons.Outlined.ShoppingCart

    /** Сторно: отмена уже пробитой строки. */
    val storno: ImageVector = Icons.AutoMirrored.Outlined.Undo

    /** Убрать строку или запись до того, как она ушла в ОФД. */
    val remove: ImageVector = Icons.Outlined.Delete

    /** Предупреждение перед необратимым. */
    val warning: ImageVector = Icons.Outlined.WarningAmber

    /** Денежный ящик: внесение и изъятие. */
    val drawer: ImageVector = Icons.Outlined.AccountBalanceWallet

    /** Деньги внесены в ящик. */
    /** Закрытие смены: итог дня, а не удаление. */
    val shiftClose: ImageVector = Icons.Outlined.EventAvailable

    val paidIn: ImageVector = Icons.Outlined.ArrowDownward

    /** Деньги изъяты из ящика. */
    val paidOut: ImageVector = Icons.Outlined.ArrowUpward

    /** Пин кассира. */
    val pin: ImageVector = Icons.Outlined.Password

    /** Акцизная марка на позиции чека. */
    val excise: ImageVector = Icons.Outlined.QrCodeScanner

    /** Поле штрихкода: включить буквы — у маркировочного кода они бывают. */
    val keyboardLetters: ImageVector = Icons.Outlined.Keyboard

    /** Поле штрихкода: вернуть цифровую клавиатуру. */
    val keyboardDigits: ImageVector = Icons.Outlined.Dialpad

    /** Доставка чека покупателю: SMS, мессенджер, почта. */
    val receiptDelivery: ImageVector = Icons.AutoMirrored.Outlined.Send

    /** Подробности строки чека: всё, что в строку корзины не влезло. */
    val receiptLine: ImageVector = Icons.AutoMirrored.Outlined.ReceiptLong

    /** Цена, которую задаёт кассир: в каталоге её нет. */
    val price: ImageVector = Icons.Outlined.Sell

    /** Режим отладки и журнал приложения. */
    val debug: ImageVector = Icons.Outlined.BugReport

    /** Светлая тема: день в зале. */
    val lightTheme: ImageVector = Icons.Outlined.LightMode

    /** Тёмная тема: ночная смена. */
    val darkTheme: ImageVector = Icons.Outlined.DarkMode

    /** Вышла новая версия кассы. */
    val update: ImageVector = Icons.Outlined.NewReleases

    /**
     * Разделы настроек в списке слева — контурные, как значки строк
     * списка Material 3: основное кассы, печать, налоги, связь с БФД,
     * оформление, язык, экран продажи, машина и подключение к кабинету.
     * Доставка, сведения о программе и отладка берут значки своих областей.
     */
    val sectionKkm: ImageVector = Icons.Outlined.PointOfSale
    val sectionPrinting: ImageVector = Icons.Outlined.Print
    val sectionTaxes: ImageVector = Icons.Outlined.Percent
    val sectionBfd: ImageVector = Icons.Outlined.CloudSync
    val sectionLook: ImageVector = Icons.Outlined.Palette
    val sectionLanguage: ImageVector = Icons.Outlined.Language
    val sectionSalePanels: ImageVector = Icons.AutoMirrored.Outlined.ViewSidebar
    val sectionMachine: ImageVector = Icons.Outlined.Computer
    val sectionAddresses: ImageVector = Icons.Outlined.Link
}
