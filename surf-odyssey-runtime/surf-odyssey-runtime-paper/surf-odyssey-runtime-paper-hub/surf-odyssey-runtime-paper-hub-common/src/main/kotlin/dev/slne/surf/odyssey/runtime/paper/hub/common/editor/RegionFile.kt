package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import java.io.IOException
import java.io.Writer
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/** Saves the shape block of an editor session as `regions/<id>.kt`. */
object RegionFile {
    /** Outcome of [save]. */
    sealed interface Result {
        /**
         * The shape block was written.
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
     * Writes the shape block of [session] to `regions/<id>.kt` inside [dataFolder], creating the
     * folder if needed. An existing file is never overwritten.
     */
    fun save(dataFolder: Path, session: EditorSession): Result =
        save(dataFolder, session.regionId) { it.write(RegionCodeWriter.write(session)) }

    /**
     * Creates `regions/<regionId>.kt` inside [dataFolder] and fills it with [write]. An existing file is
     * never overwritten; a file whose writing fails is deleted again.
     */
    internal fun save(dataFolder: Path, regionId: String, write: (Writer) -> Unit): Result {
        val file = dataFolder.resolve("regions").resolve("$regionId.kt")
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
            writer.use(write)
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
