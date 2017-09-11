package net.torocraft.minecoprocessors.items;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
		return super.getUnlocalizedName(stack) + "." + ((IMetaBlockName) block).getSpecialName(stack);
	}

	@Override
	public int getMetadata(int damage) {
		return damage;
	}
}