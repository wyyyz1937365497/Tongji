package com.example.tongji.data.repository

import android.util.Log
import com.example.tongji.auth.CredentialStore
import com.example.tongji.data.local.dao.CampusCardDao
import com.example.tongji.data.local.entity.CampusCardBalanceEntity
import com.example.tongji.data.local.entity.CampusCardTransactionEntity
import com.example.tongji.data.remote.api.YikatongApi
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "YikatongRepository"

class YikatongRepository(
    private val yikatongApi: YikatongApi,
    private val dao: CampusCardDao,
    private val credentialStore: CredentialStore
) {

    suspend fun syncBalance(): Result<CampusCardBalanceEntity> = runCatching {
        val cardInfoResp = yikatongApi.getCardInfo()
        val cardInfoBody = cardInfoResp.body() ?: throw Exception("Empty card info response")
        val account = parseCardAccount(cardInfoBody)
            ?: throw Exception("Failed to parse card account")

        val balanceResp = yikatongApi.getCardAccInfo(account)
        val balanceBody = balanceResp.body() ?: throw Exception("Empty balance response")
        val balanceYuan = parseBalance(balanceBody)
            ?: throw Exception("Failed to parse balance")

        val balance = CampusCardBalanceEntity(
            capturedAt = System.currentTimeMillis(),
            balanceYuan = balanceYuan,
            account = account,
            ownerName = "",
            cardIdentifier = account
        )
        dao.insertBalance(balance)
        Log.d(TAG, "余额同步成功: account=$account balance=$balanceYuan")
        balance
    }

    suspend fun syncTransactions(days: Int = 30): Result<Int> = runCatching {
        val cardInfoResp = yikatongApi.getCardInfo()
        val cardInfoBody = cardInfoResp.body() ?: throw Exception("Empty card info response")
        val account = parseCardAccount(cardInfoBody)
            ?: throw Exception("Failed to parse card account")

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val endDate = sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = sdf.format(cal.time)

        val resp = yikatongApi.getPersonTrjn(startDate, endDate, account, 1, 50)
        val body = resp.body() ?: throw Exception("Empty transaction response")

        val total = (body["total"] as? Number)?.toInt() ?: 0
        @Suppress("UNCHECKED_CAST")
        val rows = body["rows"] as? List<Map<String, Any>> ?: emptyList()

        val transactions = rows.mapNotNull { row -> parseTransactionRow(row, account) }
        if (transactions.isNotEmpty()) {
            dao.insertTransactions(transactions)
        }
        Log.d(TAG, "交易同步成功: total=$total parsed=${transactions.size}")
        transactions.size
    }

    suspend fun getLatestBalance(): CampusCardBalanceEntity? = dao.getLatestBalance()
    suspend fun getRecentTransactions(limit: Int = 50): List<CampusCardTransactionEntity> =
        dao.getRecentTransactions(limit)

    private fun parseCardAccount(body: Map<String, Any>): String? {
        return try {
            val msgStr = body["Msg"] as? String ?: return null
            val msgJson = JSONObject(msgStr)
            val queryCard = msgJson.optJSONObject("query_card") ?: return null
            val cardArray = queryCard.optJSONArray("card") ?: return null
            if (cardArray.length() == 0) return null
            cardArray.getJSONObject(0).optString("account").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "parseCardAccount failed: ${e.message}")
            null
        }
    }

    private fun parseBalance(body: Map<String, Any>): Double? {
        return try {
            val msgStr = body["Msg"] as? String ?: return null
            val msgJson = JSONObject(msgStr)
            val queryAccInfo = msgJson.optJSONObject("query_accinfo") ?: return null
            val accInfoArray = queryAccInfo.optJSONArray("accinfo") ?: return null
            if (accInfoArray.length() == 0) return null
            val balanceStr = accInfoArray.getJSONObject(0).optString("balance", "0")
            val balanceFen = balanceStr.toIntOrNull() ?: 0
            balanceFen / 100.0
        } catch (e: Exception) {
            Log.e(TAG, "parseBalance failed: ${e.message}")
            null
        }
    }

    private fun parseTransactionRow(row: Map<String, Any>, account: String): CampusCardTransactionEntity? {
        return try {
            val occTime = row["OCCTIME"] as? String ?: return null
            val mercName = row["MERCNAME"] as? String ?: ""
            val tranAmt = when (val v = row["TRANAMT"]) {
                is Number -> v.toDouble() / 100.0
                is String -> v.toDoubleOrNull()?.div(100.0) ?: 0.0
                else -> 0.0
            }
            val cardBal = when (val v = row["CARDBAL"]) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val tranName = row["TRANNAME"] as? String ?: ""

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = dateFormat.parse(occTime)?.time ?: System.currentTimeMillis()

            val orderId = "${occTime}_${mercName}_${tranAmt}".hashCode().toString()

            CampusCardTransactionEntity(
                orderId = orderId,
                transactionDateTime = timestamp,
                amountYuan = tranAmt,
                balanceYuan = cardBal,
                transactionDescription = tranName,
                turnoverType = tranName,
                locationName = mercName,
                payName = mercName,
                updatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseTransactionRow failed: ${e.message}")
            null
        }
    }
}
