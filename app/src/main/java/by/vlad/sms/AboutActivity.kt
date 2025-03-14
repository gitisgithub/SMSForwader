package by.vlad.sms

import android.animation.AnimatorInflater
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class AboutActivity : AppCompatActivity() {

    // Переменные для отслеживания быстрых нажатий
    private var tapCount = 0
    private var lastTapTime: Long = 0
    private val tapInterval = 600L // Интервал между нажатиями (в миллисекундах)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Заголовок (можно задать прямо в XML, но тут оставим на случай локализации)
        val textViewGuideTitle = findViewById<TextView>(R.id.textViewGuideTitle)
        textViewGuideTitle.text = "Инструкция"

        // Основной текст шагов
        val textViewGuide = findViewById<TextView>(R.id.textViewGuide)
        textViewGuide.text = """
            1) Создать бота в Telegram (с помощью BotFather).
            2) Скопировать токен, ввести его в программе и сохранить.
            3) В настройках выбрать отправителей и имена SIM-карт.
            4) Разрешить (вручную) этому приложению работать в фоне.
            Готово!
        """.trimIndent()

        // Логотип с 3D-анимацией (если используете rotate_3d.xml в res/animator/)
        val imageViewLogo = findViewById<ImageView>(R.id.imageViewLogo)
        val animator = AnimatorInflater.loadAnimator(this, R.animator.rotate_3d)
        animator.setTarget(imageViewLogo)
        animator.start()

        // Текст об авторе
        val textViewAuthor = findViewById<TextView>(R.id.textViewAuthor)
        textViewAuthor.text = getString(R.string.author_info)

        // Кнопки соцсетей
        val buttonVK = findViewById<Button>(R.id.buttonVK)
        buttonVK.setOnClickListener {
            val vkUrl = "https://vk.com/kunyakin"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vkUrl))
            startActivity(intent)
        }

        val buttonTelegram = findViewById<Button>(R.id.buttonTelegram)
        buttonTelegram.setOnClickListener {
            val tgUrl = "https://t.me/your_channel"
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(tgUrl)))
        }

        // ==========================
        // ОБРАБОТКА ПАСХАЛКИ
        // ==========================
        imageViewLogo.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime < tapInterval) {
                tapCount++
            } else {
                tapCount = 1
            }
            lastTapTime = now

            when (tapCount) {
                5 -> {
                    // Меняем картинку
                    imageViewLogo.setImageResource(R.drawable.other_image)
                }
                10 -> {
                    // Открываем нашу EasterEggActivity
                    EasterEggActivity.start(this)
                }
            }
        }
    }

    /**
     * Показываем простую математическую игру в виде диалога
     * Пользователю задаются случайные выражения вида "72 + 4 = 75?"
     * Нужно ответить "Верно" или "Неверно".
     */
    private fun showMathGameDialog() {
        // Можно сделать AlertDialog, который в цикле генерирует вопросы
        // Для простоты — генерируем один вопрос за раз, после ответа — следующий

        var score = 0
        var total = 0

        fun nextQuestion() {
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(false)

            // Генерируем случайное выражение
            val (question, isCorrect) = generateMathExpression()

            builder.setTitle("Матем. проверка")
            builder.setMessage("Верно ли это выражение?\n$question")

            // Кнопка "Верно"
            builder.setPositiveButton("Верно") { dialog, _ ->
                if (isCorrect) {
                    score++
                } else {
                    score--
                }
                total++
                if (total < 5) {
                    // Следующий вопрос
                    nextQuestion()
                } else {
                    showResultDialog(score)
                }
                dialog.dismiss()
            }
            // Кнопка "Неверно"
            builder.setNegativeButton("Неверно") { dialog, _ ->
                if (!isCorrect) {
                    score++
                } else {
                    score--
                }
                total++
                if (total < 5) {
                    // Следующий вопрос
                    nextQuestion()
                } else {
                    showResultDialog(score)
                }
                dialog.dismiss()
            }
            builder.show()
        }

        nextQuestion()
    }

    /**
     * Генерируем простое выражение вида "72+4=75?" и флаг isCorrect
     */
    private fun generateMathExpression(): Pair<String, Boolean> {
        // Генерируем два числа
        val a = Random.nextInt(1, 100)
        val b = Random.nextInt(1, 100)
        val sum = a + b

        // Иногда делаем "ошибку" в сумме
        val mistakeChance = Random.nextBoolean()
        val displayedSum = if (mistakeChance) sum + Random.nextInt(-3, 4) else sum

        val question = "$a + $b = $displayedSum"
        val isCorrect = (displayedSum == sum)
        return question to isCorrect
    }

    /**
     * Показываем результат
     */
    private fun showResultDialog(score: Int) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Итоги")
        builder.setMessage("Ваш итоговый счёт: $score")
        builder.setPositiveButton("OK") { d, _ -> d.dismiss() }
        builder.show()
    }
}