package com.despertadorflash

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject

internal enum class NeonGender(val label: String) {
    MAN("SOY HOMBRE"), WOMAN("SOY MUJER"), PRIVATE("PREFIERO NO DECIRLO")
}

internal data class NeonPortrait(val id: String, val label: String, val gender: NeonGender, val asset: String)

internal data class NeonCatalog(val logo: String, val portraits: List<NeonPortrait>) {
    init {
        require(portraits.size == 32 && portraits.map { it.id }.distinct().size == 32)
        require(portraits.map { it.asset }.distinct().size == 32)
        require(portraits.none { it.gender == NeonGender.PRIVATE })
        require(portraits.map { it.gender }.toSet().size == 2)
    }

    fun forGender(gender: NeonGender): List<NeonPortrait> =
        if (gender == NeonGender.PRIVATE) portraits else portraits.filter { it.gender == gender }
}

internal data class NeonDraft(val gender: NeonGender? = null, val name: String = "", val avatar: String? = null) {
    fun canContinue(): Boolean = gender != null && name.trim().length in 1..100

    fun selectGender(value: NeonGender): NeonDraft =
        if (gender == value) this else copy(gender = value, avatar = null)

    fun validFor(catalog: NeonCatalog): Boolean =
        canContinue() && catalog.forGender(requireNotNull(gender)).any { it.id == avatar }
}

internal object NeonIdentity {
    private var loaded = false
    private var cached: NeonCatalog? = null

    @Synchronized
    fun catalog(context: Context): NeonCatalog? {
        if (loaded) return cached
        loaded = true
        cached = runCatching {
            val files = context.assets.list("neon").orEmpty().toSet()
            val root = context.assets.open("neon/catalog.json").bufferedReader().use { JSONObject(it.readText()) }
            val logo = root.getString("logo").also { require(it in files) }
            val entries = root.getJSONArray("portraits")
            NeonCatalog(logo, (0 until entries.length()).map { index ->
                val entry = entries.getJSONObject(index)
                NeonPortrait(
                    entry.getString("id"),
                    entry.getString("label").also { require(it.isNotBlank()) },
                    NeonGender.valueOf(entry.getString("gender")),
                    entry.getString("asset").also { require(it in files && it != logo) }
                )
            })
        }.getOrNull()
        return cached
    }

    @JvmStatic
    fun shouldWelcome(context: Context): Boolean =
        catalog(context) != null && !preferences(context).getBoolean("neon_welcome_complete", false)

    @JvmStatic
    fun menuPortrait(context: Context, pixels: Int): Bitmap? {
        val avatar = preferences(context).getString("neon_avatar", null)
        return catalog(context)?.portraits?.firstOrNull { it.id == avatar }
            ?.let { bitmap(context, it.asset, pixels) }
    }

    fun preferences(context: Context) = context.getSharedPreferences("aurora_profile", Context.MODE_PRIVATE)

    fun bitmap(context: Context, asset: String, pixels: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open("neon/$asset").use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= pixels.coerceAtLeast(1)) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.assets.open("neon/$asset").use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}
