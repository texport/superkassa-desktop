package kz.mybrain.superkassa.presentation.users

import kz.mybrain.superkassa.KassaScene
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Список кассиров во всех своих состояниях, включая отказ кассы.
 *
 * Снимки остаются в `/tmp/kassa-users-*.png`: по ним видно, объяснён ли
 * отказ словами. Проверка следит за тем, что случаи собираются и
 * отличаются друг от друга.
 */
class UsersListLookTest {

    /**
     * Кассиры кассы: пустой список, список с кассирами и молчащая касса.
     *
     * Пустой список — это не пустота: у кассы без кассиров работать нельзя,
     * и экран обязан сказать, что делать. Отказ кассы — третье состояние,
     * и оно не должно выглядеть пустым списком.
     */
    @Test
    fun `список кассиров рисуется и пустым, и заполненным`() {
        val none = object : UsersActions {}
        val me = UsersScene.ADMIN
        val empty = KassaScene.shot("users-empty") {
            UsersContent(UsersUiState(answered = true, kkmChosen = true, me = me), none)
        }
        val filled = KassaScene.shot("users-two") {
            UsersContent(UsersUiState(users = CASHIERS, answered = true, kkmChosen = true, me = me), none)
        }
        val silent = KassaScene.shot("users-node-silent") {
            UsersContent(UsersUiState(unreadable = true, kkmChosen = true, me = me), none)
        }

        val frames = listOf(empty, filled, silent)
        frames.forEach { assertTrue(it.isNotEmpty()) }
        assertTrue(frames.map { it.toList() }.distinct().size == 3, "состояния списка кассиров неотличимы")
    }

    private companion object {
        val CASHIERS = listOf(UsersScene.ADMIN, UsersScene.CASHIER)
    }
}
