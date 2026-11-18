package com.unsa.dhh.captions

import com.unsa.dhh.captions.domain.asr.RecognitionFallbackPolicy
import com.unsa.dhh.captions.domain.asr.RecognitionFallbackPolicy.Action
import org.junit.Assert.*
import org.junit.Test

/** Política de recuperación del reconocedor: casos reales reportados con el dongle USB (errores 12 y 5). */
class RecognitionFallbackPolicyTest {
    private val langNotSupported = 12
    private val langUnavailable = 13
    private val client = 5
    private val audio = 3
    private val noMatch = 7
    private val speechTimeout = 6
    private val permissions = 9

    @Test fun languageNotSupported_switchesProfile_insteadOfStopping() {
        val p = RecognitionFallbackPolicy(profileCount = 3)
        assertEquals(Action.SwitchProfile(1), p.onError(langNotSupported))
        assertEquals(Action.SwitchProfile(2), p.onError(langUnavailable))
        assertEquals(Action.GiveUp, p.onError(langNotSupported)) // se agotó la escalera
    }

    @Test fun transientClientErrors_recreateThenSwitchAfterThree() {
        val p = RecognitionFallbackPolicy(profileCount = 2)
        assertEquals(Action.Recreate, p.onError(client))
        assertEquals(Action.Recreate, p.onError(audio))
        assertEquals(Action.SwitchProfile(1), p.onError(client))
    }

    @Test fun successResetsClientErrorCount() {
        val p = RecognitionFallbackPolicy(profileCount = 2)
        p.onError(client); p.onError(client)
        p.onSuccess()
        assertEquals(Action.Recreate, p.onError(client))
        assertEquals(0, p.profileIndex)
    }

    @Test fun silenceAndNoMatch_justRestart() {
        val p = RecognitionFallbackPolicy(profileCount = 2)
        assertEquals(Action.Restart, p.onError(noMatch))
        assertEquals(Action.Restart, p.onError(speechTimeout))
        assertEquals(0, p.profileIndex)
    }

    @Test fun missingPermission_givesUpImmediately() {
        assertEquals(Action.GiveUp, RecognitionFallbackPolicy(profileCount = 3).onError(permissions))
    }

    @Test fun resetReturnsToBestProfile() {
        val p = RecognitionFallbackPolicy(profileCount = 3)
        p.onError(langNotSupported)
        assertEquals(1, p.profileIndex)
        p.reset()
        assertEquals(0, p.profileIndex)
    }
}
