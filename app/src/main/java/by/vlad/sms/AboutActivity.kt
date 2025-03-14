package by.vlad.sms

import android.animation.AnimatorInflater
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

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
            val vkUrl = "https://vk.com/your_profile"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vkUrl))
            startActivity(intent)
        }

        val buttonTelegram = findViewById<Button>(R.id.buttonTelegram)
        buttonTelegram.setOnClickListener {
            val tgUrl = "https://t.me/your_channel"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tgUrl))
            startActivity(intent)
        }
    }
}
