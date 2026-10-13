package com.dmax.nexus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputRulesTest {
    @Test fun emailValidationAcceptsNormalEmail() {
        assertTrue(InputRules.isValidEmail("nexus@example.com"))
        assertFalse(InputRules.isValidEmail("not-an-email"))
        assertFalse(InputRules.isValidEmail("two@@example.com"))
    }

    @Test fun passwordValidationRequiresEightCharacters() {
        assertFalse(InputRules.isValidPassword("short"))
        assertTrue(InputRules.isValidPassword("correct-horse"))
        assertFalse(InputRules.isValidPassword("x".repeat(129)))
    }

    @Test fun portsMustBeWithinTcpUdpRange() {
        assertEquals(25565, InputRules.parsePort("25565"))
        assertEquals(null, InputRules.parsePort("0"))
        assertEquals(null, InputRules.parsePort("65536"))
        assertEquals(null, InputRules.parsePort("abc"))
    }

    @Test fun endpointRequiresHttps() {
        assertTrue(InputRules.isSafeHttpsEndpoint("https://api.example.com/api/chat"))
        assertFalse(InputRules.isSafeHttpsEndpoint("http://localhost:3000/api/chat"))
        assertFalse(InputRules.isSafeHttpsEndpoint("http://api.example.com/api/chat"))
        assertFalse(InputRules.isSafeHttpsEndpoint("https://"))
        assertFalse(InputRules.isSafeHttpsEndpoint("https://user:pass@api.example.com/api/chat"))
        assertFalse(InputRules.isSafeHttpsEndpoint("https://api.example.com/api/chat#fragment"))
    }
}
