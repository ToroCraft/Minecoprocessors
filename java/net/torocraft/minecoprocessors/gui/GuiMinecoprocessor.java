package net.torocraft.minecoprocessors.gui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.blocks.ContainerMinecoprocessor;

public class GuiMinecoprocessor extends net.minecraft.client.gui.inventory.GuiContainer {

	private static final ResourceLocation BREWING_STAND_GUI_TEXTURES = new ResourceLocation(Minecoprocessors.MODID, "textures/gui/minecoprocessor.png");
	private static final int[] BUBBLELENGTHS = new int[] { 29, 24, 20, 16, 11, 6, 0 };
	/** The player inventory bound to this GUI. */
	private final IInventory playerInventory;
	private final IInventory tileBrewingStand;

	public GuiMinecoprocessor(IInventory playerInv, IInventory p_i45506_2_) {
		super(new ContainerMinecoprocessor(playerInv, p_i45506_2_));
		this.playerInventory = playerInv;
		this.tileBrewingStand = p_i45506_2_;
	}

	/**
	 * Draw the foreground layer for the GuiContainer (everything in front of
	 * the items)
	 */
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		String s = this.tileBrewingStand.getDisplayName().getUnformattedText();
		this.fontRendererObj.drawString(s, this.xSize / 2 - this.fontRendererObj.getStringWidth(s) / 2, 6, 4210752);
		this.fontRendererObj.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
	}

	/**
	 * Draws the background layer of this container (behind the items).
	 */
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(BREWING_STAND_GUI_TEXTURES);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
		int k = this.tileBrewingStand.getField(1);
		int l = MathHelper.clamp((18 * k + 20 - 1) / 20, 0, 18);

		if (l > 0) {
			this.drawTexturedModalRect(i + 60, j + 44, 176, 29, l, 4);
		}

		int i1 = this.tileBrewingStand.getField(0);

		if (i1 > 0) {
			int j1 = (int) (28.0F * (1.0F - (float) i1 / 400.0F));

			if (j1 > 0) {
				this.drawTexturedModalRect(i + 97, j + 16, 176, 0, 9, j1);
			}

			j1 = BUBBLELENGTHS[i1 / 2 % 7];

			if (j1 > 0) {
				this.drawTexturedModalRect(i + 63, j + 14 + 29 - j1, 185, 29 - j1, 12, j1);
			}
		}
	}
}