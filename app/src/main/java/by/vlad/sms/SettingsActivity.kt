package by.vlad.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var editSim1Name: EditText
    private lateinit var editSim2Name: EditText
    private lateinit var listViewSenders: ListView
    private lateinit var editManualSender: EditText
    private lateinit var buttonAddManualSender: Button
    private lateinit var buttonSaveSettings: Button
    private lateinit var switchForwardAll: Switch

    // Хранение subId (на случай двух SIM)
    private var firstSubId: Int = -1
    private var secondSubId: Int = -1

    // Список всех отправителей
    private val sendersList = mutableListOf<String>()
    // Множество «разрешённых» отправителей
    private val checkedSenders = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Ищем элементы
        editSim1Name = findViewById(R.id.editTextSim1Name)
        editSim2Name = findViewById(R.id.editTextSim2Name)
        listViewSenders = findViewById(R.id.listViewSenders)
        editManualSender = findViewById(R.id.editTextManualSender)
        buttonAddManualSender = findViewById(R.id.buttonAddManualSender)
        buttonSaveSettings = findViewById(R.id.buttonSaveSettings)
        switchForwardAll = findViewById(R.id.switchForwardAll)

        // Загружаем prefs
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        // 1) Имена SIM (из prefs)
        val sim1NameSaved = prefs.getString("sim1_name", "SIM1")
        val sim2NameSaved = prefs.getString("sim2_name", "SIM2")
        editSim1Name.setText(sim1NameSaved)
        editSim2Name.setText(sim2NameSaved)

        // 2) Список «разрешённых» отправителей
        val savedSenders = prefs.getStringSet("allowed_senders", emptySet())?.toMutableSet() ?: mutableSetOf()
        checkedSenders.addAll(savedSenders)

        // 3) «Пересылать от всех?»
        val forwardAll = prefs.getBoolean("forward_all", false)
        switchForwardAll.isChecked = forwardAll
        setVisibilityForSenders(!forwardAll)  // если forwardAll=true, скрываем список

        // Слушатель на switch
        switchForwardAll.setOnCheckedChangeListener { _, isChecked ->
            setVisibilityForSenders(!isChecked)
        }

        // 4) subId, если есть READ_PHONE_STATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val hasPhoneStatePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            if (hasPhoneStatePerm == PackageManager.PERMISSION_GRANTED) {
                val manager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val subs = manager.activeSubscriptionInfoList
                if (!subs.isNullOrEmpty()) {
                    firstSubId = subs[0].subscriptionId
                    // Перезаписываем имя из SubscriptionManager ТОЛЬКО если пользователь
                    // не менял его (т. е. оно осталось "SIM1" или пустое)
                    if (sim1NameSaved.isNullOrEmpty() || sim1NameSaved == "SIM1") {
                        editSim1Name.setText(subs[0].displayName.toString())
                    }
                }
                if (subs != null && subs.size > 1) {
                    secondSubId = subs[1].subscriptionId
                    if (sim2NameSaved.isNullOrEmpty() || sim2NameSaved == "SIM2") {
                        editSim2Name.setText(subs[1].displayName.toString())
                    }
                }
            }
        }

        // 5) Читаем список отправителей (READ_SMS)
        loadAllDistinctSenders()

        // Заполняем ListView
        sendersList.sort()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, sendersList)
        listViewSenders.adapter = adapter
        listViewSenders.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // Отмечаем ранее сохранённых
        for (i in sendersList.indices) {
            if (checkedSenders.contains(sendersList[i])) {
                listViewSenders.setItemChecked(i, true)
            }
        }

        // При нажатии на элемент списка
        listViewSenders.setOnItemClickListener { _, _, position, _ ->
            val sender = sendersList[position]
            if (checkedSenders.contains(sender)) {
                checkedSenders.remove(sender)
            } else {
                checkedSenders.add(sender)
            }
        }

        // Кнопка «Добавить вручную»
        buttonAddManualSender.setOnClickListener {
            val manual = editManualSender.text.toString().trim()
            if (manual.isNotEmpty()) {
                if (!sendersList.contains(manual)) {
                    sendersList.add(manual)
                    sendersList.sort()
                    adapter.notifyDataSetChanged()
                }
                checkedSenders.add(manual)
                val index = sendersList.indexOf(manual)
                if (index >= 0) {
                    listViewSenders.setItemChecked(index, true)
                }
                editManualSender.setText("")
            } else {
                Toast.makeText(this, "Введите номер/имя отправителя", Toast.LENGTH_SHORT).show()
            }
        }

        // Кнопка «Сохранить настройки»
        buttonSaveSettings.setOnClickListener {
            val sim1Name = editSim1Name.text.toString().trim()
            val sim2Name = editSim2Name.text.toString().trim()

            prefs.edit()
                // Сохраняем имена SIM
                .putString("sim1_name", sim1Name)
                .putString("sim2_name", sim2Name)
                // Сохраняем subId
                .putInt("sim1_subId", firstSubId)
                .putInt("sim2_subId", secondSubId)
                // Сохраняем список отправителей
                .putStringSet("allowed_senders", checkedSenders)
                // Сохраняем «пересылать от всех»
                .putBoolean("forward_all", switchForwardAll.isChecked)
                .apply()

            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Показывать/скрывать список отправителей и поля ввода.
     */
    private fun setVisibilityForSenders(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        listViewSenders.visibility = visibility
        editManualSender.visibility = visibility
        buttonAddManualSender.visibility = visibility
    }

    /**
     * Считываем уникальные адреса отправителей из «входящих» SMS (требует разрешения READ_SMS).
     */
    private fun loadAllDistinctSenders() {
        val uriInbox = Uri.parse("content://sms/inbox")
        val permCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        if (permCheck != PackageManager.PERMISSION_GRANTED) {
            return
        }

        contentResolver.query(uriInbox, arrayOf("address"), null, null, null)?.use { cursor ->
            val columnIndexAddress = cursor.getColumnIndex("address")
            while (cursor.moveToNext()) {
                val address = cursor.getString(columnIndexAddress)
                if (!address.isNullOrEmpty() && !sendersList.contains(address)) {
                    sendersList.add(address)
                }
            }
        }
    }
}
