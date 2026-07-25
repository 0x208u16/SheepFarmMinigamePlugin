package dev.x208.sheepmerge

import org.bukkit.DyeColor

enum class SheepTier(
    val level: Int,
    val color: DyeColor?,
    val displayName: String
) {
    WHITE(0, DyeColor.WHITE, "White Sheep"),
    ORANGE(1, DyeColor.ORANGE, "Orange Sheep"),
    MAGENTA(2, DyeColor.MAGENTA, "Magenta Sheep"),
    LIGHT_BLUE(3, DyeColor.LIGHT_BLUE, "Light Blue Sheep"),
    YELLOW(4, DyeColor.YELLOW, "Yellow Sheep"),
    LIME(5, DyeColor.LIME, "Lime Sheep"),
    PINK(6, DyeColor.PINK, "Pink Sheep"),
    GRAY(7, DyeColor.GRAY, "Gray Sheep"),
    LIGHT_GRAY(8, DyeColor.LIGHT_GRAY, "Light Gray Sheep"),
    CYAN(9, DyeColor.CYAN, "Cyan Sheep"),
    PURPLE(10, DyeColor.PURPLE, "Purple Sheep"),
    BLUE(11, DyeColor.BLUE, "Blue Sheep"),
    BROWN(12, DyeColor.BROWN, "Brown Sheep"),
    GREEN(13, DyeColor.GREEN, "Green Sheep"),
    RED(14, DyeColor.RED, "Red Sheep"),
    BLACK(15, DyeColor.BLACK, "Black Sheep"),
    RAINBOW(16, null, "Rainbow Sheep");

    fun getPointsOnShear(): Int {
        var points = 1L
        for (index in 0 until level) {
            points *= 4L
            if (points >= Int.MAX_VALUE) {
                return Int.MAX_VALUE
            }
        }
        return points.toInt()
    }

    fun hasNext(): Boolean {
        return tierByLevel.containsKey(level + 1)
    }

    fun next(): SheepTier {
        return tierByLevel[level + 1] ?: this
    }

    companion object {
        private val tierByLevel: Map<Int, SheepTier> = entries.associateBy { it.level }

        @JvmStatic
        fun byLevel(level: Int): SheepTier {
            return tierByLevel[level] ?: WHITE
        }
    }
}
