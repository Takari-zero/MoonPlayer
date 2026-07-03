package com.shenghui.localvibe.core.player

import android.media.audiofx.Equalizer

enum class EqualizerPreset(
    val label: String
) {
    DEFAULT("默认"),
    CLASSICAL("古典"),
    DANCE("舞曲"),
    VOCAL("人声"),
    ROCK("摇滚"),
    METAL("金属"),
    CUSTOM("自定义")
}

fun Equalizer.applyPreset(preset: EqualizerPreset) {
    val bandCount = numberOfBands.toInt().coerceAtLeast(1)
    val range = bandLevelRange
    val minLevel = range[0].toInt()
    val maxLevel = range[1].toInt()

    repeat(bandCount) { index ->
        val normalized = if (bandCount == 1) 0f else index / (bandCount - 1).toFloat()
        val target = when (preset) {
            EqualizerPreset.DEFAULT -> 0
            EqualizerPreset.CLASSICAL -> ((normalized - 0.35f) * 700).toInt()
            EqualizerPreset.DANCE -> if (normalized < 0.35f) 950 else if (normalized > 0.75f) 500 else 100
            EqualizerPreset.VOCAL -> {
                val distanceFromMid = kotlin.math.abs(normalized - 0.5f)
                ((1f - distanceFromMid * 2f) * 900).toInt()
            }
            EqualizerPreset.ROCK -> if (normalized < 0.25f || normalized > 0.7f) 850 else 100
            EqualizerPreset.METAL -> if (normalized < 0.2f || normalized > 0.68f) 1100 else -250
            EqualizerPreset.CUSTOM -> 0
        }.coerceIn(minLevel, maxLevel)
        setBandLevel(index.toShort(), target.toShort())
    }
}
