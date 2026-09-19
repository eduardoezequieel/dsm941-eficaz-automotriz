package com.eficazautomotriz.cli

import com.eficazautomotriz.cli.menu.ClientMenu
import com.eficazautomotriz.cli.menu.StaffMenu
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.UserRole

/**
 * Sesion en memoria y enrutamiento por rol. La consola lee, valida el formato,
 * invoca el caso de uso y muestra: no decide nada de negocio.
 */
class ConsoleApp(private val context: AppContext) {

    fun start() {
        welcome()
        while (true) {
            val user = selectUser() ?: break
            routeByRole(user)
            context.writer.success("Sesion de ${user.name} cerrada.")
            if (!context.reader.confirm("Ingresar con otro usuario")) break
        }
        farewell()
    }

    private fun welcome() {
        context.writer.title("Eficaz Automotriz - Gestion de citas y mantenimiento")
        context.writer.line("Etapa 2 - Nucleo funcional en Kotlin sobre consola.")
        context.writer.line("Universidad Don Bosco - DSM941 - Ciclo II 2026.")
        context.writer.hint("Los datos cargados son de demostracion y no corresponden al taller real.")
    }

    private fun farewell() {
        context.writer.line()
        context.writer.line("Hasta pronto.")
    }

    /** No hay contrasena: la autenticacion real llega en la Etapa 3 con Firebase. */
    private fun selectUser(): User? {
        context.writer.section("Seleccione el usuario de la sesion")
        val users = context.auth.availableUsers()
        val selected = context.reader.readChoice("Usuario", users) { "${it.name} - ${it.role.label}" }
            ?: return null

        return when (val outcome = context.auth.signIn(selected.id)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> {
                context.writer.failure(outcome.error.message)
                null
            }
        }
    }

    /** when exhaustivo sobre el rol: un rol nuevo rompe la compilacion, no la ejecucion. */
    private fun routeByRole(user: User) = when (user.role) {
        UserRole.CLIENT -> ClientMenu(context, user).run()
        UserRole.STAFF -> StaffMenu(context, user).run()
    }
}