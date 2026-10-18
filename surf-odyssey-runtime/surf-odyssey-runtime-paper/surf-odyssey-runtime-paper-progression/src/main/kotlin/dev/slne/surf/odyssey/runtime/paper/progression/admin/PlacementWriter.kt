package dev.slne.surf.odyssey.runtime.paper.progression.admin

import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/** Builds the Kotlin statements that place NPCs and stations from positions in the world. */
object PlacementWriter {
    /** The IDs of definitions: lowercase letters, digits and underscores. */
    private val ID_PATTERN = Regex("[a-z0-9_]+")

    /** Returns whether [id] is a definition ID that can name a placement file and a constant. */
    fun isValidId(id: String): Boolean = ID_PATTERN.matches(id)

    /** Returns the name of the constant that holds the definition with [id], which is [id] in upper case. */
    fun constantName(id: String): String = id.uppercase(Locale.ROOT)

    /**
     * Returns the statement that places the NPC with [id], ended by a line break. Coordinates have one decimal and
     * the angles are whole degrees.
     *
     * @param id the ID of the NPC definition
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param yaw the horizontal view direction in degrees
     * @param pitch the vertical view direction in degrees
     */
    fun npc(id: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): String =
        "npc(Npcs.${constantName(id)}, at = SpawnPoint(${decimal(x)}, ${decimal(y)}, ${decimal(z)}, " +
                "${degrees(yaw)}f, ${degrees(pitch)}f))\n"

    /**
     * Returns the statement that places the station with [id] on a block, ended by a line break.
     *
     * @param id the ID of the station definition
     * @param x the x coordinate of the block
     * @param y the y coordinate of the block
     * @param z the z coordinate of the block
     */
    fun station(id: String, x: Int, y: Int, z: Int): String =
        "station(Stations.${constantName(id)}, at = BlockPos($x, $y, $z))\n"

    /** Formats [value] with one decimal and a point, without a negative zero. */
    private fun decimal(value: Double): String {
        val text = String.format(Locale.ROOT, "%.1f", value)
        return if (text == "-0.0") "0.0" else text
    }

    /** Rounds [value] to whole degrees. */
    private fun degrees(value: Float): Int = Math.round(value)
}

/** Saves a placement statement as `placements/<id>.kt`. */
object PlacementFile {
    /** The outcome of [save]. */
    sealed interface Result {
        /**
         * The statement was written.
         *
         * @property file the written file
         */
        data class Saved(val file: Path) : Result

        /**
         * The file already exists and was left unchanged.
         *
         * @property file the existing file
         */
        data class Exists(val file: Path) : Result

        /**
         * Writing the file failed; no partial file is left behind.
         *
         * @property file the file that could not be written
         * @property error the failure
         */
        data class Failed(val file: Path, val error: IOException) : Result
    }

    /**
     * Writes [text] to `placements/<id>.kt` inside [dataFolder], creating the folder if needed. An existing file is
     * never overwritten, and a file whose writing fails is deleted again.
     *
     * @param dataFolder the data folder of the plugin
     * @param id the ID of the placed definition
     * @param text the file content
     */
    fun save(dataFolder: Path, id: String, text: String): Result {
        val file = dataFolder.resolve("placements").resolve("$id.kt")
        if (file.exists()) return Result.Exists(file)
        val writer = try {
            file.parent.createDirectories()
            file.bufferedWriter(Charsets.UTF_8, options = arrayOf(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))
        } catch (_: FileAlreadyExistsException) {
            if (file.exists()) return Result.Exists(file)
            return Result.Failed(file, IOException("${file.parent} is no directory"))
        } catch (error: IOException) {
            return Result.Failed(file, error)
        }
        return try {
            writer.use { it.write(text) }
            Result.Saved(file)
        } catch (error: IOException) {
            try {
                file.deleteIfExists()
            } catch (cleanup: IOException) {
                error.addSuppressed(cleanup)
            }
            Result.Failed(file, error)
        }
    }
}
