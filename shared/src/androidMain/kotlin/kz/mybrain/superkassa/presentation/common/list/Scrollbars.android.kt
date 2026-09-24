package kz.mybrain.superkassa.presentation.common.list

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** На телефоне полосу показывает сама прокрутка пальцем: своей нет. */
@Composable
actual fun ListScrollbar(state: LazyListState, modifier: Modifier) = Unit

/** На телефоне полосу показывает сама прокрутка пальцем: своей нет. */
@Composable
actual fun ColumnScrollbar(state: ScrollState, modifier: Modifier) = Unit

/** На телефоне полосу показывает сама прокрутка пальцем: своей нет. */
@Composable
actual fun RowScrollbar(state: ScrollState, modifier: Modifier) = Unit
