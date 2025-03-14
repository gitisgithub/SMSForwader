package by.vlad.sms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class EasterEggActivity : AppCompatActivity() {

    private lateinit var textScore: TextView
    private lateinit var textTimer: TextView
    private lateinit var textQuestion: TextView
    private lateinit var buttonTrue: Button
    private lateinit var buttonFalse: Button

    private var score = 0
    private var totalQuestions = 0
    private var timeLeftMs = 30_000L // 30 секунд на игру

    private lateinit var timer: CountDownTimer

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, EasterEggActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Можно сделать Activity полноэкранной:
        // window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_easter_egg)

        textScore = findViewById(R.id.textScore)
        textTimer = findViewById(R.id.textTimer)
        textQuestion = findViewById(R.id.textQuestion)
        buttonTrue = findViewById(R.id.buttonTrue)
        buttonFalse = findViewById(R.id.buttonFalse)

        // Запускаем таймер на 30 секунд
        timer = object : CountDownTimer(timeLeftMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                textTimer.text = "Время: $seconds"
            }
            override fun onFinish() {
                // Время вышло
                endGame()
            }
        }
        timer.start()

        // Генерируем первый вопрос
        nextQuestion()

        buttonTrue.setOnClickListener {
            checkAnswer(true)
        }
        buttonFalse.setOnClickListener {
            checkAnswer(false)
        }
    }

    private fun nextQuestion() {
        val (question, isCorrect) = generateExpression()
        textQuestion.tag = isCorrect // Храним правильность в tag
        textQuestion.text = question
    }

    private fun checkAnswer(userSaidTrue: Boolean) {
        val isCorrect = textQuestion.tag as? Boolean ?: false
        if (userSaidTrue == isCorrect) {
            score++
        } else {
            score--
        }
        totalQuestions++
        textScore.text = "Счёт: $score"

        // Можно задать лимит вопросов (например, 10), или пусть игра идёт до истечения таймера
        if (totalQuestions >= 20) {
            endGame()
        } else {
            nextQuestion()
        }
    }

    private fun generateExpression(): Pair<String, Boolean> {
        val a = Random.nextInt(1, 200)
        val b = Random.nextInt(1, 200)
        val sum = a + b
        // С вероятностью 50% подменяем результат
        val displayed = if (Random.nextBoolean()) sum else sum + Random.nextInt(-5, 6)
        val expression = "$a + $b = $displayed?"
        val isCorrect = (displayed == sum)
        return expression to isCorrect
    }

    private fun endGame() {
        timer.cancel()
        // Можно показать итоговое окно или просто закрыть Activity
        val finalMessage = "Игра окончена!\nВаш счёт: $score"
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Результат")
            .setMessage(finalMessage)
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .create()
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }
}
