package com.krishimitra.app.data.local

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.krishimitra.app.domain.model.Crop
import com.krishimitra.app.domain.model.CropVariety
import com.krishimitra.app.domain.model.Disease
import com.krishimitra.app.domain.model.FarmerExperience
import com.krishimitra.app.domain.model.Loan
import com.krishimitra.app.domain.model.MarketPrice
import com.krishimitra.app.domain.model.RAGKnowledgeRecord
import com.krishimitra.app.domain.model.Scheme

import java.io.File
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "krishi_knowledge.db"
        const val DB_VERSION = 4
        private const val TAG = "DatabaseHelper"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        ensureDatabaseExists()
    }

    private fun ensureDatabaseExists() {
        val dbFile = context.getDatabasePath(DB_NAME)
        var needsCopy = !dbFile.exists()

        if (dbFile.exists()) {
            try {
                SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    val cTable = db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='market_prices'", null)
                    val hasMarketTable = cTable.moveToFirst() && cTable.getInt(0) > 0
                    cTable.close()

                    val cRag = db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='rag_knowledge'", null)
                    val hasRagTable = cRag.moveToFirst() && cRag.getInt(0) > 0
                    cRag.close()

                    val cCrops = db.rawQuery("SELECT count(*) FROM crops", null)
                    val cropCount = if (cCrops.moveToFirst()) cCrops.getInt(0) else 0
                    cCrops.close()

                    val ragCount = if (hasRagTable) {
                        val cCount = db.rawQuery("SELECT count(*) FROM rag_knowledge", null)
                        val cnt = if (cCount.moveToFirst()) cCount.getInt(0) else 0
                        cCount.close()
                        cnt
                    } else 0

                    if (!hasMarketTable || !hasRagTable || cropCount < 40 || ragCount < 400) {
                        Log.i(TAG, "Existing local database is outdated (hasMarketTable=$hasMarketTable, hasRagTable=$hasRagTable, cropCount=$cropCount, ragCount=$ragCount). Refreshing from assets.")
                        needsCopy = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking existing database: ${e.message}, refreshing from assets.")
                needsCopy = true
            }
        }

        if (needsCopy) {
            try {
                dbFile.delete()
                File("${dbFile.path}-wal").delete()
                File("${dbFile.path}-shm").delete()
                File("${dbFile.path}-journal").delete()
            } catch (ignored: Exception) {}

            dbFile.parentFile?.mkdirs()
            try {
                context.assets.open(DB_NAME).use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Copied updated pre-seeded knowledge database from assets successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy database from assets: ${e.message}", e)
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Database is copied pre-populated from assets
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (newVersion > oldVersion) {
            val dbFile = context.getDatabasePath(DB_NAME)
            try {
                context.assets.open(DB_NAME).use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Upgraded local database to version $newVersion from assets.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upgrade database: ${e.message}")
            }
        }
    }

    fun getAllCrops(): List<Crop> {
        val list = mutableListOf<Crop>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM crops ORDER BY name_en ASC", null)
            while (cursor.moveToNext()) {
                list.add(
                    Crop(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        nameEn = cursor.getString(cursor.getColumnIndexOrThrow("name_en")),
                        nameHi = cursor.getString(cursor.getColumnIndexOrThrow("name_hi")),
                        scientificName = cursor.getString(cursor.getColumnIndexOrThrow("scientific_name")),
                        category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        categoryHi = cursor.getString(cursor.getColumnIndexOrThrow("category_hi")),
                        soil = cursor.getString(cursor.getColumnIndexOrThrow("soil")),
                        soilHi = cursor.getString(cursor.getColumnIndexOrThrow("soil_hi")),
                        soilPh = cursor.getString(cursor.getColumnIndexOrThrow("soil_ph")),
                        climate = cursor.getString(cursor.getColumnIndexOrThrow("climate")),
                        climateHi = cursor.getString(cursor.getColumnIndexOrThrow("climate_hi")),
                        temperature = cursor.getString(cursor.getColumnIndexOrThrow("temperature")),
                        sowingSeason = cursor.getString(cursor.getColumnIndexOrThrow("sowing_season")),
                        sowingSeasonHi = cursor.getString(cursor.getColumnIndexOrThrow("sowing_season_hi")),
                        irrigation = cursor.getString(cursor.getColumnIndexOrThrow("irrigation")),
                        irrigationHi = cursor.getString(cursor.getColumnIndexOrThrow("irrigation_hi")),
                        fertilizer = cursor.getString(cursor.getColumnIndexOrThrow("fertilizer")),
                        fertilizerHi = cursor.getString(cursor.getColumnIndexOrThrow("fertilizer_hi")),
                        harvesting = cursor.getString(cursor.getColumnIndexOrThrow("harvesting")),
                        harvestingHi = cursor.getString(cursor.getColumnIndexOrThrow("harvesting_hi")),
                        pests = cursor.getString(cursor.getColumnIndexOrThrow("pests")),
                        pestsHi = cursor.getString(cursor.getColumnIndexOrThrow("pests_hi")),
                        diseases = cursor.getString(cursor.getColumnIndexOrThrow("diseases")),
                        diseasesHi = cursor.getString(cursor.getColumnIndexOrThrow("diseases_hi")),
                        cultivationTips = cursor.getString(cursor.getColumnIndexOrThrow("cultivation_tips")),
                        cultivationTipsHi = cursor.getString(cursor.getColumnIndexOrThrow("cultivation_tips_hi")),
                        source = cursor.getString(cursor.getColumnIndexOrThrow("source")),
                        sourceUrl = cursor.getString(cursor.getColumnIndexOrThrow("source_url"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching crops: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun getAllDiseases(): List<Disease> {
        val list = mutableListOf<Disease>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM diseases ORDER BY crop ASC", null)
            while (cursor.moveToNext()) {
                list.add(
                    Disease(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        crop = cursor.getString(cursor.getColumnIndexOrThrow("crop")),
                        cropHi = cursor.getString(cursor.getColumnIndexOrThrow("crop_hi")),
                        diseaseNameEn = cursor.getString(cursor.getColumnIndexOrThrow("disease_name_en")),
                        diseaseNameHi = cursor.getString(cursor.getColumnIndexOrThrow("disease_name_hi")),
                        pathogen = cursor.getString(cursor.getColumnIndexOrThrow("pathogen")),
                        symptomsEn = cursor.getString(cursor.getColumnIndexOrThrow("symptoms_en")),
                        symptomsHi = cursor.getString(cursor.getColumnIndexOrThrow("symptoms_hi")),
                        causesEn = cursor.getString(cursor.getColumnIndexOrThrow("causes_en")),
                        causesHi = cursor.getString(cursor.getColumnIndexOrThrow("causes_hi")),
                        treatmentOrganicEn = cursor.getString(cursor.getColumnIndexOrThrow("treatment_organic_en")),
                        treatmentOrganicHi = cursor.getString(cursor.getColumnIndexOrThrow("treatment_organic_hi")),
                        treatmentChemicalEn = cursor.getString(cursor.getColumnIndexOrThrow("treatment_chemical_en")),
                        treatmentChemicalHi = cursor.getString(cursor.getColumnIndexOrThrow("treatment_chemical_hi")),
                        preventionEn = cursor.getString(cursor.getColumnIndexOrThrow("prevention_en")),
                        preventionHi = cursor.getString(cursor.getColumnIndexOrThrow("prevention_hi")),
                        confidenceThreshold = cursor.getFloat(cursor.getColumnIndexOrThrow("confidence_threshold"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching diseases: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun getDiseaseById(diseaseId: String): Disease? {
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM diseases WHERE id = ? LIMIT 1", arrayOf(diseaseId))
            if (cursor.moveToFirst()) {
                return Disease(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    crop = cursor.getString(cursor.getColumnIndexOrThrow("crop")),
                    cropHi = cursor.getString(cursor.getColumnIndexOrThrow("crop_hi")),
                    diseaseNameEn = cursor.getString(cursor.getColumnIndexOrThrow("disease_name_en")),
                    diseaseNameHi = cursor.getString(cursor.getColumnIndexOrThrow("disease_name_hi")),
                    pathogen = cursor.getString(cursor.getColumnIndexOrThrow("pathogen")),
                    symptomsEn = cursor.getString(cursor.getColumnIndexOrThrow("symptoms_en")),
                    symptomsHi = cursor.getString(cursor.getColumnIndexOrThrow("symptoms_hi")),
                    causesEn = cursor.getString(cursor.getColumnIndexOrThrow("causes_en")),
                    causesHi = cursor.getString(cursor.getColumnIndexOrThrow("causes_hi")),
                    treatmentOrganicEn = cursor.getString(cursor.getColumnIndexOrThrow("treatment_organic_en")),
                    treatmentOrganicHi = cursor.getString(cursor.getColumnIndexOrThrow("treatment_organic_hi")),
                    treatmentChemicalEn = cursor.getString(cursor.getColumnIndexOrThrow("treatment_chemical_en")),
                    treatmentChemicalHi = cursor.getString(cursor.getColumnIndexOrThrow("treatment_chemical_hi")),
                    preventionEn = cursor.getString(cursor.getColumnIndexOrThrow("prevention_en")),
                    preventionHi = cursor.getString(cursor.getColumnIndexOrThrow("prevention_hi")),
                    confidenceThreshold = cursor.getFloat(cursor.getColumnIndexOrThrow("confidence_threshold"))
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching disease $diseaseId: ${e.message}")
        } finally {
            cursor?.close()
        }
        return null
    }

    fun getAllSchemes(): List<Scheme> {
        val list = mutableListOf<Scheme>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM schemes ORDER BY name_en ASC", null)
            while (cursor.moveToNext()) {
                list.add(
                    Scheme(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        nameEn = cursor.getString(cursor.getColumnIndexOrThrow("name_en")),
                        nameHi = cursor.getString(cursor.getColumnIndexOrThrow("name_hi")),
                        category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        categoryHi = cursor.getString(cursor.getColumnIndexOrThrow("category_hi")),
                        ministry = cursor.getString(cursor.getColumnIndexOrThrow("ministry")),
                        benefitsEn = cursor.getString(cursor.getColumnIndexOrThrow("benefits_en")),
                        benefitsHi = cursor.getString(cursor.getColumnIndexOrThrow("benefits_hi")),
                        eligibilityEn = cursor.getString(cursor.getColumnIndexOrThrow("eligibility_en")),
                        eligibilityHi = cursor.getString(cursor.getColumnIndexOrThrow("eligibility_hi")),
                        applicationProcessEn = cursor.getString(cursor.getColumnIndexOrThrow("application_process_en")),
                        applicationProcessHi = cursor.getString(cursor.getColumnIndexOrThrow("application_process_hi")),
                        officialUrl = cursor.getString(cursor.getColumnIndexOrThrow("official_url")),
                        source = cursor.getString(cursor.getColumnIndexOrThrow("source")),
                        lastVerified = cursor.getString(cursor.getColumnIndexOrThrow("last_verified"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching schemes: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun getAllLoans(): List<Loan> {
        val list = mutableListOf<Loan>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM loans ORDER BY bank_name ASC", null)
            while (cursor.moveToNext()) {
                list.add(
                    Loan(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        bankName = cursor.getString(cursor.getColumnIndexOrThrow("bank_name")),
                        bankNameHi = cursor.getString(cursor.getColumnIndexOrThrow("bank_name_hi")),
                        loanType = cursor.getString(cursor.getColumnIndexOrThrow("loan_type")),
                        loanTypeHi = cursor.getString(cursor.getColumnIndexOrThrow("loan_type_hi")),
                        purposeEn = cursor.getString(cursor.getColumnIndexOrThrow("purpose_en")),
                        purposeHi = cursor.getString(cursor.getColumnIndexOrThrow("purpose_hi")),
                        interestRate = cursor.getString(cursor.getColumnIndexOrThrow("interest_rate")),
                        interestRateHi = cursor.getString(cursor.getColumnIndexOrThrow("interest_rate_hi")),
                        maxLimit = cursor.getString(cursor.getColumnIndexOrThrow("max_limit")),
                        maxLimitHi = cursor.getString(cursor.getColumnIndexOrThrow("max_limit_hi")),
                        eligibilityEn = cursor.getString(cursor.getColumnIndexOrThrow("eligibility_en")),
                        eligibilityHi = cursor.getString(cursor.getColumnIndexOrThrow("eligibility_hi")),
                        documentsRequired = cursor.getString(cursor.getColumnIndexOrThrow("documents_required")),
                        documentsRequiredHi = cursor.getString(cursor.getColumnIndexOrThrow("documents_required_hi")),
                        officialUrl = cursor.getString(cursor.getColumnIndexOrThrow("official_url")),
                        source = cursor.getString(cursor.getColumnIndexOrThrow("source")),
                        lastVerified = cursor.getString(cursor.getColumnIndexOrThrow("last_verified"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching loans: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun getAllFarmerExperiences(): List<FarmerExperience> {
        val list = mutableListOf<FarmerExperience>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM farmer_experiences ORDER BY created_at DESC", null)
            while (cursor.moveToNext()) {
                val obsHiIdx = cursor.getColumnIndex("observation_hi")
                val obsEnIdx = cursor.getColumnIndex("observation_en")
                list.add(
                    FarmerExperience(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        cropId = cursor.getString(cursor.getColumnIndexOrThrow("crop_id")),
                        cropName = cursor.getString(cursor.getColumnIndexOrThrow("crop_name")),
                        state = cursor.getString(cursor.getColumnIndexOrThrow("state")),
                        district = cursor.getString(cursor.getColumnIndexOrThrow("district")),
                        observation = cursor.getString(cursor.getColumnIndexOrThrow("observation")),
                        observationHi = if (obsHiIdx >= 0) cursor.getString(obsHiIdx) else null,
                        observationEn = if (obsEnIdx >= 0) cursor.getString(obsEnIdx) else null,
                        language = cursor.getString(cursor.getColumnIndexOrThrow("language")),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching farmer experiences: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun getFarmerExperiencesByCrop(cropId: String): List<FarmerExperience> {
        val list = mutableListOf<FarmerExperience>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM farmer_experiences WHERE crop_id = ? ORDER BY created_at DESC", arrayOf(cropId))
            while (cursor.moveToNext()) {
                val obsHiIdx = cursor.getColumnIndex("observation_hi")
                val obsEnIdx = cursor.getColumnIndex("observation_en")
                list.add(
                    FarmerExperience(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        cropId = cursor.getString(cursor.getColumnIndexOrThrow("crop_id")),
                        cropName = cursor.getString(cursor.getColumnIndexOrThrow("crop_name")),
                        state = cursor.getString(cursor.getColumnIndexOrThrow("state")),
                        district = cursor.getString(cursor.getColumnIndexOrThrow("district")),
                        observation = cursor.getString(cursor.getColumnIndexOrThrow("observation")),
                        observationHi = if (obsHiIdx >= 0) cursor.getString(obsHiIdx) else null,
                        observationEn = if (obsEnIdx >= 0) cursor.getString(obsEnIdx) else null,
                        language = cursor.getString(cursor.getColumnIndexOrThrow("language")),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching farmer experiences for $cropId: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun insertFarmerExperience(exp: FarmerExperience): Boolean {
        val db = writableDatabase
        return try {
            val values = android.content.ContentValues().apply {
                put("id", exp.id)
                put("crop_id", exp.cropId)
                put("crop_name", exp.cropName)
                put("state", exp.state)
                put("district", exp.district)
                put("observation", exp.observation)
                put("language", exp.language)
                put("created_at", exp.createdAt)
            }
            db.insertWithOnConflict("farmer_experiences", null, values, SQLiteDatabase.CONFLICT_REPLACE) > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting farmer experience: ${e.message}")
            false
        }
    }

    fun getAllRAGKnowledge(): List<RAGKnowledgeRecord> {
        val list = mutableListOf<RAGKnowledgeRecord>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM rag_knowledge ORDER BY id ASC", null)
            while (cursor.moveToNext()) {
                list.add(
                    RAGKnowledgeRecord(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        topic = cursor.getString(cursor.getColumnIndexOrThrow("topic")),
                        cropId = cursor.getString(cursor.getColumnIndexOrThrow("crop_id")),
                        questionEn = cursor.getString(cursor.getColumnIndexOrThrow("question_en")),
                        questionHi = cursor.getString(cursor.getColumnIndexOrThrow("question_hi")),
                        answerEn = cursor.getString(cursor.getColumnIndexOrThrow("answer_en")),
                        answerHi = cursor.getString(cursor.getColumnIndexOrThrow("answer_hi")),
                        source = cursor.getString(cursor.getColumnIndexOrThrow("source")),
                        sourceUrl = cursor.getString(cursor.getColumnIndexOrThrow("source_url")),
                        isVerified = cursor.getInt(cursor.getColumnIndexOrThrow("is_verified")) == 1,
                        category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RAG knowledge: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun getLatestMarketPrice(cropId: String?, district: String? = null, market: String? = null): MarketPrice? {
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            val selectionClauses = mutableListOf<String>()
            val selectionArgs = mutableListOf<String>()

            if (!cropId.isNullOrBlank()) {
                selectionClauses.add("crop_id = ?")
                selectionArgs.add(cropId)
            }
            if (!district.isNullOrBlank()) {
                selectionClauses.add("district LIKE ?")
                selectionArgs.add("%$district%")
            }
            if (!market.isNullOrBlank()) {
                selectionClauses.add("market LIKE ?")
                selectionArgs.add("%$market%")
            }

            val where = if (selectionClauses.isNotEmpty()) selectionClauses.joinToString(" AND ") else null
            val args = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

            cursor = db.query(
                "market_prices",
                null,
                where,
                args,
                null,
                null,
                "price_date DESC, id DESC",
                "1"
            )

            if (cursor.moveToFirst()) {
                return MarketPrice(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cropId = cursor.getString(cursor.getColumnIndexOrThrow("crop_id")),
                    commodity = cursor.getString(cursor.getColumnIndexOrThrow("commodity")),
                    variety = cursor.getString(cursor.getColumnIndexOrThrow("variety")),
                    state = cursor.getString(cursor.getColumnIndexOrThrow("state")),
                    district = cursor.getString(cursor.getColumnIndexOrThrow("district")),
                    market = cursor.getString(cursor.getColumnIndexOrThrow("market")),
                    minPrice = cursor.getFloat(cursor.getColumnIndexOrThrow("min_price")),
                    maxPrice = cursor.getFloat(cursor.getColumnIndexOrThrow("max_price")),
                    modalPrice = cursor.getFloat(cursor.getColumnIndexOrThrow("modal_price")),
                    priceDate = cursor.getString(cursor.getColumnIndexOrThrow("price_date")),
                    unit = cursor.getString(cursor.getColumnIndexOrThrow("unit")),
                    source = cursor.getString(cursor.getColumnIndexOrThrow("source"))
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching market price: ${e.message}")
        } finally {
            cursor?.close()
        }
        return null
    }

    fun compareMarkets(cropId: String?): List<MarketPrice> {
        val list = mutableListOf<MarketPrice>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            val where = if (!cropId.isNullOrBlank()) "crop_id = ?" else null
            val args = if (!cropId.isNullOrBlank()) arrayOf(cropId) else null

            cursor = db.query(
                "market_prices",
                null,
                where,
                args,
                null,
                null,
                "modal_price DESC",
                "10"
            )

            while (cursor.moveToNext()) {
                list.add(
                    MarketPrice(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cropId = cursor.getString(cursor.getColumnIndexOrThrow("crop_id")),
                        commodity = cursor.getString(cursor.getColumnIndexOrThrow("commodity")),
                        variety = cursor.getString(cursor.getColumnIndexOrThrow("variety")),
                        state = cursor.getString(cursor.getColumnIndexOrThrow("state")),
                        district = cursor.getString(cursor.getColumnIndexOrThrow("district")),
                        market = cursor.getString(cursor.getColumnIndexOrThrow("market")),
                        minPrice = cursor.getFloat(cursor.getColumnIndexOrThrow("min_price")),
                        maxPrice = cursor.getFloat(cursor.getColumnIndexOrThrow("max_price")),
                        modalPrice = cursor.getFloat(cursor.getColumnIndexOrThrow("modal_price")),
                        priceDate = cursor.getString(cursor.getColumnIndexOrThrow("price_date")),
                        unit = cursor.getString(cursor.getColumnIndexOrThrow("unit")),
                        source = cursor.getString(cursor.getColumnIndexOrThrow("source"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing markets: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun getCropVarieties(cropId: String): List<CropVariety> {
        val list = mutableListOf<CropVariety>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM crop_varieties WHERE crop_id = ?", arrayOf(cropId))
            while (cursor.moveToNext()) {
                list.add(
                    CropVariety(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        cropId = cursor.getString(cursor.getColumnIndexOrThrow("crop_id")),
                        varietyName = cursor.getString(cursor.getColumnIndexOrThrow("variety_name")),
                        category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        durationDays = cursor.getString(cursor.getColumnIndexOrThrow("duration_days")),
                        yieldPotential = cursor.getString(cursor.getColumnIndexOrThrow("yield_potential")),
                        suitableZones = cursor.getString(cursor.getColumnIndexOrThrow("suitable_zones")),
                        specialFeatures = cursor.getString(cursor.getColumnIndexOrThrow("special_features")),
                        specialFeaturesHi = cursor.getString(cursor.getColumnIndexOrThrow("special_features_hi")),
                        source = cursor.getString(cursor.getColumnIndexOrThrow("source"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching varieties for $cropId: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun getAllMarketPrices(): List<MarketPrice> {
        val list = mutableListOf<MarketPrice>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM market_prices ORDER BY price_date DESC, modal_price DESC", null)
            while (cursor.moveToNext()) {
                list.add(
                    MarketPrice(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cropId = cursor.getString(cursor.getColumnIndexOrThrow("crop_id")),
                        commodity = cursor.getString(cursor.getColumnIndexOrThrow("commodity")),
                        variety = cursor.getString(cursor.getColumnIndexOrThrow("variety")),
                        state = cursor.getString(cursor.getColumnIndexOrThrow("state")),
                        district = cursor.getString(cursor.getColumnIndexOrThrow("district")),
                        market = cursor.getString(cursor.getColumnIndexOrThrow("market")),
                        minPrice = cursor.getFloat(cursor.getColumnIndexOrThrow("min_price")),
                        maxPrice = cursor.getFloat(cursor.getColumnIndexOrThrow("max_price")),
                        modalPrice = cursor.getFloat(cursor.getColumnIndexOrThrow("modal_price")),
                        priceDate = cursor.getString(cursor.getColumnIndexOrThrow("price_date")),
                        unit = cursor.getString(cursor.getColumnIndexOrThrow("unit")),
                        source = cursor.getString(cursor.getColumnIndexOrThrow("source"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all market prices: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }
}
