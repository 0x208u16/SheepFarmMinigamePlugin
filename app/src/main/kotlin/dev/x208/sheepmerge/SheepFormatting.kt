package dev.x208.sheepmerge

import java.math.BigInteger
import java.util.Locale
import org.bukkit.ChatColor

object SheepFormatting {
    private val pointSuffixes = arrayOf("", "K", "M", "B", "T", "Q", "Qi", "Sx", "Sp", "Oc", "No", "Dc")
    private val suffixThreshold = BigInteger.valueOf(10_000L)
    private val suffixDivisor = BigInteger.valueOf(1_000L)

    @JvmStatic
    fun formatPoints(points: Long): String = formatPoints(BigInteger.valueOf(points))

    @JvmStatic
    fun formatPoints(points: BigInteger?): String {
        val safe = points ?: BigInteger.ZERO
        val negative = safe.signum() < 0
        var value = if (negative) safe.negate() else safe
        var suffixIndex = 0
        while (value >= suffixThreshold && suffixIndex < pointSuffixes.lastIndex) {
            value /= suffixDivisor
            suffixIndex++
        }
        return (if (negative) "-" else "") + value + pointSuffixes[suffixIndex]
    }

    @JvmStatic
    fun formatRainbowTier(tier: Int): String = "T" + formatPoints(tier.coerceAtLeast(1).toLong())

    @JvmStatic
    fun color(message: String?): String {
        if (message == null) {
            return ""
        }
        return ChatColor.translateAlternateColorCodes('&', message)
    }

    @JvmStatic
    fun formatComboMultiplier(multiplier: Double): String = String.format(Locale.ROOT, "%.2f", multiplier)

    @JvmStatic
    fun formatDuration(durationMs: Long): String = (durationMs / 1_000L).coerceAtLeast(0L).toString() + "s"
}