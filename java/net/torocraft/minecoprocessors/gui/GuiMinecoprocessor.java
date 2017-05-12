package net.torocraft.minecoprocessors.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.blocks.ContainerMinecoprocessor;
import net.torocraft.minecoprocessors.blocks.TileEntityMinecoprocessor;
import net.torocraft.minecoprocessors.processor.Register;

// TODO loading arrow support

// TODO fault and paused light

// TODO processor diagram

// TODO register readout current line

// TODO clock mode (20hz vs manual)

// TODO step button

// TODO reset and pause button

public class GuiMinecoprocessor extends net.minecraft.client.gui.inventory.GuiContainer {

  private static final ResourceLocation BREWING_STAND_GUI_TEXTURES = new ResourceLocation(Minecoprocessors.MODID, "textures/gui/minecoprocessor.png");

  private final IInventory playerInventory;
  private final TileEntityMinecoprocessor minecoprocessor;

  private byte[] registers = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

  private GuiButton buttonReset;
  private GuiButton buttonPause;
  private GuiButton buttonStep;

  public GuiMinecoprocessor(IInventory playerInv, TileEntityMinecoprocessor te) {
    super(new ContainerMinecoprocessor(playerInv, te));
    this.playerInventory = playerInv;
    this.minecoprocessor = te;
  }

  /**
   * Draw the foreground layer for the GuiContainer (everything in front of the items)
   */
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {

    GlStateManager.pushMatrix();
    GlStateManager.scale(0.5d, 0.5d, 0.5d);
    int scale = 2;
    int y = 20 * scale;
    // fontRendererObj.drawString("A B C D ", x, y - 5, 0x404040);

    // fontRendererObj.drawString("0F 01 00 00", x, y + 10, 0xffffff);

    y = 50;
    drawRegister(Register.A, 130 * 2, y);
    drawRegister(Register.B, 139 * 2, y);
    drawRegister(Register.C, 148 * 2, y);
    drawRegister(Register.D, 157 * 2, y);

    y = 82;
    drawFlag("Z", true, 130 * 2, y);
    drawFlag("C", false, 139 * 2, y);
    drawFlag("F", false, 148 * 2, y);
    drawFlag("S", true, 157 * 2, y);

    // PF
    centered("00", 176, 47);

    // PR
    centered("01", 216, 86);

    // PL
    centered("02", 137, 86);

    // PB
    centered("03", 176, 125);

    drawCode();

    GlStateManager.popMatrix();

    drawGuiTitle();
    drawInventoryTitle();

    drawButtons();

  }

  private void drawCode() {
    int x = 22;
    int y = 50;

    String label = "NEXT 0F";
    String value = "MOV A, B";

    fontRendererObj.drawString(label, x - 4, y - 14, 0x404040);
    fontRendererObj.drawString(value, x, y, 0xffffff);
  }

  private void drawRegister(Register register, int x, int y) {
    String label = register.toString();
    String value = Integer.toHexString(registers[register.ordinal()]).substring(0, 2);
    drawLabeledValue(label, value, x, y);
  }

  private void drawFlag(String label, boolean flag, int x, int y) {
    String value = flag ? "1" : "0";
    drawLabeledValue(label, value, x, y);
  }

  private void drawLabeledValue(String label, String value, int x, int y) {
    int wLabel = fontRendererObj.getStringWidth(label) / 2;
    int wValue = fontRendererObj.getStringWidth(value) / 2;

    fontRendererObj.drawString(label, x - wLabel, y - 14, 0x404040);
    fontRendererObj.drawString(value, x - wValue, y, 0xffffff);
  }

  private void centered(String s, float x, float y) {
    int xs = (int) x - fontRendererObj.getStringWidth(s) / 2;
    int ys = (int) y - fontRendererObj.FONT_HEIGHT / 2;
    fontRendererObj.drawString(s, xs, ys, 0xffffff);
  }

  private void drawInventoryTitle() {
    fontRendererObj.drawString(playerInventory.getDisplayName().getUnformattedText(), 8, ySize - 96 + 2, 4210752);
  }

  private void drawGuiTitle() {
    String s = minecoprocessor.getDisplayName().getUnformattedText();
    fontRendererObj.drawString(s, xSize / 2 - fontRendererObj.getStringWidth(s) / 2, 6, 4210752);
  }

  @Override
  public void initGui() {
    super.initGui();

  }

  private void drawButtons() {
    int buttonId = 0;
    int x = 8 + guiLeft;
    int y = 34 + guiTop;
    int width = 49;
    int height = 10;

    buttonReset = new ScaledGuiButton(buttonId++, x, y, width, height, I18n.format("gui.button.reset", (Object) null));
    buttonPause = new ScaledGuiButton(buttonId++, x, y + 11, width, height, I18n.format("gui.button.pause", (Object) null));
    buttonStep = new ScaledGuiButton(buttonId++, x, y + 22, width, height, I18n.format("gui.button.step", (Object) null));
    
    buttonStep.enabled = false;

    buttonList.add(buttonReset);
    buttonList.add(buttonStep);
    buttonList.add(buttonPause);
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    if (button == buttonReset) {
      System.out.println("Reset Button Pressed");
    }
    if (button == buttonPause) {
      System.out.println("Pause Button Pressed");
    }
    if (button == buttonStep) {
      System.out.println("Step Button Pressed");
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
