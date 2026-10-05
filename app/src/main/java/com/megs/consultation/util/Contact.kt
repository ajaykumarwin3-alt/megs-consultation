package com.megs.consultation.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object Contact {
    fun dial(ctx: Context, phone: String) = launch(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))

    fun whatsapp(ctx: Context, phone: String, message: String = "") {
        var digits = phone.filter { it.isDigit() }
        if (digits.length == 10) digits = "91$digits"          // default to India
        val q = if (message.isNotEmpty()) "?text=" + Uri.encode(message) else ""
        launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits$q")))
    }

    fun shareText(ctx: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        launch(ctx, Intent.createChooser(send, "Share summary"))
    }

    private fun launch(ctx: Context, i: Intent) {
        try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(ctx, "No app available for this action", Toast.LENGTH_SHORT).show()
        }
    }
}
