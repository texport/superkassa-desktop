package kz.mybrain.superkassa.presentation.common.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.signin.model.SignInState
import kz.mybrain.superkassa.domain.signin.model.sameSeat

/** Следит за потоком, пока жива модель: каждое новое значение — в [block]. */
fun <T> ViewModel.follow(flow: Flow<T>, block: suspend (T) -> Unit): Job =
    viewModelScope.launch { flow.collect { block(it) } }

/**
 * Следит за входом так, как это нужно экрану области.
 *
 * Касса меняется и под тем же кассиром — перечитана после чека, режим
 * включён, — и это [each]: экран берёт её новое состояние, набранное
 * остаётся. Сел другой кассир или выбрана другая касса — это [seated]:
 * прочитанное и набранное принадлежали прежнему, экран начинает заново.
 * Первое состояние — тоже [seated]: модель читает то, что застала.
 */
fun ViewModel.followSeat(
    signIn: Flow<SignInState>,
    each: (SignInState) -> Unit = {},
    seated: (SignInState) -> Unit
): Job {
    var last: SignInState? = null
    return follow(signIn) { now ->
        each(now)
        val same = last?.sameSeat(now) == true
        last = now
        if (!same) seated(now)
    }
}
