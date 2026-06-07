package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verification_logs")
data class VerificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serialNumber: String,
    val brandName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val verificationMethod: String, // "QR SCANNER", "VISUAL CHECK", "AI RECON"
    val isAuthentic: Boolean,
    val confidence: Int, // 0 to 100
    val statusReason: String
)
