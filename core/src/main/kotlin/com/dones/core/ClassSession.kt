package com.dones.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClassInfo(
    val code: String,
    val courseName: String,
    val churchName: String,
    val teacherName: String,
)

/** Cada persona avanza sola por la enseñanza; la clase solo da los datos del curso. */
data class ClassState(val info: ClassInfo)

sealed interface JoinResult {
    data class Joined(val state: ClassState) : JoinResult
    data object InvalidCode : JoinResult
    /** El código tiene buen formato pero no hay una clase abierta con ese código. */
    data object ClassNotFound : JoinResult
}

/** Conexión con la clase del maestro. En el demo solo existe la clase simulada. */
interface ClassSession {
    val state: StateFlow<ClassState?>
    fun join(rawCode: String): JoinResult
}

object JoinCode {
    const val DEMO_CODE = "000000"

    /**
     * Acepta lo que escribe la persona o lo que trae el QR
     * (por ejemplo "481902", "481 902" o "dones://clase/481902") y devuelve los 6 dígitos.
     */
    fun parse(raw: String): String? {
        val match = Regex("""(?<!\d)(\d{3})\s?(\d{3})(?!\d)""").find(raw.trim()) ?: return null
        return match.groupValues[1] + match.groupValues[2]
    }
}

/** Clase simulada dentro del teléfono (código 000000), para probar el demo sin servidor. */
class SimulatedClassSession : ClassSession {

    private val info = ClassInfo(
        code = JoinCode.DEMO_CODE,
        courseName = "Descubriendo Mi Ministerio",
        churchName = "Iglesia Anglicana San Andrés",
        teacherName = "Maestro de prueba",
    )

    private val _state = MutableStateFlow<ClassState?>(null)
    override val state: StateFlow<ClassState?> = _state.asStateFlow()

    override fun join(rawCode: String): JoinResult {
        val code = JoinCode.parse(rawCode) ?: return JoinResult.InvalidCode
        if (code != JoinCode.DEMO_CODE) return JoinResult.ClassNotFound
        val joined = _state.value ?: ClassState(info)
        _state.value = joined
        return JoinResult.Joined(joined)
    }

    fun leave() {
        _state.value = null
    }
}
