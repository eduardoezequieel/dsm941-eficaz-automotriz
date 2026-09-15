package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.format.Column
import com.eficazautomotriz.cli.format.TableFormatter
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User

/** Una entrada del menu: su etiqueta y la accion que ejecuta. */
data class MenuItem(
    val label: String,
    val action: () -> Unit,
)

/**
 * Base de todos los menus: dibuja las opciones, enruta la eleccion y concentra los
 * gestos que todos repiten (mostrar un resultado, listar una tabla, elegir de una lista).
 */
abstract class Menu(protected val context: AppContext, protected val actor: User) {

    protected abstract val title: String

    protected abstract fun items(): List<MenuItem>

    /** Texto de la opcion 0. El menu raiz cierra sesion; los submenus vuelven atras. */
    protected open val exitLabel: String = "Volver"

    fun run() {
        while (true) {
            val options = items()
            context.writer.title(title)
            options.forEachIndexed { index, item -> context.writer.line("  ${index + 1}) ${item.label}") }
            context.writer.line("  0) $exitLabel")

            val choice = context.reader.readMenuOption("Opcion", options.size) ?: return
            if (choice == 0) return
            options[choice - 1].action()
        }
    }

    /** Resume en una linea que cambio, o por que no se pudo. */
    protected fun <T> report(outcome: Outcome<T>, onSuccess: (T) -> String) =
        handle(outcome) { context.writer.success(onSuccess(it)) }

    /** Igual que `report`, pero el exito se dibuja en pantalla en vez de resumirse. */
    protected fun <T> handle(outcome: Outcome<T>, onSuccess: (T) -> Unit): Unit = when (outcome) {
        is Outcome.Success -> onSuccess(outcome.value)
        is Outcome.Failure -> showError(outcome.error)
    }

    /** Contenido del outcome, o lista vacia dejando el error a la vista. */
    protected fun <T> contentsOf(outcome: Outcome<List<T>>): List<T> = when (outcome) {
        is Outcome.Success -> outcome.value
        is Outcome.Failure -> {
            context.writer.failure(outcome.error.message)
            emptyList()
        }
    }

    /** Tabla con encabezado y conteo, o la pista de que todavia no hay datos. */
    protected fun <T> showTable(
        section: String,
        columns: List<Column<T>>,
        rows: List<T>,
        emptyHint: String,
    ) {
        context.writer.section("$section (${rows.size})")
        if (rows.isEmpty()) {
            context.writer.hint(emptyHint)
            return
        }
        TableFormatter.render(columns, rows).forEach(context.writer::line)
    }

    /** Seleccion sobre una lista; avisa y pausa cuando no hay candidatos que ofrecer. */
    protected fun <T> select(
        section: String,
        prompt: String,
        options: List<T>,
        emptyMessage: String,
        label: (T) -> String,
    ): T? {
        if (options.isEmpty()) {
            context.writer.warning(emptyMessage)
            context.reader.pause()
            return null
        }
        context.writer.section(section)
        return context.reader.readChoice(prompt, options, label)
    }

    private fun showError(error: DomainError) {
        context.writer.failure(error.message)
        context.writer.hint(advice(error))
    }

    /** Traduce el error de negocio a la siguiente accion util para el usuario. */
    private fun advice(error: DomainError): String = when (error) {
        is DomainError.SlotUnavailable ->
            "Elija otra franja u otra fecha del listado."
        is DomainError.InvalidTransition ->
            "El avance del servicio es Recibido, En proceso y Finalizado, sin retrocesos."
        is DomainError.MileageDecrease ->
            "Verifique el odometro: el kilometraje solo puede aumentar."
        is DomainError.NotFound ->
            "Revise el listado y elija un registro existente."
        is DomainError.Forbidden ->
            "Esta operacion corresponde a otro rol o a otro propietario."
        is DomainError.MissingBaseline ->
            "Registre primero un mantenimiento para tener kilometraje base."
        is DomainError.Invalid ->
            "Corrija el campo ${error.validation.label} e intente de nuevo."
    }
}