package com.dmax.nexus

import java.net.URI

object InputRules {
    fun isValidEmail(value: String): Boolean {
        val clean = value.trim()
        return clean.length in 5..254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(clean)
    }

    fun isValidPassword(value: String): Boolean = value.length in 8..128

    fun parsePort(value: String): Int? = value.toIntOrNull()?.takeIf { it in 1..65535 }

    fun isSafeHttpsEndpoint(value: String): Boolean {
        val clean = value.trim()
        if (clean.isBlank() || clean.length > 512) return false
        val uri = runCatching { URI(clean) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true)
            && !uri.host.isNullOrBlank()
            && uri.userInfo == null
            && uri.fragment == null
    }
}
