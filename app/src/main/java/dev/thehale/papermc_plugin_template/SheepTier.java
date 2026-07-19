package dev.thehale.papermc_plugin_template;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.DyeColor;

public enum SheepTier {
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

    private static final Map<Integer, SheepTier> TIER_BY_LEVEL = Arrays.stream(values())
            .collect(Collectors.toMap(SheepTier::getLevel, tier -> tier));

    private final int level;
    private final DyeColor color;
    private final String displayName;

    SheepTier(int level, DyeColor color, String displayName) {
        this.level = level;
        this.color = color;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public DyeColor getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPointsOnShear() {
        long points = 1L;
        for (int i = 0; i < level; i++) {
            points *= 4L;
            if (points >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) points;
    }

    public boolean hasNext() {
        return TIER_BY_LEVEL.containsKey(level + 1);
    }

    public SheepTier next() {
        return hasNext() ? TIER_BY_LEVEL.get(level + 1) : this;
    }

    public static SheepTier byLevel(int level) {
        return TIER_BY_LEVEL.getOrDefault(level, WHITE);
    }
}
