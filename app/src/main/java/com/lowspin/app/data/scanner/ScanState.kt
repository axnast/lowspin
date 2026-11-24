package com.lowspin.app.data.scanner

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val scanned: Int, val total: Int) : ScanState
    data class Enriching(val done: Int, val total: Int) : ScanState
    data class Complete(
        val songCount: Int,
        val filteredCount: Int,
        val finishedAt: Long
    ) : ScanState

    data class Failed(val message: String) : ScanState
}
