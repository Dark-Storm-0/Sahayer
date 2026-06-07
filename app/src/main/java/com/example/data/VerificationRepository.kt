package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VerificationRepository(private val dao: VerificationLogDao) {
    val allLogs: Flow<List<VerificationLog>> = dao.getAllLogs()

    suspend fun insert(log: VerificationLog) = withContext(Dispatchers.IO) {
        dao.insertLog(log)
    }

    suspend fun deleteById(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteLogById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAllLogs()
    }

    // Handles scanning, analyzing, counting clones, and inserting logged outcome
    suspend fun verifyAndSavePack(
        code: String,
        method: String = "QR SCANNER"
    ): VerificationOutcome = withContext(Dispatchers.IO) {
        val trimmed = code.trim()
        
        // 1. Fetch count of existing scans of this code to detect Cloned Codes
        val allCurrentLogs = dao.getAllLogs().firstOrNull() ?: emptyList()
        val scannedCount = allCurrentLogs.count { it.serialNumber.equals(trimmed, ignoreCase = true) }

        // 2. Run engine validation
        val outcome = TobaccoAuthEngine.authenticateCode(trimmed, scannedCount)

        // 3. Draft the log model to save
        val logToSave = when (outcome) {
            is VerificationOutcome.Success -> {
                VerificationLog(
                    serialNumber = trimmed,
                    brandName = outcome.brand,
                    verificationMethod = method,
                    isAuthentic = true,
                    confidence = 100,
                    statusReason = outcome.details
                )
            }
            is VerificationOutcome.Warning -> {
                VerificationLog(
                    serialNumber = trimmed,
                    brandName = outcome.brand,
                    verificationMethod = method,
                    isAuthentic = true, // Still marked authentic but with advisory caution
                    confidence = 65,
                    statusReason = outcome.reason
                )
            }
            is VerificationOutcome.Failed -> {
                VerificationLog(
                    serialNumber = trimmed,
                    brandName = outcome.brand,
                    verificationMethod = method,
                    isAuthentic = false,
                    confidence = 0,
                    statusReason = outcome.reason
                )
            }
        }

        // 4. Save into database log history
        dao.insertLog(logToSave)

        return@withContext outcome
    }

    // Custom method to save an AI analysis result
    suspend fun saveAiLog(
        code: String,
        brand: String,
        isAuthentic: Boolean,
        confidence: Int,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val log = VerificationLog(
            serialNumber = code.ifBlank { "AI-INPUT" },
            brandName = brand,
            verificationMethod = "AI RECON",
            isAuthentic = isAuthentic,
            confidence = confidence,
            statusReason = notes
        )
        dao.insertLog(log)
    }
}
