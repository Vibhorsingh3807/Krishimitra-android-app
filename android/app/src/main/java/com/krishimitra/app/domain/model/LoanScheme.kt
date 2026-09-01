package com.krishimitra.app.domain.model

import java.util.UUID

enum class FarmerOwnershipType(val labelEn: String, val labelHi: String) {
    OWN_LAND("Own Land (Owner-Cultivator)", "खुद की जमीन (मालिक-किसान)"),
    TENANT("Tenant Farmer", "किरायेदार / काश्तकार किसान"),
    SHARECROPPER("Sharecropper", "बटाईदार किसान"),
    LEASED("Lease Agreement", "पट्टा / लीज पर ली गई भूमि")
}

enum class LoanPurpose(val id: String, val icon: String, val labelEn: String, val labelHi: String) {
    SEEDS("seeds", "🌱", "Seeds & Sowing", "बीज एवं बुवाई"),
    FERTILIZER("fertilizer", "🧪", "Fertilizers & Nutrients", "खाद एवं उर्वरक"),
    IRRIGATION("irrigation", "💧", "Irrigation & Borewell", "सिंचाई व बोरवेल"),
    MACHINERY("machinery", "🚜", "Tractor & Machinery", "ट्रैक्टर व कृषि यंत्र"),
    CULTIVATION("cultivation", "🌾", "Crop Cultivation Expenses", "फसल उत्पादन खर्च"),
    POST_HARVEST("post_harvest", "🏠", "Post-Harvest Expenses", "कटाई उपरांत खर्च"),
    STORAGE("storage", "📦", "Warehouse & Storage", "भंडारण व वेयरहाउस"),
    ALLIED("allied", "🐄", "Dairy & Allied Agriculture", "पशुपालन व डेयरी")
}

enum class MatchLevel(val labelEn: String, val labelHi: String) {
    HIGH("High Match", "उच्च मिलान"),
    MEDIUM("Medium Match", "मध्यम मिलान"),
    POTENTIAL("Potential Match", "संभावित मिलान")
}

data class LoanScheme(
    val schemeId: String,
    val nameEn: String,
    val nameHi: String,
    val provider: String,
    val providerHi: String,
    val schemeType: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val eligibleFarmerTypes: List<FarmerOwnershipType>,
    val eligibleStates: List<String>, // Empty means all India
    val eligibleCrops: List<String>, // Empty means all crops
    val eligiblePurposes: List<LoanPurpose>,
    val minAreaAcres: Double = 0.0,
    val maxAreaAcres: Double = 1000.0,
    val interestRateInfoEn: String,
    val interestRateInfoHi: String,
    val collateralInfoEn: String,
    val collateralInfoHi: String,
    val benefitsEn: List<String>,
    val benefitsHi: List<String>,
    val requiredDocumentsEn: List<String>,
    val requiredDocumentsHi: List<String>,
    val applicationMethodEn: String,
    val applicationMethodHi: String,
    val officialUrl: String,
    val source: String,
    val lastVerified: String
)

data class LoanRecommendation(
    val scheme: LoanScheme,
    val matchLevel: MatchLevel,
    val matchScore: Int, // 0 - 100
    val matchReasonsEn: List<String>,
    val matchReasonsHi: List<String>,
    val estimatedCreditLimitTextEn: String,
    val estimatedCreditLimitTextHi: String
)

data class FarmerLoanProfile(
    val farmerName: String = "",
    val mobileNumber: String = "",
    val state: String = "उत्तर प्रदेश (Uttar Pradesh)",
    val district: String = "",
    val village: String = "",
    val ownershipType: FarmerOwnershipType = FarmerOwnershipType.OWN_LAND,
    val area: Double = 3.0,
    val isHectares: Boolean = false,
    val selectedCrops: List<String> = listOf("Wheat", "Rice"),
    val selectedPurposes: List<LoanPurpose> = listOf(LoanPurpose.CULTIVATION, LoanPurpose.FERTILIZER)
)

data class UploadedDocument(
    val categoryId: String,
    val categoryNameEn: String,
    val categoryNameHi: String,
    val fileName: String,
    val isUploaded: Boolean = false,
    val uploadTimestamp: Long = 0L
)

enum class ApplicationStatus(val labelEn: String, val labelHi: String) {
    DRAFT("Draft", "ड्राफ्ट"),
    READY_TO_SUBMIT("Ready to Submit", "जमा करने हेतु तैयार"),
    PREPARED_PROTOTYPE("Application Prepared (Demo)", "आवेदन तैयार (डेमो)"),
    SUBMITTED_DEMO("Submitted to Demo Backend", "डेमो बैकएंड में जमा"),
    UNDER_REVIEW("Under Review", "समीक्षाधीन")
}

data class LoanApplicationSubmission(
    val applicationId: String = "KM-LOAN-" + UUID.randomUUID().toString().take(8).uppercase(),
    val schemeId: String,
    val schemeNameEn: String,
    val schemeNameHi: String,
    val farmerName: String,
    val mobileNumber: String,
    val state: String,
    val district: String,
    val village: String,
    val landAreaText: String,
    val ownershipTypeText: String,
    val selectedCropsText: String,
    val purposeText: String,
    val requestedAmountText: String,
    val bankName: String,
    val branchName: String,
    val accountNumberMasked: String, // Masked: XXXXXX1234
    val documentsUploadedCount: Int,
    val totalRequiredDocuments: Int,
    val status: ApplicationStatus = ApplicationStatus.PREPARED_PROTOTYPE,
    val submissionDate: String,
    val officialPortalUrl: String
)
