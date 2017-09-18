package net.torocraft.minecoprocessors;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RangeInt;

@Config(modid = Minecoprocessors.MODID)
public class Settings {
    @Comment("The maximum number of characters a single line in the code book may have.")
    @RangeInt(min = 1, max = 80)
    public static int maxColumnsPerLine = 18;
}
