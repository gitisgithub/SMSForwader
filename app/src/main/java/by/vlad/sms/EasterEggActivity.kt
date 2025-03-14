package by.vlad.sms

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.random.Random

class EasterEggActivity : AppCompatActivity() {

    private lateinit var progressBarLife: LinearProgressIndicator
    private lateinit var textScore: TextView
    private lateinit var textQuestion: TextView
    private lateinit var buttonTrue: Button
    private lateinit var buttonFalse: Button

    private var score = 0
    private var life = 100

    private lateinit var timer: CountDownTimer

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, EasterEggActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easter_egg)

        textScore = findViewById(R.id.textScore)
        textQuestion = findViewById(R.id.textQuestion)
        buttonTrue = findViewById(R.id.buttonTrue)
        buttonFalse = findViewById(R.id.buttonFalse)
        progressBarLife = findViewById(R.id.progressBarLife)

        // Устанавливаем max=100 (это доступно и на старых API)
        progressBarLife.max = 100

        // Начинаем со 100% без анимации (нормально на любом API)
        setProgressCompat(100, animate = false)

        // Таймер на Long.MAX_VALUE, каждую секунду уменьшаем life
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                life--
                if (life < 0) life = 0
                setProgressCompat(life, animate = true)
                if (life == 0) {
                    showGameOverDialog("Время вышло! Полоса опустела.")
                }
            }
            override fun onFinish() { /* никогда не вызывается, т.к. Long.MAX_VALUE */ }
        }
        timer.start()

        textScore.text = getString(R.string.easter_score_format, score)

        buttonTrue.setOnClickListener {
            checkAnswer(true)
        }
        buttonFalse.setOnClickListener {
            checkAnswer(false)
        }

        nextQuestion()
    }

    /**
     * Обновляем прогресс. Если API >= 24, делаем анимированно, иначе — без анимации.
     */
    private fun setProgressCompat(value: Int, animate: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBarLife.setProgress(value, animate)
        } else {
            // Старый метод без анимации
            progressBarLife.progress = value
        }
    }

    private fun nextQuestion() {
        val (question, isCorrect) = generateExpression()
        textQuestion.tag = isCorrect
        textQuestion.text = question
    }

    private fun checkAnswer(userSaidTrue: Boolean) {
        val isCorrect = textQuestion.tag as? Boolean ?: false
        if (userSaidTrue == isCorrect) {
            score++
            // При верном ответе немного восстанавливаем life
            life += 10
            if (life > 100) life = 100
        } else {
            // При неверном ответе уменьшаем жизнь сильнее
            life -= 50
            if (life < 0) life = 0
        }
        setProgressCompat(life, animate = true)

        // Вместо конкатенации строк, используем getString с placeholder (см. ниже)
        textScore.text = getString(R.string.easter_score_format, score)

        if (life == 0) {
            showGameOverDialog("Полоса опустела! Игра окончена.")
        } else {
            nextQuestion()
        }
    }

    private fun generateExpression(): Pair<String, Boolean> {
        val a = Random.nextInt(1, 100)
        val b = Random.nextInt(1, 100)
        val sum = a + b
        val displayed = if (Random.nextBoolean()) sum else sum + Random.nextInt(-5, 6)
        val expression = "$a + $b = $displayed?"
        val isCorrect = (displayed == sum)
        return expression to isCorrect
    }

    private fun showGameOverDialog(msg: String) {
        timer.cancel()
        AlertDialog.Builder(this)
            .setTitle("Итог")
            .setMessage("$msg\nВаш счёт: $score")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }
}
