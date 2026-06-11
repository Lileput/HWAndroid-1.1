package ru.netology.nmedia.util

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.core.text.util.LinkifyCompat

object LinkUtils {
    fun bindTextWithLinks(textView: TextView, text: String) {
        textView.text = text
        LinkifyCompat.addLinks(textView, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}
