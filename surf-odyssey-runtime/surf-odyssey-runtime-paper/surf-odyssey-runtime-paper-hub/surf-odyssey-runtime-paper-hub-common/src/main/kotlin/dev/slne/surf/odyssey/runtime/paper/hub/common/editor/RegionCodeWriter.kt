package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

/** Turns an editor session into the `shape { }` block a world plugin pastes into its region. */
object RegionCodeWriter {
    /** Returns the Kotlin source of the shape drawn in [session]. */
    fun write(session: EditorSession): String = buildString {
        appendLine("// Region ${session.regionId}")
        appendLine("shape {")
        session.parts.filter { !it.excluded }.forEach { appendPart(it, "    ") }
        val excluded = session.parts.filter { it.excluded }
        if (excluded.isNotEmpty()) {
            appendLine("    exclude {")
            excluded.forEach { appendPart(it, "        ") }
            appendLine("    }")
        }
        appendLine("}")
    }

    /** Appends the DSL call for [part], each line starting with [indent]. */
    private fun StringBuilder.appendPart(part: EditorPart, indent: String) {
        when (part) {
            is PrismPart -> {
                appendLine("${indent}prism(minY = ${part.minY}, maxY = ${part.maxY}) {")
                part.points.forEach { appendLine("$indent    point(${it.x}, ${it.z})") }
                appendLine("$indent}")
            }
            is CuboidPart -> {
                val (a, b) = part.corners
                appendLine("${indent}cuboid(${a.x}, ${a.y}, ${a.z}, ${b.x}, ${b.y}, ${b.z})")
            }
        }
    }
}
