package kz.mybrain.superkassa.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Видимая полоса прокрутки списка.
 *
 * На настольной кассе полоса стоит всегда: список, уходящий за край без
 * признака продолжения, читается как весь список. На телефоне полосу
 * рисует сама прокрутка пальцем, и своей там нет.
 */
@Composable
expect fun ListScrollbar(state: LazyListState, modifier: Modifier)

/** Видимая полоса прокрутки колонки; то же правило, что у [ListScrollbar]. */
@Composable
expect fun ColumnScrollbar(state: ScrollState, modifier: Modifier)
