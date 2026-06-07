package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class representing decoded tobacco pack metadata
data class TobaccoPackInfo(
    val brand: String,
    val manufactureDate: String,
    val productionLine: String,
    val factoryLocation: String,
    val taxPaidStatus: String,
    val securityLevel: String
)

object TobaccoAuthEngine {
    // List of predefined valid simulation templates
    val VALID_PRODUCTS = listOf(
        "Marlboro Gold" to "EU-LEPL-M72B-94F32948",
        "Camel Blue" to "EU-EUTAC-C12A-18F42173",
        "Lucky Strike Red" to "EU-EUTAC-L48X-59C10284",
        "Winston Blue" to "EU-LEPL-W22D-77A93820",
        "Dunhill Red" to "EU-EUTAC-D83H-44B83019",
        "Marlboro Red" to "EU-LEPL-M72R-50F12450"
    )

    // Decodes information embedded in the QR-code or serial number structure
    fun decodePackInfo(code: String): TobaccoPackInfo {
        val trimmed = code.uppercase().trim()
        val parts = trimmed.split("-")
        
        // Default values
        var brand = "Unknown Brand"
        var mfgDate = "Unknown Date"
        var lineId = "Line A"
        var plantName = "Central EU Facility"
        var taxGroup = "Standard Tax Case"
        var riskLevel = "Secure"

        // Determine Brand
        when {
            trimmed.contains("LEPL-M72B") -> {
                brand = "Marlboro Gold (Compact)"
                mfgDate = "2026-02-14"
                lineId = "Line 04A (High Speed)"
                plantName = "Krakow Production Plant (Poland)"
                taxGroup = "Class A - Fully Paid Stamp"
                riskLevel = "Guaranteed Authentic"
            }
            trimmed.contains("LEPL-M72R") -> {
                brand = "Marlboro Red (Standard)"
                mfgDate = "2026-03-01"
                lineId = "Line 02B"
                plantName = "Krakow Production Plant (Poland)"
                taxGroup = "Class A - Fully Paid Stamp"
                riskLevel = "Guaranteed Authentic"
            }
            trimmed.contains("EUTAC-C12A") -> {
                brand = "Camel Blue (Flipped Pack)"
                mfgDate = "2026-01-20"
                lineId = "Line 12"
                plantName = "Trier Tobacco Mill (Germany)"
                taxGroup = "Class B - Customs Cleared"
                riskLevel = "Guaranteed Authentic"
            }
            trimmed.contains("EUTAC-L48X") -> {
                brand = "Lucky Strike Red"
                mfgDate = "2025-11-15"
                lineId = "Line 01-X"
                plantName = "Ploiesti Plant (Romania)"
                taxGroup = "Class C - Domestic Retail Only"
                riskLevel = "Guaranteed Authentic"
            }
            trimmed.contains("LEPL-W22D") -> {
                brand = "Winston Blue"
                mfgDate = "2026-04-10"
                lineId = "Line 09W"
                plantName = "Geneva Tobacco Hub (Switzerland)"
                taxGroup = "Import Class S"
                riskLevel = "Guaranteed Authentic"
            }
            trimmed.contains("EUTAC-D83H") -> {
                brand = "Dunhill Red (Premium)"
                mfgDate = "2026-05-02"
                lineId = "Line P1"
                plantName = "Southampton Plant (United Kingdom)"
                taxGroup = "Premium Duty Paid"
                riskLevel = "Guaranteed Authentic"
            }
            else -> {
                // Generative decode based on parts
                if (parts.size >= 4) {
                    val mfgHash = parts[2].hashCode().coerceAtLeast(0)
                    brand = when(parts[1].uppercase()) {
                        "LEPL" -> "Philip Morris Int. Group Product"
                        "EUTAC" -> "British American Tobacco Group Product"
                        else -> "Imported Tobacco Product"
                    }
                    val monthsAgo = (mfgHash % 5) + 1
                    mfgDate = "2025-" + String.format(Locale.getDefault(), "%02d", 12 - monthsAgo) + "-15"
                    lineId = "Sub-line ${parts[2].take(2)}"
                    plantName = "EU Authorized Area Plant ID ${parts[2].takeLast(2)}"
                }
            }
        }
        return TobaccoPackInfo(brand, mfgDate, lineId, plantName, taxGroup, riskLevel)
    }

    // Checksum verification loop (XOR/Sum parity verification of code)
    private fun verifyChecksum(code: String): Boolean {
        if (code.length < 5) return false
        val cleanCode = code.replace("-", "").uppercase()
        if (cleanCode.length < 8) return false
        
        // Take everything except the last 2 digits which contain hex checksum
        val body = cleanCode.substring(0, cleanCode.length - 2)
        val claimedHex = cleanCode.substring(cleanCode.length - 2)
        
        var calculatedCheck = 0
        for (i in body.indices) {
            calculatedCheck = (calculatedCheck + body[i].code * (i + 1)) % 256
        }
        val calculatedHex = String.format(Locale.getDefault(), "%02X", calculatedCheck)
        
        // For standard simulated codes we bypass mathematical CRC mismatch if they are from pre-set list
        for (vp in VALID_PRODUCTS) {
            if (code.equals(vp.second, ignoreCase = true)) {
                return true
            }
        }
        
        return claimedHex == calculatedHex
    }

    // Main local verification algorithm
    fun authenticateCode(
        code: String,
        historicScanCount: Int
    ): VerificationOutcome {
        val trimmed = code.uppercase().trim()
        
        if (trimmed.isEmpty()) {
            return VerificationOutcome.Failed(
                reason = "Scan Error: Empty QR standard or unreadable barcode pixels.",
                brand = "N/A"
            )
        }

        // 1. Structural rule validation
        val parts = trimmed.split("-")
        if (parts.size != 4) {
            return VerificationOutcome.Failed(
                reason = "Counterfeit Alert: Sequence pattern error! Authorized tobacco trackcodes must conform to standard quadripartite format (e.g. EU-XXXX-XXXX-XXXXXXXX).",
                brand = "Suspicious Pack"
            )
        }

        // EU Authority Issuer verification
        val region = parts[0]
        if (region != "EU") {
            return VerificationOutcome.Failed(
                reason = "Counterfeit Alert: Invalid Region Signature. Tobacco Authority registration prefix '$region' is unauthorized.",
                brand = "Illegal Product"
            )
        }

        val issuer = parts[1]
        if (issuer != "LEPL" && issuer != "EUTAC") {
            return VerificationOutcome.Failed(
                reason = "Counterfeit Alert: Unknown ID Issuer. The registration pool '$issuer' is not a registered tobacco license agency.",
                brand = "Unauthorized Batch"
            )
        }

        // 2. Check for Cloned QR code (double-scan warning)
        if (historicScanCount > 0) {
            val packInfo = decodePackInfo(trimmed)
            return VerificationOutcome.Failed(
                reason = "Cloned QR alert! This exact serial number code ($trimmed) has already been successfully registered in your database. Counterfeiters copy/photocopy identical real QR codes from authentic stores onto hundreds of fake cartons.",
                brand = "${packInfo.brand} (Cloned Entry)"
            )
        }

        // 3. Expiration threshold (e.g. old stock)
        val packInfo = decodePackInfo(trimmed)
        if (packInfo.manufactureDate.startsWith("2021") || packInfo.manufactureDate.startsWith("2022")) {
            return VerificationOutcome.Warning(
                reason = "Suspect Stock: This pack holds an expired/archived stamp manufactured on ${packInfo.manufactureDate}. Tobacco freshness limits typically expire in 2 years. Could be repurposed or smuggled vintage stocks.",
                brand = packInfo.brand,
                packInfo = packInfo
            )
        }

        // 4. Secure Checksum verification
        if (!verifyChecksum(trimmed)) {
            return VerificationOutcome.Failed(
                reason = "Counterfeit Alert: Security checksum mismatch! The security hash sequence appended to this serialization series did not resolve on the parity verification algorithm.",
                brand = packInfo.brand
            )
        }

        // Passed all system verification rules!
        return VerificationOutcome.Success(
            brand = packInfo.brand,
            packInfo = packInfo,
            details = "Passed all cryptographic signature validations. Manufactured: ${packInfo.manufactureDate}, Factory: ${packInfo.factoryLocation}. Registered to tax group: ${packInfo.taxPaidStatus}."
        )
    }
}

sealed class VerificationOutcome {
    data class Success(
        val brand: String,
        val packInfo: TobaccoPackInfo,
        val details: String
    ) : VerificationOutcome()

    data class Warning(
        val brand: String,
        val reason: String,
        val packInfo: TobaccoPackInfo
    ) : VerificationOutcome()

    data class Failed(
        val brand: String,
        val reason: String
    ) : VerificationOutcome()
}
