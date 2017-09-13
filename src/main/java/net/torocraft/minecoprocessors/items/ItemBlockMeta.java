package net.torocraft.minecoprocessors.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemBlockMeta extends ItemBlock {

  public ItemBlockMeta(Block block) {
    super(block);
    if (!(block instanceof IMetaBlockName)) {
      throw new IllegalArgumentException(block.getUnlocalizedName());
    }
    this.setMaxDamage(0);
    this.setHasSubtypes(true);
  }

  @Override
  public String getUnlocalizedName(ItemStack stack) {
    return ((IMetaBlockName) block).getSpecialName(stack);
  }

  @Override
  public int getMetadata(int damage) {
    return damage;
  }
}