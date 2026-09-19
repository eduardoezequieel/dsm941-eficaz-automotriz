package com.eficazautomotriz.logging

/**
 * Contrato de registro de eventos. La aplicacion depende de esta interfaz y no de la
 * implementacion concreta: en la Etapa 3 se puede sustituir por un logger de Android
 * sin tocar los casos de uso.
 */
interface ErrorLogger {

    /** Errores de negocio esperados: la operacion se rechazo por una regla, no por una falla. */
    fun warn(context: String, message: String, data: String = "")

    /** Fallas no previstas: excepciones que el programa no anticipo. */
    fun error(context: String, message: String, data: String = "")
}
