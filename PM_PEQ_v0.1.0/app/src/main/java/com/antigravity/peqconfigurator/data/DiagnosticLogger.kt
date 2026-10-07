package com.antigravity.peqconfigurator.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val category: String,
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

object DiagnosticLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    var isDebugEnabled: Boolean = true

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(level: LogLevel, category: String, message: String) {
        if (!isDebugEnabled && level == LogLevel.DEBUG) return
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            category = category,
            message = message
        )
        _logs.value = (_logs.value + entry).takeLast(500)
    }

    fun d(category: String, message: String) = log(LogLevel.DEBUG, category, message)
    fun i(category: String, message: String) = log(LogLevel.INFO, category, message)
    fun w(category: String, message: String) = log(LogLevel.WARN, category, message)
    fun e(category: String, message: String) = log(LogLevel.ERROR, category, message)

    fun clear() {
        _logs.value = emptyList()
    }
}
