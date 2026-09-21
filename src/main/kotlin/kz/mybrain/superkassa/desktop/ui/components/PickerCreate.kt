package kz.mybrain.superkassa.desktop.ui.components

/**
 * Чем из списка заводят то, чего в нём ещё нет.
 *
 * Пустой список — тупик: у владельца без торговых точек мастер
 * подключения кассы просил выбрать точку и не давал её создать.
 * Заводить принято оттуда, где не хватило, а не искать другой экран.
 */
data class PickerCreate(val title: String, val onCreate: () -> Unit)
