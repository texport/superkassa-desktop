package kz.mybrain.superkassa.presentation.users

import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.ProbeNode
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Кассиры кассы процесса для нажатий и снимков.
 *
 * Касса записывает, каким пином её спросили о каждом обращении: по этому
 * видно, чей пин ушёл, — ровно то, что проверки кассиров стерегут.
 */
internal class UsersScene(val users: List<UserResponse> = listOf(ADMIN, CASHIER), pin: String = CoreScene.PIN) {
    val signIn = SignIn().apply { enter(CoreScene.kkm(), ADMIN, pin) }
    val core = FakeCore()

    /** Метод и пин каждого обращения к кассе. */
    val asked = CopyOnWriteArrayList<String>()

    init {
        core.on("getUserRoles") { emptyList<Any>() }
        core.on("listUsers") { args -> users.also { asked += "listUsers ${args[1]}" } }
        core.on("updateUser") { args -> ADMIN.also { asked += "updateUser ${args[2]}" } }
        core.on("deleteUser") { args -> true.also { asked += "deleteUser ${args[2]}" } }
    }

    fun model(): UsersViewModel = usersModel(CoreScene.app(core, signIn))

    companion object {
        val ADMIN = UserResponse(userId = "u-1", name = "Айгүл Сәрсенова", role = UserRole.ADMIN)
        val CASHIER = UserResponse(userId = "u-2", name = "Дана Жумабаева", role = UserRole.CASHIER)
        val DEPUTY = UserResponse(userId = "u-3", name = "Асхат Нұрланов", role = UserRole.ADMIN)
    }
}

/** Узел с надписью [text], стоящий в одной строке с именем [who]. */
internal fun RenderProbe.inRowOf(who: String, text: (ProbeNode) -> Boolean): ProbeNode {
    val nodes = nodes()
    val row = nodes.first { it.text == who }
    val middle = row.at.y + row.height / 2f
    return nodes.filter(text).minBy { kotlin.math.abs(it.at.y + it.height / 2f - middle) }
}
