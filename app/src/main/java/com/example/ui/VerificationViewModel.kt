package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiService
import com.example.data.AppDatabase
import com.example.data.TobaccoPackInfo
import com.example.data.VerificationLog
import com.example.data.VerificationOutcome
import com.example.data.VerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VerificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VerificationRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VerificationRepository(database.verificationLogDao())
    }

    // --- Local Log History Flow ---
    val logs: StateFlow<List<VerificationLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Scanning / QR State ---
    private val _scannedCode = MutableStateFlow("")
    val scannedCode: StateFlow<String> = _scannedCode.asStateFlow()

    private val _isCameraOpen = MutableStateFlow(false)
    val isCameraOpen: StateFlow<Boolean> = _isCameraOpen.asStateFlow()

    private val _verificationOutcome = MutableStateFlow<VerificationOutcome?>(null)
    val verificationOutcome: StateFlow<VerificationOutcome?> = _verificationOutcome.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    // --- AI Recon State ---
    private val _aiPrompt = MutableStateFlow("")
    val aiPrompt: StateFlow<String> = _aiPrompt.asStateFlow()

    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _inspectedBrandForAi = MutableStateFlow("Marlboro Gold")
    val inspectedBrandForAi: StateFlow<String> = _inspectedBrandForAi.asStateFlow()

    // --- Visual Inspection Checklist State map ---
    val visualAnswers = mutableStateMapOf<String, Boolean?>(
        "stamp" to null,      // true = Yes (Authentic), false = No (Suspicious), null = Unanswered
        "tape" to null,
        "print" to null,
        "wrapper" to null
    )

    private val _visualChecklistBrand = MutableStateFlow("Marlboro Gold")
    val visualChecklistBrand: StateFlow<String> = _visualChecklistBrand.asStateFlow()

    // --- Actions ---

    fun setScannedCode(code: String) {
        _scannedCode.value = code
    }

    fun setCameraOpen(open: Boolean) {
        _isCameraOpen.value = open
    }

    fun setAiPrompt(text: String) {
        _aiPrompt.value = text
    }

    fun setInspectedBrandForAi(brand: String) {
        _inspectedBrandForAi.value = brand
    }

    fun setVisualBrand(brand: String) {
        _visualChecklistBrand.value = brand
    }

    fun setVisualAnswer(criterion: String, value: Boolean?) {
        visualAnswers[criterion] = value
    }

    fun resetVisualChecklist() {
        visualAnswers["stamp"] = null
        visualAnswers["tape"] = null
        visualAnswers["print"] = null
        visualAnswers["wrapper"] = null
    }

    // Trigger QR Scan verification
    fun verifyScannedCode(code: String, method: String = "QR SCANNER") {
        if (code.isBlank()) return
        viewModelScope.launch {
            _isVerifying.value = true
            _scannedCode.value = code
            val outcome = repository.verifyAndSavePack(code, method)
            _verificationOutcome.value = outcome
            _isVerifying.value = false
        }
    }

    fun clearActiveOutcome() {
        _verificationOutcome.value = null
        _scannedCode.value = ""
    }

    // Trigger AI forensic analysis via REST API + save log to Room
    fun analyzePackWithAi() {
        val userPrompt = _aiPrompt.value
        val brand = _inspectedBrandForAi.value
        if (userPrompt.isBlank()) return

        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResult.value = null

            val forensicPrompt = "Brand to analyze: $brand.\n" +
                    "Context: The merchant reports these findings on this pack: $userPrompt\n" +
                    "Perform a thorough forensic verification."

            val result = GeminiService.analyzeCigarettePack(forensicPrompt)
            _aiResult.value = result

            // Evaluate if result refers to "genuine" or "authentic" or "counterfeit" to log
            val isVerifiedCounterfeit = result.lowercase().contains("counterfeit") || result.lowercase().contains("fake")
            val isVerifiedAuthentic = result.lowercase().contains("genuine") || result.lowercase().contains("authentic")
            
            val isAuthentic = !isVerifiedCounterfeit && (isVerifiedAuthentic || !result.lowercase().contains("suspect"))
            val confidence = if (isVerifiedCounterfeit) 15 else if (isVerifiedAuthentic) 95 else 50
            val summary = if (result.length > 200) result.substring(0, 197) + "..." else result

            repository.saveAiLog(
                code = "AI-ANALYSIS",
                brand = brand,
                isAuthentic = isAuthentic,
                confidence = confidence,
                notes = "AI Recon notes: $summary"
            )

            _isAiLoading.value = false
        }
    }

    fun resetAiConsole() {
        _aiPrompt.value = ""
        _aiResult.value = null
    }

    // Save visual checklist evaluation results
    fun submitVisualChecklist() {
        val brand = _visualChecklistBrand.value
        
        // Calculate scores
        var yesCount = 0
        var totalAnswered = 0
        for ((_, ans) in visualAnswers) {
            if (ans != null) {
                totalAnswered++
                if (ans) yesCount++
            }
        }

        if (totalAnswered == 0) return

        val percentRating = ((yesCount.toFloat() / totalAnswered.toFloat()) * 100).toInt()
        val isAuthentic = percentRating >= 70
        val outcomeNotes = "Visual assessment rating: $percentRating% alignment with authentic features. " +
                "Answers summary: Tax Stamp: ${visualAnswers["stamp"]}, Hologram: ${visualAnswers["tape"]}, " +
                "Printing: ${visualAnswers["print"]}, Foil Pack: ${visualAnswers["wrapper"]}."

        viewModelScope.launch {
            _isVerifying.value = true
            repository.saveAiLog(
                code = "VISUAL-CHECKLIST",
                brand = brand,
                isAuthentic = isAuthentic,
                confidence = percentRating,
                notes = outcomeNotes
            )
            
            // Set simulated outcome to trigger result card dialog
            val visualOutcome = if (isAuthentic) {
                VerificationOutcome.Success(
                    brand = brand,
                    packInfo = TobaccoPackInfo(
                        brand = brand,
                        manufactureDate = "Manual Visual Check",
                        productionLine = "Merchant Inspection",
                        factoryLocation = "Store Counter Counterfoil",
                        taxPaidStatus = "Excise Stamp Present: ${visualAnswers["stamp"] == true}",
                        securityLevel = "Checklist Match: $percentRating%"
                    ),
                    details = "Self-inspection complete. Verified authentic features: $yesCount/$totalAnswered. $outcomeNotes"
                )
            } else {
                VerificationOutcome.Failed(
                    brand = brand,
                    reason = "Failed manual criteria. Custom visual markings are non-compliant, suggesting counterfeited packaging: $outcomeNotes"
                )
            }

            _verificationOutcome.value = visualOutcome
            _isVerifying.value = false
        }
    }

    // Database Log Utilities
    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
