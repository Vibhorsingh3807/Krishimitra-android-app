package com.krishimitra.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krishimitra.app.domain.model.*
import java.text.DecimalFormat

class LoanSchemeRepository(private val context: Context) {

    private val df = DecimalFormat("#,##,##0")

    val verifiedSchemes: List<LoanScheme> = listOf(
        LoanScheme(
            schemeId = "kcc_crop_loan",
            nameEn = "Kisan Credit Card (KCC) Crop Loan",
            nameHi = "किसान क्रेडिट कार्ड (KCC) फसल ऋण",
            provider = "All Scheduled Commercial Banks, RRBs & Cooperative Banks (SBI, PNB, BoB, etc.)",
            providerHi = "सभी वाणिज्यिक, क्षेत्रीय ग्रामीण एवं सहकारी बैंक (SBI, PNB, आदि)",
            schemeType = "Short-Term Working Capital & Cultivation Credit",
            descriptionEn = "Government of India subsidized credit line for smallholder and commercial farmers to finance crop production expenses, seeds, fertilizers, pesticides, and post-harvest household expenses.",
            descriptionHi = "फसल उत्पादन, बीज, खाद, कीटनाशक एवं कटाई उपरांत खर्चों हेतु भारत सरकार द्वारा रियायती ब्याज दर पर उपलब्ध क्रेडिट कार्ड ऋण।",
            eligibleFarmerTypes = listOf(
                FarmerOwnershipType.OWN_LAND,
                FarmerOwnershipType.TENANT,
                FarmerOwnershipType.SHARECROPPER,
                FarmerOwnershipType.LEASED
            ),
            eligibleStates = emptyList(), // All India
            eligibleCrops = emptyList(), // All major food and commercial crops
            eligiblePurposes = listOf(
                LoanPurpose.CULTIVATION,
                LoanPurpose.SEEDS,
                LoanPurpose.FERTILIZER,
                LoanPurpose.IRRIGATION,
                LoanPurpose.POST_HARVEST
            ),
            minAreaAcres = 0.1,
            maxAreaAcres = 500.0,
            interestRateInfoEn = "7.0% per annum base rate; 3.0% prompt repayment incentive brings effective interest to 4.0% p.a. (up to ₹3 Lakhs).",
            interestRateInfoHi = "7% वार्षिक आधार दर; समय पर भुगतान पर 3% ब्याज छूट से प्रभावी दर मात्र 4% वार्षिक (₹3 लाख तक)।",
            collateralInfoEn = "No collateral or hypothecation required for loans up to ₹1.60 Lakhs (waived by RBI/NABARD).",
            collateralInfoHi = "₹1.60 लाख तक के ऋण पर किसी प्रकार की जमानत/बंधक (Collateral) की आवश्यकता नहीं।",
            benefitsEn = listOf(
                "Short-term crop production credit tied to District Scale of Finance",
                "Subsidized interest rate at effective 4% per annum",
                "Includes 10% provision for post-harvest/household expenses + 20% for farm asset maintenance",
                "Revolving cash credit with ATM-enabled RuPay Kisan Card"
            ),
            benefitsHi = listOf(
                "जिला स्तरीय 'स्केल ऑफ फाइनेंस' के अनुसार फसल लागत का 100% वित्तपोषण",
                "समय पर चुकाने पर मात्र 4% प्रभावी वार्षिक ब्याज दर",
                "घरेलू खर्च हेतु 10% एवं कृषि यंत्र रखरखाव हेतु 20% अतिरिक्त ऋण सीमा",
                "ATM समर्थित रुपे किसान कार्ड से कभी भी नकद निकासी की सुविधा"
            ),
            requiredDocumentsEn = listOf(
                "Identity Proof (Aadhaar Card / Voter ID)",
                "Address Proof (Aadhaar / Ration Card / Domicile)",
                "Land Ownership Record (Khasra/Khatauni/7/12 or Tenancy Agreement)",
                "Crop Sowing Details / Cropping Pattern Declaration",
                "Passport-size Photograph"
            ),
            requiredDocumentsHi = listOf(
                "पहचान प्रमाण पत्र (आधार कार्ड / वोटर आईडी)",
                "निवास प्रमाण पत्र (आधार कार्ड / राशन कार्ड)",
                "भूमि स्वामित्व अभिलेख (खसरा / खतौनी / पट्टा समझौता)",
                "फसल बुवाई विवरण / स्व-घोषणा पत्र",
                "पासपोर्ट साइज रंगीन फोटो"
            ),
            applicationMethodEn = "Online via State Bank / JanSamarth Portal, or offline at any local bank branch or Common Service Centre (CSC).",
            applicationMethodHi = "जनसमर्थ पोर्टल / बैंक शाखा अथवा निकटतम जन सेवा केंद्र (CSC) के माध्यम से।",
            officialUrl = "https://www.myscheme.gov.in/schemes/kcc",
            source = "MyScheme (Ministry of Agriculture & Farmers Welfare, Govt of India)",
            lastVerified = "2026-08-15"
        ),

        LoanScheme(
            schemeId = "nabard_machinery_term_loan",
            nameEn = "Agricultural Term Loan for Farm Mechanization",
            nameHi = "कृषि यंत्रीकरण व ट्रैक्टर मियादी ऋण (Term Loan)",
            provider = "NABARD / Nationalized Commercial Banks",
            providerHi = "नाबार्ड / राष्ट्रीयकृत बैंक (SBI, Canara, BoB, etc.)",
            schemeType = "Medium & Long-Term Investment Credit",
            descriptionEn = "Term loans for purchase of new tractors, rotavators, power tillers, combine harvesters, and crop processing machinery to boost farm productivity.",
            descriptionHi = "खेत की उत्पादकता बढ़ाने हेतु नए ट्रैक्टर, रोटावेटर, पावर टिलर, थ्रेशर एवं अन्य कृषि यंत्र खरीदने हेतु मध्यम व दीर्घकालीन ऋण।",
            eligibleFarmerTypes = listOf(
                FarmerOwnershipType.OWN_LAND,
                FarmerOwnershipType.LEASED
            ),
            eligibleStates = emptyList(),
            eligibleCrops = emptyList(),
            eligiblePurposes = listOf(
                LoanPurpose.MACHINERY
            ),
            minAreaAcres = 2.0,
            maxAreaAcres = 500.0,
            interestRateInfoEn = "8.5% - 10.5% p.a. depending on bank base lending rate; eligible for SMAM capital subsidy.",
            interestRateInfoHi = "8.5% से 10.5% वार्षिक (बैंक दर अनुसार); SMAM सरकारी सब्सिडी हेतु पात्र।",
            collateralInfoEn = "Hypothecation of purchased machinery + mortgage of land for loans exceeding ₹1.6 Lakhs.",
            collateralInfoHi = "खरीदे गए यंत्र का दृष्टिबंधक (Hypothecation) एवं ₹1.6 लाख से अधिक पर भूमि बंधक।",
            benefitsEn = listOf(
                "Financing up to 85% - 90% of total machinery invoice cost",
                "Repayment period of 5 to 9 years in half-yearly/annual crop-harvest installments",
                "Subsidies up to 40% - 50% available under SMAM government scheme"
            ),
            benefitsHi = listOf(
                "कृषि यंत्र की कुल कीमत का 85% से 90% तक बैंक वित्तपोषण",
                "फसल कटाई पर आधारित 5 से 9 वर्षों में आसान अर्धवार्षिक किस्तें",
                "SMAM योजना के तहत 40% से 50% तक सरकारी सब्सिडी का सीधा लाभ"
            ),
            requiredDocumentsEn = listOf(
                "Identity Proof (Aadhaar / Voter ID)",
                "Land Records showing minimum 2 acres landholding",
                "Proforma Invoice / Quotation from authorized machinery dealer",
                "Bank Account Statement for last 6 months"
            ),
            requiredDocumentsHi = listOf(
                "पहचान प्रमाण (आधार / वोटर आईडी)",
                "न्यूनतम 2 एकड़ कृषि भूमि का खसरा/खतौनी",
                "अधिकृत ट्रैक्टर/यंत्र डीलर से कोटेशन (Proforma Invoice)",
                "पिछले 6 माह का बैंक खाता विवरण"
            ),
            applicationMethodEn = "Apply directly at authorized farm machinery dealerships or nearest commercial bank branch.",
            applicationMethodHi = "अधिकृत कृषि यंत्र डीलर अथवा निकटतम बैंक शाखा में सीधे संपर्क करें।",
            officialUrl = "https://agrimachinery.nic.in",
            source = "Sub-Mission on Agricultural Mechanization (SMAM), DAC&FW",
            lastVerified = "2026-08-20"
        ),

        LoanScheme(
            schemeId = "pm_kusum_irrigation_loan",
            nameEn = "PM-KUSUM Solar Agricultural Pump & Irrigation Loan",
            nameHi = "प्रधानमंत्री कुसुम सोलर पंप व सिंचाई ऋण योजना",
            provider = "Ministry of New and Renewable Energy (MNRE) & Lead District Banks",
            providerHi = "नवीन एवं नवीकरणीय ऊर्जा मंत्रालय (MNRE) एवं अग्रणी बैंक",
            schemeType = "Clean Energy Agricultural Infrastructure",
            descriptionEn = "Financial assistance and bank loan for installation of standalone off-grid solar water pumps or solarization of existing grid-connected agricultural tube wells.",
            descriptionHi = "खेतों में सिंचाई हेतु सोलर पंप लगाने अथवा पुराने डीजल पंप को सौर ऊर्जा पंप में बदलने हेतु 60% सरकारी अनुदान एवं रियायती बैंक ऋण।",
            eligibleFarmerTypes = listOf(
                FarmerOwnershipType.OWN_LAND,
                FarmerOwnershipType.LEASED
            ),
            eligibleStates = emptyList(),
            eligibleCrops = emptyList(),
            eligiblePurposes = listOf(
                LoanPurpose.IRRIGATION
            ),
            minAreaAcres = 0.5,
            maxAreaAcres = 100.0,
            interestRateInfoEn = "Priority Sector Lending (PSL) rates (~7.5% - 9.0% p.a.); 60% capital subsidy from Central & State Govt.",
            interestRateInfoHi = "प्राथमिकता क्षेत्र दरें (7.5% - 9%); केंद्र व राज्य सरकार द्वारा 60% सीधा अनुदान। किसान को मात्र 10% नकद देना होता है।",
            collateralInfoEn = "Hypothecation of solar pump equipment and panel structure.",
            collateralInfoHi = "सोलर पंप सेट एवं पैनल स्ट्रक्चर का बैंक दृष्टिबंधक।",
            benefitsEn = listOf(
                "60% combined subsidy (30% Central + 30% State Govt)",
                "Farmer needs to invest only 10% upfront; remaining 30% available as bank loan",
                "Zero recurring electricity bills or diesel expenditure for 25 years"
            ),
            benefitsHi = listOf(
                "कुल लागत पर 60% भारी सब्सिडी (30% केंद्र + 30% राज्य सरकार)",
                "किसान को केवल 10% लागत देनी होती है; शेष 30% बैंक ऋण में उपलब्ध",
                "अगले 25 वर्षों तक सिंचाई हेतु शून्य बिजली बिल व डीजल से पूर्ण मुक्ति"
            ),
            requiredDocumentsEn = listOf(
                "Aadhaar Card",
                "Land Ownership Record (Khatauni) with water source feasibility certificate",
                "Bank Passbook copy",
                "Electricity discom NOC (if replacing grid pump)"
            ),
            requiredDocumentsHi = listOf(
                "आधार कार्ड",
                "भूमि स्वामित्व दस्तावेज (खतौनी) एवं जल स्रोत प्रमाण",
                "बैंक पासबुक की प्रति",
                "बिजली विभाग से एनओसी (यदि ग्रिड पंप बदल रहे हैं)"
            ),
            applicationMethodEn = "Apply online through respective State Renewable Energy Development Agency portal.",
            applicationMethodHi = "राज्य ऊर्जा विकास निगम (REDA/KUSUM) के आधिकारिक ऑनलाइन पोर्टल से।",
            officialUrl = "https://pmkusum.mnre.gov.in",
            source = "Ministry of New and Renewable Energy (Govt of India)",
            lastVerified = "2026-08-10"
        ),

        LoanScheme(
            schemeId = "dairy_allied_kcc",
            nameEn = "KCC for Animal Husbandry, Dairy & Fisheries",
            nameHi = "पशुपालन, डेयरी एवं मत्स्य पालन किसान क्रेडिट कार्ड",
            provider = "All Commercial, Cooperative & Regional Rural Banks",
            providerHi = "सभी वाणिज्यिक, सहकारी एवं क्षेत्रीय ग्रामीण बैंक",
            schemeType = "Short-Term Working Capital for Allied Agriculture",
            descriptionEn = "Working capital credit for livestock maintenance, cattle feed, veterinary medicine, breeding, and dairy operational expenses.",
            descriptionHi = "दुधारू पशुओं के चारे, आहार, दवाइयों एवं डेयरी व्यवसाय के दैनिक खर्चों हेतु विशेष किसान क्रेडिट कार्ड ऋण।",
            eligibleFarmerTypes = listOf(
                FarmerOwnershipType.OWN_LAND,
                FarmerOwnershipType.TENANT,
                FarmerOwnershipType.SHARECROPPER,
                FarmerOwnershipType.LEASED
            ),
            eligibleStates = emptyList(),
            eligibleCrops = emptyList(),
            eligiblePurposes = listOf(
                LoanPurpose.ALLIED
            ),
            minAreaAcres = 0.0,
            maxAreaAcres = 500.0,
            interestRateInfoEn = "7.0% p.a. base; effective 4.0% p.a. with 3% prompt repayment subvention up to ₹2 Lakhs.",
            interestRateInfoHi = "7% वार्षिक आधार दर; ₹2 लाख तक समय पर भुगतान करने पर 3% छूट सहित मात्र 4% वार्षिक ब्याज दर।",
            collateralInfoEn = "Collateral-free up to ₹1.60 Lakhs (combined KCC limit).",
            collateralInfoHi = "₹1.60 लाख तक बिना किसी जमानत के ऋण उपलब्ध।",
            benefitsEn = listOf(
                "Working capital limit per milch cow (~₹44,000) or buffalo (~₹61,000)",
                "No agricultural land ownership required (accessible to landless livestock keepers)",
                "ATM-enabled RuPay debit card for instant feed and medicine purchase"
            ),
            benefitsHi = listOf(
                "प्रति दुधारू गाय (~₹44,000) एवं प्रति भैंस (~₹61,000) कार्यशील पूंजी",
                "भूमि का मालिक होना अनिवार्य नहीं (भूमिहीन पशुपालक भी पात्र)",
                "चारा व दवा खरीदने हेतु एटीएम युक्त रुपे कार्ड की सुविधा"
            ),
            requiredDocumentsEn = listOf(
                "Aadhaar Card",
                "Animal Tagging / Health Certificate from Government Veterinary Doctor",
                "Bank Account Details",
                "Passport Photograph"
            ),
            requiredDocumentsHi = listOf(
                "आधार कार्ड",
                "पशु का सरकारी टैग नंबर एवं पशु चिकित्सा अधिकारी से स्वास्थ्य प्रमाण",
                "बैंक खाता विवरण",
                "पासपोर्ट साइज फोटो"
            ),
            applicationMethodEn = "Submit simplified common application form at local bank branch or veterinary hospital.",
            applicationMethodHi = "निकटतम बैंक शाखा अथवा राजकीय पशु चिकित्सालय में आवेदन जमा करें।",
            officialUrl = "https://dahd.nic.in/schemes/kcc",
            source = "Department of Animal Husbandry & Dairying, Govt of India",
            lastVerified = "2026-08-18"
        ),

        LoanScheme(
            schemeId = "produce_marketing_warehouse_loan",
            nameEn = "Produce Marketing & Electronic Warehouse (e-NWR) Loan",
            nameHi = "फसल विपणन एवं वेयरहाउस रसीद (e-NWR) ऋण",
            provider = "Scheduled Commercial Banks / WDRA Accredited Warehouses",
            providerHi = "अनुसूचित बैंक / WDRA पंजीकृत वेयरहाउस",
            schemeType = "Pledge Financing Against Harvested Agri Produce",
            descriptionEn = "Short-term post-harvest pledge loan against accredited warehouse receipts to prevent distress selling immediately after harvest when market prices are low.",
            descriptionHi = "कटाई के तुरंत बाद सस्ते भाव में फसल बेचने से बचने हेतु वेयरहाउस में जमा उपज की रसीद पर तुरंत नकद ऋण।",
            eligibleFarmerTypes = listOf(
                FarmerOwnershipType.OWN_LAND,
                FarmerOwnershipType.TENANT,
                FarmerOwnershipType.SHARECROPPER,
                FarmerOwnershipType.LEASED
            ),
            eligibleStates = emptyList(),
            eligibleCrops = listOf("Wheat", "Rice", "Maize", "Mustard", "Soybean", "Gram", "Cotton"),
            eligiblePurposes = listOf(
                LoanPurpose.STORAGE,
                LoanPurpose.POST_HARVEST
            ),
            minAreaAcres = 0.5,
            maxAreaAcres = 500.0,
            interestRateInfoEn = "7.0% p.a. under interest subvention for smallholders up to 6 months post-harvest.",
            interestRateInfoHi = "छोटे व सीमांत किसानों को 6 महीने तक 7% रियायती ब्याज दर पर ऋण।",
            collateralInfoEn = "Pledge of Electronic Negotiable Warehouse Receipt (e-NWR); no physical land mortgage.",
            collateralInfoHi = "केवल वेयरहाउस की इलेक्ट्रॉनिक रसीद (e-NWR) पर ऋण; जमीन गिरवी रखने की आवश्यकता नहीं।",
            benefitsEn = listOf(
                "Financing up to 75% of current market value of deposited produce",
                "Enables farmers to hold crop and sell when mandi rates rise later in the season",
                "Up to 12 months repayment flexibility linked to commodity sale"
            ),
            benefitsHi = listOf(
                "वेयरहाउस में रखी फसल के बाजार मूल्य का 75% तक तुरंत नकद भुगतान",
                "मंडी में उचित भाव आने तक उपज रोककर रखने और अधिक लाभ कमाने का अवसर",
                "फसल बिकते ही ऋण खाते से सीधी कटौती की पारदर्शी सुविधा"
            ),
            requiredDocumentsEn = listOf(
                "Aadhaar Card",
                "e-NWR Warehouse Receipt from WDRA accredited cold storage/warehouse",
                "Farmer Bank Account Passbook"
            ),
            requiredDocumentsHi = listOf(
                "आधार कार्ड",
                "मान्यता प्राप्त गोदाम/वेयरहाउस से जारी इलेक्ट्रॉनिक रसीद (e-NWR)",
                "बैंक पासबुक की प्रति"
            ),
            applicationMethodEn = "Apply directly at the WDRA-registered warehouse or affiliated bank branch.",
            applicationMethodHi = "संबंधित वेयरहाउस अथवा बैंक शाखा में रसीद प्रस्तुत करके।",
            officialUrl = "https://wdra.gov.in",
            source = "Warehousing Development and Regulatory Authority (WDRA)",
            lastVerified = "2026-08-12"
        ),

        LoanScheme(
            schemeId = "jlg_tenant_microcredit",
            nameEn = "Joint Liability Group (JLG) Micro-Credit for Tenant Farmers",
            nameHi = "संयुक्त देयता समूह (JLG) काश्तकार एवं बटाईदार ऋण",
            provider = "NABARD / Regional Rural Banks (RRBs) & Microfinance Institutions",
            providerHi = "नाबार्ड / क्षेत्रीय ग्रामीण बैंक (RRBs) एवं सहकारी समितियां",
            schemeType = "Collateral-Free Group Credit for Landless & Oral Lessees",
            descriptionEn = "Group-based collateral-free credit designed specifically for tenant farmers, sharecroppers, and landless agricultural labourers who lack formal land title documents.",
            descriptionHi = "भूमिहीन, बटाईदार एवं मौखिक पट्टे पर खेती करने वाले किसानों के 4 से 10 सदस्यों के समूह हेतु बिना किसी जमीन बंधक के विशेष कृषि ऋण।",
            eligibleFarmerTypes = listOf(
                FarmerOwnershipType.TENANT,
                FarmerOwnershipType.SHARECROPPER,
                FarmerOwnershipType.LEASED
            ),
            eligibleStates = emptyList(),
            eligibleCrops = emptyList(),
            eligiblePurposes = listOf(
                LoanPurpose.SEEDS,
                LoanPurpose.FERTILIZER,
                LoanPurpose.CULTIVATION
            ),
            minAreaAcres = 0.1,
            maxAreaAcres = 10.0,
            interestRateInfoEn = "7.0% - 9.0% p.a. depending on lending RRB; eligible for KCC subvention benefits.",
            interestRateInfoHi = "7% से 9% वार्षिक; समय पर अदायगी पर KCC ब्याज छूट के समान लाभ।",
            collateralInfoEn = "100% Collateral-Free; based on mutual social guarantee of 4 to 10 group members.",
            collateralInfoHi = "100% बिना जमानत का ऋण; समूह के 4 से 10 किसानों की आपसी सामाजिक गारंटी पर आधारित।",
            benefitsEn = listOf(
                "Up to ₹50,000 per member or up to ₹5 Lakhs per Joint Liability Group",
                "No requirement for Khasra/Khatauni or formal land ownership documents",
                "Empowers informal tenants to access formal institutional banking credit"
            ),
            benefitsHi = listOf(
                "प्रति किसान ₹50,000 तक अथवा प्रति समूह ₹5,00,000 तक की ऋण सुविधा",
                "जमीन के पक्के कागजात या खतौनी की कोई आवश्यकता नहीं",
                "साहूकारों के ऊंचे ब्याज से मुक्ति एवं सरकारी बैंकिंग प्रणाली से सीधा जुड़ाव"
            ),
            requiredDocumentsEn = listOf(
                "Aadhaar Card of all 4-10 group members",
                "Mutual JLG Agreement (standard bank format)",
                "Local Village Gram Pradhan / Panchayat confirmation of oral cultivation"
            ),
            requiredDocumentsHi = listOf(
                "समूह के सभी सदस्यों के आधार कार्ड",
                "बैंक द्वारा प्रदत्त साधारण समूह अनुबंध पत्र",
                "ग्राम प्रधान / पंचायत द्वारा काश्तकारी का सामान्य सत्यापन"
            ),
            applicationMethodEn = "Form a group of 4-10 local farmers and apply at the nearest Regional Rural Bank (RRB) or Primary Agricultural Credit Society (PACS).",
            applicationMethodHi = "4-10 किसानों का समूह बनाकर निकटतम ग्रामीण बैंक (RRB) अथवा PACS समिति में संपर्क करें।",
            officialUrl = "https://www.nabard.org/content1.aspx?id=516",
            source = "NABARD Policy Guidelines for JLG Financing",
            lastVerified = "2026-08-25"
        )
    )

    /**
     * Intelligent recommendation engine evaluating crop, cultivated area,
     * land ownership/tenancy, purpose, and farmer location.
     */
    fun getRecommendations(profile: FarmerLoanProfile): List<LoanRecommendation> {
        val areaInAcres = if (profile.isHectares) profile.area * 2.47105 else profile.area
        val recommendations = mutableListOf<LoanRecommendation>()

        for (scheme in verifiedSchemes) {
            var score = 0
            val reasonsEn = mutableListOf<String>()
            val reasonsHi = mutableListOf<String>()

            // 1. Ownership Match
            if (scheme.eligibleFarmerTypes.contains(profile.ownershipType)) {
                score += 25
                reasonsEn.add("Matches your land ownership status (${profile.ownershipType.labelEn})")
                reasonsHi.add("आपकी भूमि व्यवस्था (${profile.ownershipType.labelHi}) के लिए पूर्णतः पात्र")
            }

            // 2. Purpose Match
            val matchedPurposes = scheme.eligiblePurposes.filter { profile.selectedPurposes.contains(it) }
            if (matchedPurposes.isNotEmpty()) {
                score += 35
                val pEn = matchedPurposes.joinToString(", ") { it.labelEn }
                val pHi = matchedPurposes.joinToString(", ") { it.labelHi }
                reasonsEn.add("Directly finances your specified purpose: $pEn")
                reasonsHi.add("आपके चुने गए उद्देश्य हेतु अनुकूल: $pHi")
            }

            // 3. Area Compatibility
            if (areaInAcres >= scheme.minAreaAcres && areaInAcres <= scheme.maxAreaAcres) {
                score += 20
                reasonsEn.add("Compatible with your ${DecimalFormat("#.##").format(areaInAcres)} acre farm size")
                reasonsHi.add("आपके ${DecimalFormat("#.##").format(areaInAcres)} एकड़ जोत आकार के अनुरूप")
            }

            // 4. Crop Match (if scheme specifies crops, or generic for all crops)
            if (scheme.eligibleCrops.isEmpty()) {
                score += 10
                reasonsEn.add("Applicable for all cultivated crops including ${profile.selectedCrops.take(2).joinToString(", ")}")
                reasonsHi.add("आपकी फसलों (${profile.selectedCrops.take(2).joinToString(", ")}) पर मान्य")
            } else if (scheme.eligibleCrops.any { c -> profile.selectedCrops.any { it.contains(c, ignoreCase = true) } }) {
                score += 20
                reasonsEn.add("Specifically verified for your crop: ${profile.selectedCrops.joinToString(", ")}")
                reasonsHi.add("आपकी चयनित फसल (${profile.selectedCrops.joinToString(", ")}) हेतु विशेष रूप से स्वीकृत")
            }

            // 5. Special boost for smallholders & KCC
            if (scheme.schemeId == "kcc_crop_loan" && profile.selectedPurposes.any { it == LoanPurpose.CULTIVATION || it == LoanPurpose.SEEDS || it == LoanPurpose.FERTILIZER }) {
                score += 10
            }

            // 6. Special boost for tenant farmers on JLG
            if ((profile.ownershipType == FarmerOwnershipType.TENANT || profile.ownershipType == FarmerOwnershipType.SHARECROPPER) && scheme.schemeId == "jlg_tenant_microcredit") {
                score += 20
            }

            // Scale of finance approximation (e.g. ₹28,000 - ₹38,000 / acre for seasonal crops)
            val approxPerAcre = 32000.0
            val estKccLimit = (areaInAcres * approxPerAcre).coerceAtLeast(25000.0).coerceAtMost(300000.0)

            val (limitTextEn, limitTextHi) = when (scheme.schemeId) {
                "kcc_crop_loan" -> {
                    Pair(
                        "Estimated Scale of Finance limit: ~₹${df.format(estKccLimit)} (Subject to district scale & bank approval)",
                        "अनुमानित वित्त पैमाना: ~₹${df.format(estKccLimit)} (जिला स्तरीय स्केल एवं बैंक अनुमोदन अनुसार)"
                    )
                }
                "nabard_machinery_term_loan" -> {
                    Pair(
                        "Up to 85% of machinery invoice value (Up to ₹8 - 12 Lakhs for tractors)",
                        "कृषि यंत्र की इनवॉइस का 85% तक (ट्रैक्टर हेतु ₹8 से 12 लाख तक)"
                    )
                }
                "pm_kusum_irrigation_loan" -> {
                    Pair(
                        "30% bank loan + 60% Govt subsidy (Farmer pays only 10% cash)",
                        "30% बैंक ऋण + 60% सरकारी सब्सिडी (किसान अंश मात्र 10%)"
                    )
                }
                "dairy_allied_kcc" -> {
                    Pair(
                        "Up to ₹2.0 Lakhs collateral-free credit limit for livestock",
                        "दुधारू पशुओं हेतु ₹2 लाख तक बिना जमानत ऋण सीमा"
                    )
                }
                "produce_marketing_warehouse_loan" -> {
                    Pair(
                        "Up to 75% of market value of deposited produce",
                        "गोदाम में रखी फसल के बाजार मूल्य का 75% तक"
                    )
                }
                else -> {
                    Pair(
                        "Final loan amount will be decided by the bank based on eligibility and assessment.",
                        "अंतिम ऋण राशि का निर्धारण बैंक द्वारा पात्रता व मूल्यांकन के आधार पर किया जाएगा।"
                    )
                }
            }

            val level = when {
                score >= 70 -> MatchLevel.HIGH
                score >= 45 -> MatchLevel.MEDIUM
                else -> MatchLevel.POTENTIAL
            }

            recommendations.add(
                LoanRecommendation(
                    scheme = scheme,
                    matchLevel = level,
                    matchScore = score.coerceIn(20, 98),
                    matchReasonsEn = reasonsEn,
                    matchReasonsHi = reasonsHi,
                    estimatedCreditLimitTextEn = limitTextEn,
                    estimatedCreditLimitTextHi = limitTextHi
                )
            )
        }

        // Rank highest match first
        return recommendations.sortedByDescending { it.matchScore }
    }

    // Save and load applications
    fun saveApplication(submission: LoanApplicationSubmission) {
        try {
            val list = getSavedApplications().toMutableList()
            list.removeAll { it.applicationId == submission.applicationId }
            list.add(0, submission)
            val json = Gson().toJson(list)
            context.getSharedPreferences("krishi_loan_apps", Context.MODE_PRIVATE)
                .edit()
                .putString("saved_apps", json)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSavedApplications(): List<LoanApplicationSubmission> {
        return try {
            val json = context.getSharedPreferences("krishi_loan_apps", Context.MODE_PRIVATE)
                .getString("saved_apps", null) ?: return emptyList()
            val type = object : TypeToken<List<LoanApplicationSubmission>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
