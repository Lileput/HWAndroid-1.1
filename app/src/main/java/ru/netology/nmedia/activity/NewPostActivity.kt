package ru.netology.nmedia.activity

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityNewPostBinding
import ru.netology.nmedia.dto.Post

class NewPostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EDIT_POST = "edit_post"
        const val EXTRA_EDIT_POST_ID = "edit_post_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityNewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val editPost = intent.getStringExtra(EXTRA_EDIT_POST)
        val postId = intent.getLongExtra(EXTRA_EDIT_POST_ID, 0L)

        if (editPost != null) {
            binding.content.setText(editPost)
        }
        binding.ok.setOnClickListener {
            val text = binding.content.text.toString()
            if (text.isBlank()) {
                setResult(RESULT_CANCELED)
            } else {
                val resultIntent = Intent().apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    if (postId != 0L) putExtra(EXTRA_EDIT_POST_ID, postId)
                }
                setResult(RESULT_OK, resultIntent)
            }
            finish()
        }
    }
}

object NewPostContract : ActivityResultContract<Post?, Pair<String, Long>?>() {
    override fun createIntent(context: Context, post: Post?) =
        Intent(context, NewPostActivity::class.java).apply {
            post?.let {
                putExtra(NewPostActivity.EXTRA_EDIT_POST, it.content)
                putExtra(NewPostActivity.EXTRA_EDIT_POST_ID, it.id)
            }
        }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        if (resultCode == RESULT_OK) {
            val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
            val postId = intent?.getLongExtra(NewPostActivity.EXTRA_EDIT_POST_ID, 0L) ?: 0L
            if (text != null) text to postId else null
        } else {
            null
        }
}