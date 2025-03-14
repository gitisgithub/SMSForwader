package by.vlad.sms

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object TelegramSender {

    private const val PREFS_NAME = "settings"
    private const val KEY_TELEGRAM_TOKEN = "telegram_bot_token"
    private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"

    @SuppressLint("UseKtx")
    suspend fun sendMessage(context: Context, message: String) {
        withContext(Dispatchers.IO) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val token = prefs.getString(KEY_TELEGRAM_TOKEN, "") ?: ""
            if (token.isEmpty()) {
                Log.e("TelegramSender", "Токен Telegram-бота не задан!")
                MainActivity.logCallback?.invoke("Ошибка: Токен Telegram-бота не задан!")
                return@withContext
            }

            var chatId = prefs.getString(KEY_TELEGRAM_CHAT_ID, null)
            if (chatId.isNullOrEmpty()) {
                // Пытаемся получить chat_id через getUpdates
                chatId = fetchChatId(token)
                if (chatId != null) {
                    prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, chatId).apply()
                } else {
                    Log.e("TelegramSender", "chat_id не найден. Отправьте сообщение вашему боту!")
                    MainActivity.logCallback?.invoke("Ошибка: chat_id не найден. Отправьте сообщение боту в Telegram!")
                    return@withContext
                }
            }

            // Формируем URL для отправки сообщения
            val sendUrl = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=${message.encodeURL()}"
            try {
                val url = URL(sendUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()
                    Log.d("TelegramSender", "Сообщение отправлено: $response")
                    MainActivity.logCallback?.invoke("Отправлено в Telegram: $message")
                } else {
                    Log.e("TelegramSender", "Ошибка отправки. Код ответа: $responseCode")
                    MainActivity.logCallback?.invoke("Ошибка отправки в Telegram: код $responseCode")
                }
            } catch (e: Exception) {
                Log.e("TelegramSender", "Ошибка при отправке сообщения: ${e.message}")
                MainActivity.logCallback?.invoke("Ошибка при отправке: ${e.message}")
            }
        }
    }

    private fun String.encodeURL(): String =
        java.net.URLEncoder.encode(this, "UTF-8")

    // Получение chat_id через getUpdates (требует, чтобы бот получил хоть какое-то сообщение)
    private fun fetchChatId(token: String): String? {
        val getUpdatesUrl = "https://api.telegram.org/bot$token/getUpdates"
        try {
            val url = URL(getUpdatesUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonObj = JSONObject(response)
                if (jsonObj.getBoolean("ok")) {
                    val resultArray = jsonObj.getJSONArray("result")
                    if (resultArray.length() > 0) {
                        val firstUpdate = resultArray.getJSONObject(0)
                        val message = firstUpdate.optJSONObject("message")
                        if (message != null) {
                            val chat = message.optJSONObject("chat")
                            if (chat != null) {
                                return chat.optString("id", null)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TelegramSender", "Ошибка получения chat_id: ${e.message}")
            MainActivity.logCallback?.invoke("Ошибка получения chat_id: ${e.message}")
        }
        return null
    }
}
