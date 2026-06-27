package com.example.tongji.data.repository

import com.example.tongji.auth.CredentialStore
import com.example.tongji.data.local.dao.CampusCardDao
import com.example.tongji.data.local.entity.CampusCardBalanceEntity
import com.example.tongji.data.local.entity.CampusCardTransactionEntity
import com.example.tongji.data.remote.api.AllTongjiApi
import com.example.tongji.data.remote.api.YikatongApi
import java.text.SimpleDateFormat
import java.util.*

class YikatongRepository(
    private val api: YikatongApi,
    private val allTongjiApi: AllTongjiApi,
    private val dao: CampusCardDao,
    private val credentialStore: CredentialStore
) {
    suspend fun syncBalance(): Result<CampusCardBalanceEntity> = runCatching {
        val uid = credentialStore.getString(CredentialStore.KEY_UID)
            ?: throw Exception("未登录，无法获取余额")
        val resp = allTongjiApi.getCardBalance(uid)
        val body = resp.body() ?: throw Exception("Empty response")
        val dataList = body["data"] as? List<Map<String, Any>>
        val data = dataList?.firstOrNull() ?: throw Exception("No balance data")

        val balance = CampusCardBalanceEntity(
            capturedAt = System.currentTimeMillis(),
            balanceYuan = (data["balance"] as? Number)?.toDouble() ?: 0.0,
            account = uid,
            ownerName = "",
            cardIdentifier = uid
        )
        dao.insertBalance(balance)
        balance
    }

    suspend fun syncTransactions(): Result<Unit> = runCatching {
        for (page in 1..7) {
            val resp = api.getTurnover(page = page, size = 100)
            val body = resp.body() ?: break
            val list = body["list"] as? List<Map<String, Any>>
                ?: (body["data"] as? List<Map<String, Any>>)
                ?: break
            if (list.isEmpty()) break
            val transactions = list.map { parseTransaction(it) }
            dao.insertTransactions(transactions)
        }
    }

    suspend fun getLatestBalance(): CampusCardBalanceEntity? = dao.getLatestBalance()
    suspend fun getRecentTransactions(limit: Int = 50) = dao.getRecentTransactions(limit)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    private fun parseTransaction(item: Map<String, Any>): CampusCardTransactionEntity {
        return CampusCardTransactionEntity(
            orderId = item["orderId"] as? String ?: UUID.randomUUID().toString(),
            transactionDateTime = parseDateTime(item["transactionDateTime"] as? String),
            amountYuan = (item["amount"] as? Number)?.toDouble() ?: 0.0,
            balanceYuan = (item["balance"] as? Number)?.toDouble() ?: 0.0,
            transactionDescription = item["description"] as? String ?: item["transactionDescription"] as? String ?: "",
            turnoverType = item["turnoverType"] as? String ?: item["type"] as? String ?: "",
            locationName = item["locationName"] as? String ?: item["location"] as? String ?: "",
            payName = item["payName"] as? String ?: item["merchant"] as? String ?: "",
            updatedAt = parseDateTime(item["updatedAt"] as? String)
        )
    }

    private fun parseDateTime(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return System.currentTimeMillis()
        return try {
            dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
