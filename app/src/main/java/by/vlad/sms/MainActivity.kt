package by.vlad.sms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// Для отключения фоновых ограничений (Android 10+):
private const val ACTION_IGNORE_BACKGROUND_RESTRICTIONS = "android.settings.IGNORE_BACKGROUND_RESTRICTIONS"

/** Проверяем валидность Telegram-токена через getMe. */
private suspend fun isTokenValid(token: String): Boolean {
    return withContext(Dispatchers.IO) {
        val checkUrl = "https://api.telegram.org/bot$token/getMe"
        try {
            val url = URL(checkUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                // Успех, если "ok" == true
                json.optBoolean("ok", false)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var tokenEditText: EditText
    private lateinit var saveOrEditButton: Button
    private lateinit var disableBatteryButton: Button
    private lateinit var toggleServiceButton: Button
    private lateinit var logsTextView: TextView

    // Храним логи в памяти
    private val logBuffer = StringBuilder()

    // Флаг, показывающий, редактируем ли мы сейчас токен
    private var isEditingToken = false

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100

        // callback, чтобы другие классы могли добавить запись в логи MainActivity
        var logCallback: ((String) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tokenEditText = findViewById(R.id.editTextToken)
        saveOrEditButton = findViewById(R.id.buttonSaveOrEdit)
        disableBatteryButton = findViewById(R.id.buttonDisableBattery)
        toggleServiceButton = findViewById(R.id.buttonToggleService)
        logsTextView = findViewById(R.id.textViewLogs)

        // Лог для отладки
        logCallback = { message ->
            runOnUiThread {
                appendLog(message)
            }
        }

        // Считываем сохранённый токен
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedToken = prefs.getString("telegram_bot_token", "")

        if (!savedToken.isNullOrEmpty()) {
            // Токен уже есть, скрываем и выходим из режима редактирования
            tokenEditText.setText("••••••")
            isEditingToken = false
            appendLog("Токен уже сохранён (скрыт).")
        } else {
            // Токена нет, даём возможность ввести
            isEditingToken = true
            appendLog("Токен отсутствует, введите и сохраните.")
        }
        updateButtonLabel()

        saveOrEditButton.setOnClickListener {
            if (isEditingToken) {
                // Сохраняем токен с проверкой
                val token = tokenEditText.text.toString().trim()
                if (token.isEmpty()) {
                    Toast.makeText(this, "Введите токен!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.Main).launch {
                    val valid = isTokenValid(token)
                    if (valid) {
                        prefs.edit().putString("telegram_bot_token", token).apply()
                        Toast.makeText(this@MainActivity, "Токен сохранён", Toast.LENGTH_SHORT).show()
                        appendLog("Токен сохранён: $token")

                        tokenEditText.setText("••••••")
                        isEditingToken = false
                        updateButtonLabel()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Токен неверный, проверьте и повторите",
                            Toast.LENGTH_LONG
                        ).show()
                        appendLog("Токен неверный")
                    }
                }
            } else {
                // Режим редактирования
                val realToken = prefs.getString("telegram_bot_token", "")
                tokenEditText.setText(realToken)
                isEditingToken = true
                updateButtonLabel()
            }
        }

        disableBatteryButton.setOnClickListener {
            disableBatteryOptimizations()
        }

        toggleServiceButton.setOnClickListener {
            val isRunning = MyForegroundService.isServiceRunning
            if (isRunning) {
                // Останавливаем сервис
                stopService(Intent(this, MyForegroundService::class.java))
                MyForegroundService.isServiceRunning = false
                toggleServiceButton.text = getString(R.string.service_on)
                appendLog("Фоновый сервис остановлен")
            } else {
                // Запускаем сервис
                val serviceIntent = Intent(this, MyForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                MyForegroundService.isServiceRunning = true
                toggleServiceButton.text = getString(R.string.service_off)
                appendLog("Фоновый сервис запущен")
            }
        }

        // Запрашиваем разрешения
        requestAllPermissions()
    }

    private fun updateButtonLabel() {
        saveOrEditButton.text = if (isEditingToken) {
            getString(R.string.save_token)
        } else {
            "Редактировать"
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestAllPermissions() {
        val neededPermissions = arrayListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            neededPermissions.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            neededPermissions.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            neededPermissions.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                neededPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            appendLog("Все разрешения уже выданы")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Разрешения получены", Toast.LENGTH_SHORT).show()
                appendLog("Разрешения получены")
            } else {
                Toast.makeText(this, "Не все разрешения выданы!", Toast.LENGTH_SHORT).show()
                appendLog("Не все разрешения выданы. Функционал может быть ограничен.")
            }
        }
    }

    /**
     * Улучшенная версия отключения энергосбережения + фоновых ограничений.
     */
    private fun disableBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val ignoring = pm.isIgnoringBatteryOptimizations(packageName)
        Log.d("DisableBattery", "Перед запросом. ignoringBatteryOptimizations=$ignoring")

        // Сначала ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        if (!ignoring) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Log.d("DisableBattery", "Запустили ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
                appendLog("Пытаемся отключить оптимизацию батареи (Android).")
            } catch (e: Exception) {
                Log.e("DisableBattery", "Ошибка при открытии настроек оптимизации: ", e)
                appendLog("Невозможно открыть настройки оптимизации батареи: ${e.message}")
                Toast.makeText(this, "Невозможно открыть настройки оптимизации батареи", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.service_already_disabled), Toast.LENGTH_SHORT).show()
            Log.d("DisableBattery", "Система уже игнорирует оптимизацию для $packageName")
            appendLog("Уже отключено энергосбережение для данного приложения.")
        }

        // Если Android 10+ - попробуем также отключить Background Restrictions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val intent = Intent("android.settings.IGNORE_BACKGROUND_RESTRICTIONS").apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Log.d("DisableBattery", "Запустили IGNORE_BACKGROUND_RESTRICTIONS (Android 10+)")
                appendLog("Пытаемся отключить фоновые ограничения (Android 10+).")
            } catch (e: Exception) {
                Log.e("DisableBattery", "Ошибка при открытии Background Restrictions: ", e)
                appendLog("Не удалось открыть Background Restrictions: ${e.message}")
            }
        }
    }

    private fun appendLog(message: String) {
        logBuffer.append(message).append("\n")
        logsTextView.text = logBuffer.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (logCallback === MainActivity.logCallback) {
            logCallback = null
        }
    }
}
