@file:Suppress("DEPRECATION")

package by.vlad.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            // Получаем массив PDU (каждый сегмент SMS)
            val pdus = bundle["pdus"] as? Array<*>
            if (!pdus.isNullOrEmpty()) {

                // Преобразуем все pdu в SmsMessage
                val messages = pdus.mapNotNull { pdu ->
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }

                if (messages.isNotEmpty()) {
                    // Берём отправителя из первой части
                    val sender = messages[0].displayOriginatingAddress ?: ""

                    // Склеиваем все части сообщения
                    val messageBody = messages.joinToString(separator = "") {
                        it.displayMessageBody ?: ""
                    }

                    // Пытаемся определить subscriptionId (если API >= 22)
                    // Обычно у всех частей он одинаковый, берём из первой
                    val subId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        val tmp = intent.getIntExtra("subscription", -1)
                        if (tmp != -1) tmp
                        else intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
                    } else {
                        -1
                    }

                    // Читаем настройки
                    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

                    // Считываем subId и имя для SIM1, SIM2
                    val sim1SubId = prefs.getInt("sim1_subId", -1)
                    val sim2SubId = prefs.getInt("sim2_subId", -1)
                    val sim1Name = prefs.getString("sim1_name", "SIM1")
                    val sim2Name = prefs.getString("sim2_name", "SIM2")

                    // Фильтр отправителей
                    val forwardAll = prefs.getBoolean("forward_all", false)
                    val allowedSenders = prefs.getStringSet("allowed_senders", emptySet()) ?: emptySet()

                    // Если «пересылать только от выбранных» (forwardAll == false),
                    // но отправителя нет в списке, пропускаем.
                    if (!forwardAll) {
                        if (allowedSenders.isNotEmpty() && !allowedSenders.contains(sender)) {
                            Log.d("SMSReceiver", "Отправитель '$sender' не в списке. Пропускаем.")
                            return
                        }
                    }

                    // Определяем, какая SIM
                    val simName = when (subId) {
                        sim1SubId -> sim1Name
                        sim2SubId -> sim2Name
                        else -> "Неизвестная SIM"
                    }

                    // Формируем итоговый текст
                    val fullMessage = "[$simName] SMS от $sender:\n$messageBody"
                    Log.d("SMSReceiver", fullMessage)

                    // Отправляем сообщение в Telegram
                    CoroutineScope(Dispatchers.IO).launch {
                        TelegramSender.sendMessage(context, fullMessage)
                    }
                }
            }
        }
    }
}
