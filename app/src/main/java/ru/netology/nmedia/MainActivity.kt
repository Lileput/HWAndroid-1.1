package ru.netology.nmedia

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val post = Post(
            id = 1,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "21 мая в 18:36",
            content = "Привет, это новая Нетология! Когда-то Нетология начиналась с " +
                    "интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, " +
                    "аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до " +
                    "уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, " +
                    "целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb",
            likes = 24540,
            share = 2000,
            views = 2540000,
        )

        with(binding) {
            author.text = post.author
            published.text = post.published
            content.text = post.content
            countEyes.text = ShortNumberFormatter.format(post.views)
            countLikes.text = ShortNumberFormatter.format(post.likes)
            countReposts.text = ShortNumberFormatter.format(post.share)
            likes.setImageResource(R.drawable.ic_outline_like_24)

            likes.setOnClickListener {
                post.likeByMe = !post.likeByMe
                if (post.likeByMe) {
                    likes.setImageResource(R.drawable.ic_liked_red_24)
                    post.likes++
                } else {
                    likes.setImageResource(R.drawable.ic_outline_like_24)
                    post.likes--
                }
                countLikes.text = ShortNumberFormatter.format(post.likes)
            }

            reposts.setOnClickListener {
                post.share++
                countReposts.text = ShortNumberFormatter.format(post.share)
            }
        }

    }
}

object ShortNumberFormatter {
    fun format(number: Int): String {
        return when {
            number < 1000 -> {
                number.toString()
            }
            number < 10_000 -> {
                val hundreds = number % 1000 / 100
                if (hundreds == 0) {
                    (number / 1000).toString() + "K"
                } else {
                    String.format("%.1fK", number.toDouble() / 1000)
                }
            }

            number < 1_000_000 -> {
                (number / 1000).toString() + "K"
            }

            else -> {
                val hundredsThousands = (number % 1_000_000) / 100_000
                if (hundredsThousands == 0) {
                    (number / 1_000_000).toString() + "M"
                } else {
                    String.format("%.1fM", number.toDouble() / 1_000_000)
                }
            }
        }
    }
}