package net.torocraft.minecoprocessors.gui;


public class GuiMinecoprocessor {}

//import java.util.ArrayList;
//import java.util.List;
//import net.minecraft.client.gui.GuiButton;
//import net.minecraft.client.gui.GuiScreenBook;
//import net.minecraft.client.renderer.GlStateManager;
//import net.minecraft.client.resources.I18n;
//import net.minecraft.inventory.IInventory;
//import net.minecraft.nbt.NBTTagCompound;
//import net.minecraft.util.ResourceLocation;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.MathHelper;
//import net.torocraft.minecoprocessors.Minecoprocessors;
//import net.torocraft.minecoprocessors.blocks.ContainerMinecoprocessor;
//import net.torocraft.minecoprocessors.blocks.TileEntityMinecoprocessor;
//import net.torocraft.minecoprocessors.network.MessageEnableGuiUpdates;
//import net.torocraft.minecoprocessors.network.MessageProcessorAction;
//import net.torocraft.minecoprocessors.network.MessageProcessorAction.Action;
//import net.torocraft.minecoprocessors.processor.FaultCode;
//import net.torocraft.minecoprocessors.processor.Processor;
//import net.torocraft.minecoprocessors.processor.Register;
//import net.torocraft.minecoprocessors.util.BookCreator;
//import net.torocraft.minecoprocessors.util.InstructionUtil;
//
//public class GuiMinecoprocessor extends net.minecraft.client.gui.inventory.GuiContainer {
//
//  private static final ResourceLocation TEXTURES = new ResourceLocation(Minecoprocessors.MODID, "textures/gui/minecoprocessor.png");
//
//  private final IInventory playerInventory;
//  private final TileEntityMinecoprocessor minecoprocessor;
//  private final List<String> hoveredFeature = new ArrayList<>(5);
//
//  private GuiButton buttonReset;
//  private GuiButton buttonPause;
//  private GuiButton buttonStep;
//  private GuiButton buttonHelp;
//  private Processor processor;
//  private byte[] registers = new byte[Register.values().length];
//  private byte faultCode = FaultCode.FAULT_STATE_NOMINAL;
//
//  public BlockPos getPos() {
//    return minecoprocessor.getPos();
//  }
//
//  public static GuiMinecoprocessor INSTANCE;
//
//  public GuiMinecoprocessor(IInventory playerInv, TileEntityMinecoprocessor te) {
//    super(new ContainerMinecoprocessor(playerInv, te));
//    this.playerInventory = playerInv;
//    this.minecoprocessor = te;
//    INSTANCE = this;
//    Minecoprocessors.NETWORK.sendToServer(new MessageEnableGuiUpdates(minecoprocessor.getPos(), true));
//  }
//
//  public void updateData(NBTTagCompound processorData, String name) {
//    if (processor == null) {
//      processor = new Processor();
//    }
//    processor.readFromNBT(processorData);
//    minecoprocessor.setName(I18n.format(name));
//    registers = processor.getRegisters();
//    faultCode = processor.getFaultCode();
//  }
//
//  @Override
//  public void onGuiClosed() {
//    super.onGuiClosed();
//    Minecoprocessors.NETWORK.sendToServer(new MessageEnableGuiUpdates(minecoprocessor.getPos(), false));
//    INSTANCE = null;
//  }
//
//  /**
//   * Draw the foreground layer for the GuiContainer (everything in front of the items)
//   */
//  @Override
//  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
//    hoveredFeature.clear();
//
//    GlStateManager.pushMatrix();
//    GlStateManager.scale(0.5d, 0.5d, 0.5d);
//    int scale = 2;
//    int y;
//
//    mouseX = (mouseX - guiLeft) * scale;
//    mouseY = (mouseY - guiTop) * scale;
//
//    y = 50;
//    drawRegister(Register.A, 130 * 2, y, mouseX, mouseY);
//    drawRegister(Register.B, 139 * 2, y, mouseX, mouseY);
//    drawRegister(Register.C, 148 * 2, y, mouseX, mouseY);
//    drawRegister(Register.D, 157 * 2, y, mouseX, mouseY);
//
//    y = 82;
//    drawFlag("Z", processor == null ? null : processor.isZero(), 130 * 2, y, mouseX, mouseY);
//    drawFlag("C", processor == null ? null : processor.isCarry() || processor.isOverflow(), 139 * 2, y, mouseX, mouseY);
//    drawFlag("F", processor == null ? null : processor.isFault(), 148 * 2, y, 0xff0000, mouseX, mouseY);
//    drawFlag("S", processor == null ? null : processor.isWait(), 157 * 2, y, 0x00ff00, mouseX, mouseY);
//
//    y = 114;
//    boolean mouseIsOver = drawLabeledShort("IP", processor == null ? null : processor.getIp(), 128 * 2, y, mouseX, mouseY);
//    if (mouseIsOver) {
//      hoveredFeature.add("Instruction Pointer");
//    }
//
//    drawRegister(Register.ADC, 142 * 2, y, mouseX, mouseY);
//    drawRegister(Register.PORTS, 158 * 2, y, mouseX, mouseY);
//
//    drawPortRegister(Register.PF, 176, 47, mouseX, mouseY);
//    drawPortRegister(Register.PR, 216, 86, mouseX, mouseY);
//    drawPortRegister(Register.PL, 137, 86, mouseX, mouseY);
//    drawPortRegister(Register.PB, 176, 125, mouseX, mouseY);
//
//    drawCode();
//
//    GlStateManager.popMatrix();
//
//    drawGuiTitle();
//    drawInventoryTitle();
//
//    String pauseText = "gui.button.sleep";
//    if (processor == null || processor.isWait()) {
//      pauseText = "gui.button.wake";
//    }
//    buttonPause.displayString = I18n.format(pauseText);
//    buttonStep.enabled = processor != null && processor.isWait();
//  }
//
//  private void drawPortRegister(Register register, int x, int y, int mouseX, int mouseY) {
//    byte value = registers[register.ordinal()];
//    boolean mouseIsOver = centered(toHex(value), x, y, mouseX, mouseY);
//    if (mouseIsOver) {
//
//      int portIndex = register.ordinal() - Register.PF.ordinal();
//      byte ports = registers[Register.PORTS.ordinal()];
//      byte adc = registers[Register.ADC.ordinal()];
//
//      switch (register) {
//        case PF:
//          hoveredFeature.add("Front Port - PF");
//          break;
//        case PB:
//          hoveredFeature.add("Back Port - PB");
//          break;
//        case PL:
//          hoveredFeature.add("Left Port - PL");
//          break;
//        case PR:
//          hoveredFeature.add("Right Port - PR");
//          break;
//      }
//
//      if (TileEntityMinecoprocessor.isInOutputMode(ports, portIndex)) {
//        hoveredFeature.add("Output Port");
//      } else if (TileEntityMinecoprocessor.isInInputMode(ports, portIndex)) {
//        hoveredFeature.add("Input Port");
//      } else if (TileEntityMinecoprocessor.isInResetMode(ports, portIndex)) {
//        hoveredFeature.add("Reset Port");
//      }
//
//      if (TileEntityMinecoprocessor.isADCMode(adc, portIndex)) {
//        hoveredFeature.add("Analog Mode");
//      } else {
//        hoveredFeature.add("Digital Mode");
//      }
//
//      hoveredFeature.add(String.format("0x%s %sb %s", toHex(value), toBinary(value), Integer.toString(value, 10)));
//    }
//  }
//
//  private void drawCode() {
//    int x = 22;
//    int y = 50;
//
//    String label = "NEXT";
//
//    byte[] a = null;
//    if (processor != null) {
//      try {
//        int ip = processor.getIp();
//        List<byte[]> program = processor.getProgram();
//        if (ip < program.size()) {
//          a = program.get(ip);
//        }
//      } catch (Exception e) {
//        Minecoprocessors.proxy.handleUnexpectedException(e);
//      }
//    }
//
//    int color = 0xffffff;
//
//    String value = "";
//
//    if (a != null) {
//      value = InstructionUtil.compileLine(a, processor.getLabels(), (short) -1);
//    }
//
//    if (value.isEmpty() && processor != null && processor.getError() != null) {
//      value = processor.getError();
//      color = 0xff0000;
//    }
//
//    fontRenderer.drawString(label, x - 4, y - 14, 0x404040);
//    fontRenderer.drawString(value, x, y, color);
//  }
//
//  private void drawRegister(Register register, int x, int y, int mouseX, int mouseY) {
//    String label = register.toString();
//    byte value = registers[register.ordinal()];
//
//    boolean mouseIsOver = drawLabeledValue(label, toHex(value), x, y, null, mouseX, mouseY);
//    if (mouseIsOver) {
//      hoveredFeature.add(label + " Register");
//      if (Register.PORTS.equals(register)) {
//        hoveredFeature.add("I/O port direction");
//      } else if (Register.ADC.equals(register)) {
//        hoveredFeature.add("ADC/DAC switch");
//      } else {
//        hoveredFeature.add("General Purpose");
//      }
//      hoveredFeature.add(String.format("0x%s %sb %s", toHex(value), toBinary(value), Integer.toString(value, 10)));
//    }
//  }
//
//  public static String toBinary(Byte b) {
//    if (b == null) {
//      return null;
//    }
//    return maxLength(leftPad(Integer.toBinaryString(b), 8), 8);
//  }
//
//  private static String maxLength(String s, int l) {
//    if (s.length() > l) {
//      return s.substring(s.length() - l, s.length());
//    }
//    return s;
//  }
//
//  public static String toHex(Byte b) {
//    if (b == null) {
//      return null;
//    }
//    String s = Integer.toHexString(b);
//    if (s.length() > 2) {
//      return s.substring(s.length() - 2, s.length());
//    }
//    return leftPad(s, 2);
//  }
//
//  public static String leftPad(final String str, final int size) {
//    if (str == null) {
//      return null;
//    }
//    final int pads = size - str.length();
//    if (pads <= 0) {
//      return str;
//    }
//    StringBuilder buf = new StringBuilder();
//    for (int i = 0; i < pads; i++) {
//      buf.append("0");
//    }
//    buf.append(str);
//    return buf.toString();
//  }
//
//  private static String toHex(Short b) {
//    if (b == null) {
//      return null;
//    }
//    String s = Integer.toHexString(b);
//    if (s.length() > 4) {
//      return s.substring(s.length() - 4, s.length());
//    }
//    return leftPad(s, 4);
//  }
//
//  private void drawFlag(String label, Boolean flag, int x, int y, int mouseX, int mouseY) {
//    drawFlag(label, flag, x, y, null, mouseX, mouseY);
//  }
//
//  private void drawFlag(String label, Boolean flag, int x, int y, Integer flashColor, int mouseX, int mouseY) {
//    if (flag == null) {
//      flag = false;
//    }
//    boolean mouseIsOver = drawLabeledValue(label, flag ? "1" : "0", x, y, flag ? flashColor : null, mouseX, mouseY);
//    if (mouseIsOver) {
//      switch (label) {
//        case "Z":
//          hoveredFeature.add("Zero Flag");
//          break;
//        case "C":
//          hoveredFeature.add("Carry Flag");
//          break;
//        case "F":
//          hoveredFeature.add("Fault Indicator");
//          hoveredFeature.add("STATUS 0x" + toHex(faultCode).toUpperCase());
//          break;
//        case "S":
//          hoveredFeature.add("Sleep Indicator");
//          break;
//      }
//      hoveredFeature.add(Boolean.toString(flag).toUpperCase());
//    }
//  }
//
//  @SuppressWarnings("unused")
//  private void drawLabeledByte(String label, Byte b, int x, int y, int mouseX, int mouseY) {
//    drawLabeledValue(label, toHex(b), x, y, null, mouseX, mouseY);
//  }
//
//  private boolean drawLabeledShort(String label, Short b, int x, int y, int mouseX, int mouseY) {
//    return drawLabeledValue(label, toHex(b), x, y, null, mouseX, mouseY);
//  }
//
//  private boolean drawLabeledValue(String label, String value, int x, int y, Integer flashColor, int mouseX, int mouseY) {
//
//    int wLabel = fontRenderer.getStringWidth(label) / 2;
//    int wValue = 0;
//    if (value != null) {
//      wValue = fontRenderer.getStringWidth(value) / 2;
//    }
//
//    int color = 0xffffff;
//
//    if (flashColor != null && (minecoprocessor.getWorld().getTotalWorldTime() / 10) % 2 == 0) {
//      color = flashColor;
//    }
//
//    fontRenderer.drawString(label, x - wLabel, y - 14, 0x404040);
//    if (value != null) {
//      fontRenderer.drawString(value, x - wValue, y, color);
//    }
//
//    int wMax = Math.max(wLabel, wValue);
//    boolean mouseIsOver = mouseX > (x - wMax) && mouseX < (x + wMax);
//    mouseIsOver = mouseIsOver && mouseY > y - 14 && mouseY < y + 14;
//
//    return mouseIsOver;
//  }
//
//  private boolean centered(String s, float x, float y, int mouseX, int mouseY) {
//    int hWidth = fontRenderer.getStringWidth(s) / 2;
//    int hHeight = fontRenderer.FONT_HEIGHT / 2;
//    int xs = (int) x - hWidth;
//    int ys = (int) y - hHeight;
//    fontRenderer.drawString(s, xs, ys, 0xffffff);
//
//    boolean mouseIsOver = mouseX > (x - hWidth) && mouseX < (x + hWidth);
//    mouseIsOver = mouseIsOver && mouseY > y - hHeight - 2 && mouseY < y + hHeight + 2;
//    return mouseIsOver;
//  }
//
//  private void drawInventoryTitle() {
//    fontRenderer.drawString(playerInventory.getDisplayName().getUnformattedText(), 8, ySize - 96 + 2, 4210752);
//  }
//
//  private void drawGuiTitle() {
//    String s = minecoprocessor.getDisplayName().getUnformattedText();
//    fontRenderer.drawString(s, xSize / 2 - fontRenderer.getStringWidth(s) / 2, 6, 4210752);
//  }
//
//  @Override
//  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
//    super.drawScreen(mouseX, mouseY, partialTicks);
//    renderHoveredToolTip(mouseX, mouseY);
//    renderFeatureToolTip(mouseX, mouseY);
//  }
//
//  private void renderFeatureToolTip(int x, int y) {
//    if (hoveredFeature.size() == 0) {
//      return;
//    }
//    drawHoveringText(hoveredFeature, x, y, fontRenderer);
//  }
//
//  @Override
//  public void initGui() {
//    super.initGui();
//    drawButtons();
//  }
//
//  private void drawButtons() {
//    int buttonId = 0;
//    int x = 8 + guiLeft;
//    int y = 34 + guiTop;
//    int buttonWidth = 49;
//    int buttonHeight = 10;
//
//    buttonReset = new ScaledGuiButton(buttonId++, x, y, buttonWidth, buttonHeight, I18n.format("gui.button.reset"));
//    buttonPause = new ScaledGuiButton(buttonId++, x, y + 11, buttonWidth, buttonHeight, I18n.format("gui.button.sleep"));
//    buttonStep = new ScaledGuiButton(buttonId++, x, y + 22, buttonWidth, buttonHeight, I18n.format("gui.button.step"));
//    buttonHelp = new ScaledGuiButton(buttonId++, guiLeft + 133, guiTop + 66, 35, buttonHeight, I18n.format("gui.button.help"));
//
//    buttonList.add(buttonReset);
//    buttonList.add(buttonStep);
//    buttonList.add(buttonPause);
//    buttonList.add(buttonHelp);
//  }
//
//  @Override
//  protected void actionPerformed(GuiButton button) {
//    if (button == buttonReset) {
//      Minecoprocessors.NETWORK.sendToServer(new MessageProcessorAction(minecoprocessor.getPos(), Action.RESET));
//    }
//    if (button == buttonPause) {
//      Minecoprocessors.NETWORK.sendToServer(new MessageProcessorAction(minecoprocessor.getPos(), Action.PAUSE));
//    }
//    if (button == buttonStep) {
//      Minecoprocessors.NETWORK.sendToServer(new MessageProcessorAction(minecoprocessor.getPos(), Action.STEP));
//    }
//    if (button == buttonHelp) {
//      // TODO override the book GUI so that it returns the processor GUI when closed
//      this.mc.displayGuiScreen(new GuiScreenBook(mc.player, BookCreator.manual, false));
//    }
//  }
//
//  /**
//   * Draws the background layer of this container (behind the items).
//   */
//  @Override
//  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
//    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//    this.mc.getTextureManager().bindTexture(TEXTURES);
//    int i = (this.width - this.xSize) / 2;
//    int j = (this.height - this.ySize) / 2;
//    this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
//    int k = this.minecoprocessor.getField(1);
//    int l = MathHelper.clamp((18 * k + 20 - 1) / 20, 0, 18);
//
//    if (l > 0) {
//      this.drawTexturedModalRect(i + 60, j + 44, 176, 29, l, 4);
//    }
//
//    int i1 = this.minecoprocessor.getField(0);
//
//    if (i1 > 0) {
//      int j1 = (int) (28.0F * (1.0F - i1 / 400.0F));
//
//      if (j1 > 0) {
//        this.drawTexturedModalRect(i + 97, j + 16, 176, 0, 9, j1);
//      }
//
//      j1 = 0;
//
//      if (j1 > 0) {
//        this.drawTexturedModalRect(i + 63, j + 14 + 29 - j1, 185, 29 - j1, 12, j1);
//      }
//    }
//  }
//}
