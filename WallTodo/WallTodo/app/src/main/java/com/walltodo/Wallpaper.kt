package com.walltodo

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object WallpaperRenderer {
    fun render(ctx: Context, todos: List<Todo>): Bitmap {
        val dm = ctx.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
                Color.parseColor("#0F2027"), Color.parseColor("#2C5364"), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        val left = w * 0.08f
        val textW = (w * 0.84f).toInt()
        val top = h * 0.30f          // leave room for clock/notifications
        val bottom = h * 0.90f       // leave room for shortcuts
        val avail = bottom - top

        val date = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0BEC5"); textSize = w * 0.04f
        }
        c.drawText(SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date()),
            left, top - w * 0.03f, date)

        if (todos.isEmpty()) {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = w * 0.06f }
            c.drawText("All clear \u2728", left, top + p.textSize * 1.5f, p)
            return bmp
        }

        var items = todos
        var size = w * 0.055f
        var layouts = build(items, size, textW, "")
        // shrink font first, then drop items
        while (height(layouts) > avail && size > w * 0.032f) {
            size -= 2f; layouts = build(items, size, textW, "")
        }
        while (height(layouts) > avail && items.size > 1) {
            items = items.dropLast(1)
            layouts = build(items, size, textW, "+${todos.size - items.size} more")
        }
        var y = top
        for (l in layouts) {
            c.save(); c.translate(left, y); l.draw(c); c.restore()
            y += l.height + size * 0.5f
        }
        return bmp
    }

    private fun height(l: List<StaticLayout>) =
        l.sumOf { (it.height + 20).toDouble() }.toFloat()

    private fun build(items: List<Todo>, size: Float, width: Int, more: String): List<StaticLayout> {
        val out = items.map { t ->
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                color = if (t.isDone) Color.parseColor("#78909C") else Color.WHITE
                isStrikeThruText = t.isDone
            }
            val prefix = if (t.isDone) "\u2713  " else "\u2022  "
            StaticLayout.Builder.obtain(prefix + t.title, 0, prefix.length + t.title.length, p, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        }.toMutableList()
        if (more.isNotEmpty()) {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size * 0.8f; color = Color.parseColor("#B0BEC5") }
            out += StaticLayout.Builder.obtain(more, 0, more.length, p, width).build()
        }
        return out
    }
}

object WallpaperUpdater {
    /** Returns true on success. */
    suspend fun update(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            val todos = AppDatabase.get(ctx).dao().getAll()
                .filter { !it.isDone || (it.doneAt ?: 0L) > cutoff }
            val bmp = WallpaperRenderer.render(ctx, todos)
            WallpaperManager.getInstance(ctx)
                .setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
            true
        } catch (e: Exception) { false }
    }
}
