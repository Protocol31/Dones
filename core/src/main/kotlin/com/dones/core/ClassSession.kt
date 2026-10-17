package com.dones.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que el maestro está mostrando en este momento. */
sealed interface ClassStep {
    data object Waiting : ClassStep
    data class HoutsBlock(val block: Int) : ClassStep
    data object Results : ClassStep
    /** El maestro cerró el curso: se emiten los certificados. */
    data object Closed : ClassStep
}

data class ClassInfo(
    val code: String,
    val courseName: String,
    val churchName: String,
    val teacherName: String,
)

data class ClassState(
    val info: ClassInfo,
    val steps: List<ClassStep>,
    val stepIndex: Int,
) {
    val current: ClassStep get() = steps[stepIndex]
    val isFirst: Boolean get() = stepIndex == 0
    val isLast: Boolean get() = stepIndex == steps.lastIndex
}

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

/**
 * Clase simulada dentro del teléfono (código 000000). Sirve para probar el demo sin servidor:
 * un botón "Maestro simulado" avanza los pasos como lo haría el maestro desde su PC.
 */
class SimulatedClassSession(
    blocks: List<Int>,
    initialStepIndex: Int = 0,
    private val onStepChanged: (Int) -> Unit = {},
) : ClassSession {

    private val steps: List<ClassStep> =
        listOf(ClassStep.Waiting) + blocks.map { ClassStep.HoutsBlock(it) } +
            listOf(ClassStep.Results, ClassStep.Closed)

    private val info = ClassInfo(
        code = JoinCode.DEMO_CODE,
        courseName = "Descubriendo Mi Ministerio",
        churchName = "Iglesia de prueba",
        teacherName = "Maestro de prueba",
    )

    private val _state = MutableStateFlow<ClassState?>(
        if (initialStepIndex > 0) ClassState(info, steps, initialStepIndex.coerceIn(0, steps.lastIndex)) else null
    )
    override val state: StateFlow<ClassState?> = _state.asStateFlow()

    override fun join(rawCode: String): JoinResult {
        val code = JoinCode.parse(rawCode) ?: return JoinResult.InvalidCode
        if (code != JoinCode.DEMO_CODE) return JoinResult.ClassNotFound
        val joined = _state.value ?: ClassState(info, steps, 0)
        _state.value = joined
        return JoinResult.Joined(joined)
    }

    fun teacherNext() = move(+1)
    fun teacherPrevious() = move(-1)

    fun leave() {
        _state.value = null
        onStepChanged(0)
    }

    private fun move(delta: Int) {
        val s = _state.value ?: return
        val next = (s.stepIndex + delta).coerceIn(0, s.steps.lastIndex)
        if (next != s.stepIndex) {
            _state.value = s.copy(stepIndex = next)
            onStepChanged(next)
        }
    }
}
