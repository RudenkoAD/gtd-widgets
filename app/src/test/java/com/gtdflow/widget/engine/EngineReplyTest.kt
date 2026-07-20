package com.gtdflow.widget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Разбор ответа моста QuickJS (граница движка) — покрыто без нативного QuickJS. */
class EngineReplyTest {

    @Test
    fun okReplyStripsPrefix() {
        val r = EngineReply.parse("OK:{\"a\":1}")
        assertTrue(r is EngineReply.Ok)
        assertEquals("{\"a\":1}", (r as EngineReply.Ok).payload)
    }

    @Test
    fun okReplyCanBeEmptyPayload() {
        assertEquals(EngineReply.Ok(""), EngineReply.parse("OK:"))
    }

    @Test
    fun errReplyStripsPrefix() {
        val r = EngineReply.parse("ERR:compute did not settle")
        assertTrue(r is EngineReply.Err)
        assertEquals("compute did not settle", (r as EngineReply.Err).message)
    }

    @Test
    fun nullReplyIsError() {
        assertTrue(EngineReply.parse(null) is EngineReply.Err)
    }

    @Test
    fun garbageReplyIsErrorWithDiagnostic() {
        val r = EngineReply.parse("undefined")
        assertTrue(r is EngineReply.Err)
        assertTrue((r as EngineReply.Err).message.contains("unexpected engine reply"))
    }
}
