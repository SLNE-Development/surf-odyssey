package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests for the payload of answer clicks. */
class AnswerPayloadTest {
    private val token = UUID.fromString("3f1e9c2a-7b44-4d0e-9a51-2c6d8e0f4b17")

    @Test
    fun `an encoded payload decodes to its token and line`() {
        assertEquals(token to 3, AnswerPayload.decode(AnswerPayload.encode(token, 3)))
        assertEquals(token to AnswerPayload.CLOSE, AnswerPayload.decode(AnswerPayload.encode(token, AnswerPayload.CLOSE)))
    }

    @Test
    fun `reordered and single quoted payloads decode`() {
        assertEquals(token to 2, AnswerPayload.decode("{line:2,token:'$token'}"))
        assertEquals(token to 2, AnswerPayload.decode("{token: \"$token\", line: 2}"))
    }

    @Test
    fun `malformed payloads decode to nothing`() {
        assertNull(AnswerPayload.decode(null))
        assertNull(AnswerPayload.decode("{}"))
        assertNull(AnswerPayload.decode("{token:\"nope\",line:1}"))
        assertNull(AnswerPayload.decode("{token:\"$token\"}"))
    }
}
