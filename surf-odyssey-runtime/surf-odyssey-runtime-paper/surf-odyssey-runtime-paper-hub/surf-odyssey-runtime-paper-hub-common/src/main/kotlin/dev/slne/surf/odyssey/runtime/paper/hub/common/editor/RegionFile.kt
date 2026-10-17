package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

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
         * Writing the file failed.
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
    fun save(dataFolder: Path, session: EditorSession): Result {
        val file = dataFolder.resolve("regions").resolve("${session.regionId}.kt")
        if (file.exists()) return Result.Exists(file)
        return try {
            file.parent.createDirectories()
            try {
                file.writeText(RegionCodeWriter.write(session), Charsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
                Result.Saved(file)
            } catch (_: FileAlreadyExistsException) {
                Result.Exists(file)
            }
        } catch (error: IOException) {
            Result.Failed(file, error)
        }
    }
}
