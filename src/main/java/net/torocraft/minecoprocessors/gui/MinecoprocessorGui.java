/*
 * @file MinecoprocessorGui.java
 * @license GPL
 */
package net.torocraft.minecoprocessors.gui;


import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ReadBookScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.torocraft.minecoprocessors.ModMinecoprocessors;
import net.torocraft.minecoprocessors.blocks.MinecoprocessorContainer;
import net.torocraft.minecoprocessors.blocks.MinecoprocessorTileEntity;
import net.torocraft.minecoprocessors.blocks.MinecoprocessorTileEntity.ContainerSyncFields;
import net.torocraft.minecoprocessors.processor.Register;
import net.torocraft.minecoprocessors.util.BookCreator;
import net.torocraft.minecoprocessors.util.GuiUtil;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MinecoprocessorGui extends ContainerScreen<MinecoprocessorContainer>
{
  private static final ResourceLocation BACKGROUND_IMAGE = new ResourceLocation(ModMinecoprocessors.MODID, "textures/gui/minecoprocessor.png");
  private static final double GUI_SCALE = 0.5;
  private Button buttonSleep;
  private Button buttonStep;
  private Button buttonReset;
  private Button buttonHelp;

  protected final PlayerEntity player_;
  private final List<String> hoveredFeature = new ArrayList<>(5);

  public MinecoprocessorGui(MinecoprocessorContainer container, PlayerInventory player_inventory, ITextComponent title)
  {
    super(container, player_inventory, title);
    this.player_ = player_inventory.player;
  }

  @Override
  public void init()
  {
    super.init();
    final int x0 = getGuiLeft();
    final int y0 = getGuiTop();
    final int w = 49;
    final int h = 10;
    buttons.clear();
    // buttonPause = new ScaledGuiButton(buttonId++, x+8, y + 11, buttonWidth, buttonHeight, I18n.format("gui.button.sleep"));
    buttons.add(buttonSleep = addButton(
      new ScaledButton(GUI_SCALE, x0+8,y0+34+(0*(h+2)), w,h, (new TranslationTextComponent("minecoprocessors.gui.button.sleep")).getFormattedText(),
        (bt)->getContainer().onGuiAction("sleep",1))
    ));
    // buttonReset = new ScaledGuiButton(buttonId++, x+8, y, buttonWidth, buttonHeight, I18n.format("gui.button.reset"));
    buttons.add(buttonReset = addButton(
      new ScaledButton(GUI_SCALE, x0+8,y0+34+(1*(h+2)), w,h, (new TranslationTextComponent("minecoprocessors.gui.button.reset")).getFormattedText(),
      (bt)->getContainer().onGuiAction("reset",1))
    ));
    // buttonStep = new ScaledGuiButton(buttonId++, x+8, y + 22, buttonWidth, buttonHeight, I18n.format("gui.button.step"));
    buttons.add(buttonStep = addButton(
      new ScaledButton(GUI_SCALE, x0+8,y0+34+(2*(h+2)), w,h, (new TranslationTextComponent("minecoprocessors.gui.button.step")).getFormattedText(),
      (bt)->getContainer().onGuiAction("step", 1))
    ));
    // buttonHelp = new ScaledGuiButton(buttonId++, guiLeft + 133, guiTop + 66, 35, buttonHeight, I18n.format("gui.button.help"));
    buttons.add(buttonHelp = addButton(
      new ScaledButton(GUI_SCALE, x0+133,y0+66, 35,h, (new TranslationTextComponent("minecoprocessors.gui.button.help")).getFormattedText(),
      (bt)->{ Minecraft.getInstance().displayGuiScreen(new ReadBookScreen(ReadBookScreen.IBookInfo.func_216917_a(BookCreator.getManual()))); })
    ));
    buttonStep.active = false;
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks)
  {
    renderBackground();
    super.render(mouseX, mouseY, partialTicks);
    renderHoveredToolTip(mouseX, mouseY);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
  {
    GlStateManager.color4f(1f, 1f, 1f, 1f);
    getMinecraft().getTextureManager().bindTexture(BACKGROUND_IMAGE);
    final int x0=getGuiLeft(), y0=getGuiTop(), w=getXSize(), h=getYSize();
    blit(x0, y0, 0, 0, w, h);
    MinecoprocessorContainer container = (MinecoprocessorContainer)getContainer();
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
  {
    hoveredFeature.clear();
    ContainerSyncFields fields = getContainer().getFields();
    GlStateManager.pushMatrix();
    GlStateManager.scaled(GUI_SCALE, GUI_SCALE, GUI_SCALE);
    mouseX -= getGuiLeft();
    mouseY -= getGuiTop();

    int y;
    y = 50;
    drawRegister(Register.A, 130 * 2, y, mouseX, mouseY);
    drawRegister(Register.B, 139 * 2, y, mouseX, mouseY);
    drawRegister(Register.C, 148 * 2, y, mouseX, mouseY);
    drawRegister(Register.D, 157 * 2, y, mouseX, mouseY);

    y = 82;
    drawFlag("Z", fields.isZero(), 130 * 2, y, mouseX, mouseY);
    drawFlag("C", fields.isCarry() || fields.isOverflow(), 139 * 2, y, mouseX, mouseY);
    drawFlag("F", fields.isFault(), 148 * 2, y, 0xff0000, mouseX, mouseY);
    drawFlag("S", fields.isWait(), 157 * 2, y, 0x00ff00, mouseX, mouseY);

    y = 114;

    boolean mouseIsOver = drawLabeledValue("IP", GuiUtil.toHex(fields.ip()), 128*2, y, null, mouseX, mouseY);
    if(mouseIsOver) hoveredFeature.add("Instruction Pointer");

    drawRegister(Register.ADC, 142 * 2, y, mouseX, mouseY);
    drawRegister(Register.PORTS, 158 * 2, y, mouseX, mouseY);
    drawPortRegister(Register.PF, 176, 47, mouseX, mouseY);
    drawPortRegister(Register.PR, 216, 86, mouseX, mouseY);
    drawPortRegister(Register.PL, 137, 86, mouseX, mouseY);
    drawPortRegister(Register.PB, 176, 125, mouseX, mouseY);

    //drawCode();

    GlStateManager.popMatrix();
    drawGuiTitle();
    drawInventoryTitle();
    if(fields.isWait()) {
      buttonSleep.setMessage((new TranslationTextComponent("minecoprocessors.gui.button.wake")).getFormattedText());
      buttonStep.active = true;
    } else {
      buttonSleep.setMessage((new TranslationTextComponent("minecoprocessors.gui.button.sleep")).getFormattedText());
      buttonStep.active = false;
    }
  }

  @Override
  protected void renderHoveredToolTip(int mouseX, int mouseY)
  {
    super.renderHoveredToolTip(mouseX, mouseY);
    if(!hoveredFeature.isEmpty()) {
      renderTooltip(hoveredFeature, mouseX, mouseY);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  private void drawRegister(Register register, int x, int y, int mouseX, int mouseY)
  {
    String label = register.toString();
    byte value = getContainer().getFields().register(register.ordinal()-Register.A.ordinal());
    boolean mouseIsOver = drawLabeledValue(label, GuiUtil.toHex(value), x, y, null, mouseX, mouseY);
    if (mouseIsOver) {
      hoveredFeature.add(label + " Register");
      if (Register.PORTS.equals(register)) {
        hoveredFeature.add("I/O port direction");
      } else if (Register.ADC.equals(register)) {
        hoveredFeature.add("ADC/DAC switch");
      } else {
        hoveredFeature.add("General Purpose");
      }
      hoveredFeature.add(String.format("0x%s %sb %s", GuiUtil.toHex(value), GuiUtil.toBinary(value), Integer.toString(value, 10)));
    }
  }

  private void drawPortRegister(Register register, int x, int y, int mouseX, int mouseY)
  {
    final ContainerSyncFields fields = getContainer().getFields();
    int portIndex = register.ordinal() - Register.PF.ordinal();
    byte value = fields.port(portIndex);
    boolean mouseIsOver = centered(GuiUtil.toHex(value), x, y, mouseX, mouseY);
    if(mouseIsOver) {
      switch(register) {
        case PF:
          hoveredFeature.add("Front Port - PF");
          break;
        case PB:
          hoveredFeature.add("Back Port - PB");
          break;
        case PL:
          hoveredFeature.add("Left Port - PL");
          break;
        case PR:
          hoveredFeature.add("Right Port - PR");
          break;
      }
      if(MinecoprocessorTileEntity.isInOutputMode(fields.ports(), portIndex)) {
        hoveredFeature.add("Output Port");
      } else if(MinecoprocessorTileEntity.isInInputMode(fields.ports(), portIndex)) {
        hoveredFeature.add("Input Port");
      } else if(MinecoprocessorTileEntity.isInResetMode(fields.ports(), portIndex)) {
        hoveredFeature.add("Reset Port");
      }
      if(MinecoprocessorTileEntity.isADCMode(fields.adc(), portIndex)) {
        hoveredFeature.add("Analog Mode");
      } else {
        hoveredFeature.add("Digital Mode");
      }
      hoveredFeature.add(String.format("0x%s %sb %s", GuiUtil.toHex(value), GuiUtil.toBinary(value), Integer.toString(value, 10)));
    }
  }

  private void drawFlag(String label, Boolean flag, int x, int y, int mouseX, int mouseY)
  { drawFlag(label, flag, x, y, null, mouseX, mouseY); }

  private void drawFlag(String label, Boolean flag, int x, int y, Integer flashColor, int mouseX, int mouseY)
  {
    if(flag == null) flag = false;
    boolean mouseIsOver = drawLabeledValue(label, flag ? "1" : "0", x, y, flag ? flashColor : null, mouseX, mouseY);
    if (mouseIsOver) {
      switch (label) {
        case "Z":
          hoveredFeature.add("Zero Flag");
          break;
        case "C":
          hoveredFeature.add("Carry Flag");
          break;
        case "F":
          hoveredFeature.add("Fault Indicator");
          hoveredFeature.add("STATUS 0x" + GuiUtil.toHex(getContainer().getFields().fault()).toUpperCase());
          break;
        case "S":
          hoveredFeature.add("Sleep Indicator");
          break;
      }
      hoveredFeature.add(Boolean.toString(flag).toUpperCase());
    }
  }

  private boolean drawLabeledValue(String label, String value, int x, int y, Integer flashColor, int mouseX, int mouseY)
  {
    int wLabel = font.getStringWidth(label) / 2;
    int wValue = 0;
    if(value != null) {
      wValue = font.getStringWidth(value) / 2;
    }
    int color = 0xffffff;
    if((flashColor != null) && (ModMinecoprocessors.proxy.getWorldClientSide().getGameTime() & 0xf) == 0) {
      color = flashColor;
    }
    font.drawString(label, x - wLabel, y - 14, 0x404040);
    if(value != null) {
      font.drawString(value, x - wValue, y, color);
    }
    int wMax = Math.max(wLabel, wValue);
    boolean mouseIsOver = (mouseX > (x - wMax)) && (mouseX < (x + wMax));
    mouseIsOver = mouseIsOver && mouseY > y - 14 && mouseY < y + 14;
    return mouseIsOver;
  }

  private boolean centered(String s, float x, float y, int mouseX, int mouseY)
  {
    int hWidth = font.getStringWidth(s) / 2;
    int hHeight = font.FONT_HEIGHT / 2;
    int xs = (int) x - hWidth;
    int ys = (int) y - hHeight;
    font.drawString(s, xs, ys, 0xffffff);
    boolean mouseIsOver = mouseX > (x - hWidth) && mouseX < (x + hWidth);
    mouseIsOver = mouseIsOver && mouseY > y - hHeight - 2 && mouseY < y + hHeight + 2;
    return mouseIsOver;
  }

  private void drawInventoryTitle()
  {
    // @todo: implement
    //font.drawString(playerInventory.getDisplayName().getString(), 8, ySize - 96 + 2, 4210752);
  }

  private void drawGuiTitle()
  {
    // @todo: implement
    //String s = getContainer()  .getUnformattedText();
    //font.drawString(s, xSize / 2 - font.getStringWidth(s) / 2, 6, 4210752);
  }


}

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
