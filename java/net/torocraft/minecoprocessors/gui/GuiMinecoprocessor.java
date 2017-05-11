package net.torocraft.minecoprocessors.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.blocks.ContainerMinecoprocessor;

// TODO loading arrow support

// TODO fault and paused light

// TODO processor diagram

// TODO register readout current line

// TODO clock mode (20hz vs manual)

// TODO step button

// TODO reset and pause button

public class GuiMinecoprocessor extends net.minecraft.client.gui.inventory.GuiContainer {

  private static final ResourceLocation BREWING_STAND_GUI_TEXTURES =
      new ResourceLocation(Minecoprocessors.MODID, "textures/gui/minecoprocessor.png");

  private final IInventory playerInventory;
  private final IInventory minecoprocessor;

  private GuiButton buttonReset;

  public GuiMinecoprocessor(IInventory playerInv, IInventory p_i45506_2_) {
    super(new ContainerMinecoprocessor(playerInv, p_i45506_2_));
    this.playerInventory = playerInv;
    this.minecoprocessor = p_i45506_2_;
  }


  /**
   * Draw the foreground layer for the GuiContainer (everything in front of the items)
   */
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {


    GlStateManager.pushMatrix();
    GlStateManager.scale(0.5d, 0.5d, 0.5d);
    int scale = 2;
    int x = 130 * scale;
    int y = 20 * scale;
    fontRendererObj.drawString("A  B  C  D ", x, y - 5, 0x404040);
    fontRendererObj.drawString("0F 01 00 00", x, y + 10, 0xffffff);

    //PF
    centered(scale, "00", 88, 23);
    
    //PR
    centered(scale, "01", 108, 43);
    
    //PL
    centered(scale, "02", 68, 43);
    
    //PB
    centered(scale, "03", 88, 63);



    GlStateManager.popMatrix();

    drawGuiTitle();
    drawInventoryTitle();
  }


  private void centered(int scale, String s, float x, float y) {
    int xs = (int) x * scale - fontRendererObj.getStringWidth(s) / 2;
    int ys = (int) y * scale - fontRendererObj.FONT_HEIGHT / 2;
    fontRendererObj.drawString(s, xs, ys, 0xffffff);
  }



  private void drawInventoryTitle() {
    fontRendererObj.drawString(playerInventory.getDisplayName().getUnformattedText(), 8,
        ySize - 96 + 2, 4210752);
  }

  private void drawGuiTitle() {
    String s = minecoprocessor.getDisplayName().getUnformattedText();
    fontRendererObj.drawString(s, xSize / 2 - fontRendererObj.getStringWidth(s) / 2, 6, 4210752);
  }

  @Override
  public void initGui() {
    super.initGui();
    int buttonId = 0;
    buttonReset = new GuiButton(buttonId++, 10, 10, I18n.format("reset", (Object) null));
    buttonList.add(buttonReset);
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    if (button == buttonReset) {
      System.out.println("reset button pressed");
    }
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
    int k = this.minecoprocessor.getField(1);
    int l = MathHelper.clamp((18 * k + 20 - 1) / 20, 0, 18);

    if (l > 0) {
      this.drawTexturedModalRect(i + 60, j + 44, 176, 29, l, 4);
    }

    int i1 = this.minecoprocessor.getField(0);

    if (i1 > 0) {
      int j1 = (int) (28.0F * (1.0F - (float) i1 / 400.0F));

      if (j1 > 0) {
        this.drawTexturedModalRect(i + 97, j + 16, 176, 0, 9, j1);
      }

      j1 = 0;

      if (j1 > 0) {
        this.drawTexturedModalRect(i + 63, j + 14 + 29 - j1, 185, 29 - j1, 12, j1);
      }
    }
  }
}
