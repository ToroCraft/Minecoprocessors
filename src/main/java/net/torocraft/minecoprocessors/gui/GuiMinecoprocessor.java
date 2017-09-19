package net.torocraft.minecoprocessors.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.blocks.ContainerMinecoprocessor;
import net.torocraft.minecoprocessors.blocks.TileEntityMinecoprocessor;
import net.torocraft.minecoprocessors.network.MessageProcessorAction;
import net.torocraft.minecoprocessors.network.MessageEnableGuiUpdates;
import net.torocraft.minecoprocessors.network.MessageProcessorAction.Action;
import net.torocraft.minecoprocessors.processor.Processor;
import net.torocraft.minecoprocessors.processor.Register;
import net.torocraft.minecoprocessors.util.InstructionUtil;

// TODO mouse hovers

public class GuiMinecoprocessor extends net.minecraft.client.gui.inventory.GuiContainer {

  private static final ResourceLocation BREWING_STAND_GUI_TEXTURES = new ResourceLocation(Minecoprocessors.MODID, "textures/gui/minecoprocessor.png");

  private final IInventory playerInventory;
  private final TileEntityMinecoprocessor minecoprocessor;

  private GuiButton buttonReset;
  private GuiButton buttonPause;
  private GuiButton buttonStep;
  private GuiButton buttonHelp;

  private Processor processor;

  public BlockPos getPos() {
    return minecoprocessor.getPos();
  }

  // TODO add sequence number to packets and ignore it if it is older than the last received on

  public static GuiMinecoprocessor INSTANCE;

  public GuiMinecoprocessor(IInventory playerInv, TileEntityMinecoprocessor te) {
    super(new ContainerMinecoprocessor(playerInv, te));
    this.playerInventory = playerInv;
    this.minecoprocessor = te;
    INSTANCE = this;
    Minecoprocessors.NETWORK.sendToServer(new MessageEnableGuiUpdates(minecoprocessor.getPos(), true));
  }

  public void updateData(NBTTagCompound processorData, String name) {
    if (processor == null) {
      processor = new Processor();
    }
    processor.readFromNBT(processorData);
    minecoprocessor.setName(I18n.format(name));
  }

  @Override
  public void onGuiClosed() {
    super.onGuiClosed();
    Minecoprocessors.NETWORK.sendToServer(new MessageEnableGuiUpdates(minecoprocessor.getPos(), false));
    INSTANCE = null;
  }

  /**
   * Draw the foreground layer for the GuiContainer (everything in front of the items)
   */
  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {

    GlStateManager.pushMatrix();
    GlStateManager.scale(0.5d, 0.5d, 0.5d);
    int scale = 2;
    int y = 20 * scale;

    byte[] registers = processor == null ? null : processor.getRegisters();

    y = 50;
    drawRegister(Register.A, 130 * 2, y, mouseX, mouseY);
    drawRegister(Register.B, 139 * 2, y, mouseX, mouseY);
    drawRegister(Register.C, 148 * 2, y, mouseX, mouseY);
    drawRegister(Register.D, 157 * 2, y, mouseX, mouseY);

    y = 82;
    drawFlag("Z", processor == null ? null : processor.isZero(), 130 * 2, y, mouseX, mouseY);
    drawFlag("C", processor == null ? null : processor.isCarry() || processor.isOverflow(), 139 * 2, y, mouseX, mouseY);
    drawFlag("F", processor == null ? null : processor.isFault(), 148 * 2, y, 0xff0000, mouseX, mouseY);
    drawFlag("S", processor == null ? null : processor.isWait(), 157 * 2, y, 0x00ff00, mouseX, mouseY);

    y = 114;
    drawLabeledShort("IP", processor == null ? null : processor.getIp(), 130 * 2, y, mouseX, mouseY);
    // drawLabeledByte("TEMP", processor == null ? null : processor.getTemp(), 157 * 2, y);

    //drawTemp(mouseX, mouseY, y);

    centered(toHex(registers == null ? null : registers[Register.PF.ordinal()]), 176, 47);
    centered(toHex(registers == null ? null : registers[Register.PR.ordinal()]), 216, 86);
    centered(toHex(registers == null ? null : registers[Register.PL.ordinal()]), 137, 86);
    centered(toHex(registers == null ? null : registers[Register.PB.ordinal()]), 176, 125);

    drawCode();

    GlStateManager.popMatrix();

    drawGuiTitle();
    drawInventoryTitle();

    String pauseText = "gui.button.sleep";
    if (processor == null || processor.isWait()) {
      pauseText = "gui.button.wake";
    }
    buttonPause.displayString = I18n.format(pauseText);

    buttonStep.enabled = processor != null && processor.isWait();
  }

  @SuppressWarnings("unused")
  private void drawTemp(int mouseX, int mouseY, int y) {
    Integer tempColor = null;
    if (processor != null && processor.isHot()) {
      tempColor = 0xff0000;
    }
    drawLabeledValue("TEMP", processor == null ? null : Integer.toUnsignedString(processor.getTemp()), 157 * 2, y, tempColor, mouseX, mouseY);
  }

  private void drawCode() {
    int x = 22;
    int y = 50;

    String label = "NEXT";

    byte[] a = null;
    try {
      a = processor.getProgram().get(processor.getIp());
    } catch (Exception e) {
      a = null;
    }

    int color = 0xffffff;

    String value = "";

    if (a != null) {
      value = InstructionUtil.compileLine(a, processor.getLabels(), (short) -1);
    }

    if (value.isEmpty() && processor != null && processor.getError() != null) {
      value = processor.getError();
      color = 0xff0000;
    }

    fontRenderer.drawString(label, x - 4, y - 14, 0x404040);
    fontRenderer.drawString(value, x, y, color);
  }

  private void drawRegister(Register register, int x, int y, int mouseX, int mouseY) {
    byte[] registers = processor == null ? null : processor.getRegisters();
    String label = register.toString();
    String value = toHex(registers == null ? null : registers[register.ordinal()]);
    drawLabeledValue(label, value, x, y, null, mouseX, mouseY);
  }

  private static String toHex(Byte b) {
    if (b == null) {
      return null;
    }
    String s = Integer.toHexString(b);
    if (s.length() > 2) {
      return s.substring(s.length() - 2, s.length());
    }
    if (s.length() < 2) {
      s = "0" + s;
    }
    return s;
  }

  private static String toHex(Short b) {
    if (b == null) {
      return null;
    }
    String s = Integer.toHexString(b);
    if (s.length() > 4) {
      return s.substring(s.length() - 4, s.length());
    }
    // TODO make this better 😲
    if (s.length() < 2) {
      s = "0" + s;
    }
    if (s.length() < 3) {
      s = "0" + s;
    }
    if (s.length() < 4) {
      s = "0" + s;
    }
    return s;
  }

  private void drawFlag(String label, Boolean flag, int x, int y, int mouseX, int mouseY) {
    drawFlag(label, flag, x, y, null, mouseX, mouseY);
  }

  private void drawFlag(String label, Boolean flag, int x, int y, Integer flashColor, int mouseX, int mouseY) {
    if (flag == null) {
      flag = false;
    }
    drawLabeledValue(label, flag ? "1" : "0", x, y, flag ? flashColor : null, mouseX, mouseY);
  }

  @SuppressWarnings("unused")
  private void drawLabeledByte(String label, Byte b, int x, int y, int mouseX, int mouseY) {
    drawLabeledValue(label, toHex(b), x, y, null, mouseX, mouseY);
  }

  private void drawLabeledShort(String label, Short b, int x, int y, int mouseX, int mouseY) {
    drawLabeledValue(label, toHex(b), x, y, null, mouseX, mouseY);
  }

  private void drawLabeledValue(String label, String value, int x, int y, Integer flashColor, int mouseX, int mouseY) {
    int wLabel = fontRenderer.getStringWidth(label) / 2;
    int wValue = 0;
    if (value != null) {
      wValue = fontRenderer.getStringWidth(value) / 2;
    }

    int color = 0xffffff;

    if (flashColor != null && (minecoprocessor.getWorld().getTotalWorldTime() / 10) % 2 == 0) {
      color = flashColor;
    }

    fontRenderer.drawString(label, x - wLabel, y - 14, 0x404040);
    if (value != null) {
      fontRenderer.drawString(value, x - wValue, y, color);
    }
  }

  private void centered(String s, float x, float y) {
    int xs = (int) x - fontRenderer.getStringWidth(s) / 2;
    int ys = (int) y - fontRenderer.FONT_HEIGHT / 2;
    fontRenderer.drawString(s, xs, ys, 0xffffff);
  }

  private void drawInventoryTitle() {
    fontRenderer.drawString(playerInventory.getDisplayName().getUnformattedText(), 8, ySize - 96 + 2, 4210752);
  }

  private void drawGuiTitle() {
    String s = minecoprocessor.getDisplayName().getUnformattedText();
    fontRenderer.drawString(s, xSize / 2 - fontRenderer.getStringWidth(s) / 2, 6, 4210752);
  }

  @Override
  public void initGui() {
    super.initGui();
    drawButtons();

  }

  private void drawButtons() {
    int buttonId = 0;
    int x = 8 + guiLeft;
    int y = 34 + guiTop;
    int buttonWidth = 49;
    int buttonHeight = 10;

    buttonReset = new ScaledGuiButton(buttonId++, x, y, buttonWidth, buttonHeight, I18n.format("gui.button.reset"));
    buttonPause = new ScaledGuiButton(buttonId++, x, y + 11, buttonWidth, buttonHeight, I18n.format("gui.button.sleep"));
    buttonStep = new ScaledGuiButton(buttonId++, x, y + 22, buttonWidth, buttonHeight, I18n.format("gui.button.step"));
    buttonHelp = new ScaledGuiButton(buttonId++, guiLeft + 133, guiTop + 66, 35, buttonHeight, I18n.format("gui.button.help"));

    buttonList.add(buttonReset);
    buttonList.add(buttonStep);
    buttonList.add(buttonPause);
    buttonList.add(buttonHelp);
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    if (button == buttonReset) {
      Minecoprocessors.NETWORK.sendToServer(new MessageProcessorAction(minecoprocessor.getPos(), Action.RESET));
    }
    if (button == buttonPause) {
      Minecoprocessors.NETWORK.sendToServer(new MessageProcessorAction(minecoprocessor.getPos(), Action.PAUSE));
    }
    if (button == buttonStep) {
      Minecoprocessors.NETWORK.sendToServer(new MessageProcessorAction(minecoprocessor.getPos(), Action.STEP));
    }
    if (button == buttonHelp) {
      ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
      // TODO add NBT content
      this.mc.displayGuiScreen(new GuiScreenBook(mc.player, stack, false));
    }
  }

  /**
   * Draws the background layer of this container (behind the items).
   */
  @Override
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
      int j1 = (int) (28.0F * (1.0F - i1 / 400.0F));

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
