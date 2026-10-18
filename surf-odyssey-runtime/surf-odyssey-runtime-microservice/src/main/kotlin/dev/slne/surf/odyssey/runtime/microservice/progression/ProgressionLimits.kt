package dev.slne.surf.odyssey.runtime.microservice.progression

/** The maximum lengths of the text columns that store progression state. */
object ProgressionLimits {
    /** The maximum length of a definition id (quest, recipe, title, station type) and of a delivery source. */
    const val MAX_ID_LENGTH = 64

    /** The maximum length of a stored rejection reason. */
    const val MAX_REASON_LENGTH = 255
}
