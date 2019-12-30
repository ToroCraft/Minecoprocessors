package net.torocraft.minecoprocessors.gui;

public class ScaledGuiButton {}

//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.FontRenderer;
//import net.minecraft.client.gui.GuiButton;
//import net.minecraft.client.renderer.GlStateManager;
//
//public class ScaledGuiButton extends GuiButton {
//
//  public ScaledGuiButton(int id, int x, int y, int w, int h, String s) {
//    super(id, x, y, w, h, s);
//  }
//
//  public ScaledGuiButton(int id, int x, int y, String s) {
//    super(id, x, y, s);
//  }
//
//  @Override
//  public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
//    if (!visible) {
//      return;
//    }
//
//    int xScaled = x * 2;
//    int yScaled = y * 2;
//    int widthScaled = width * 2;
//    int heightScaled = height * 2;
//
//    hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
//
//    mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
//    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//
//    GlStateManager.enableBlend();
//    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
//        GlStateManager.DestFactor.ZERO);
//    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
//
//    GlStateManager.pushMatrix();
//    GlStateManager.scale(0.5d, 0.5d, 0.5d);
//
//    int hoverState = getHoverState(hovered);
//    drawTexturedModalRect(xScaled, yScaled, 0, 46 + hoverState * 20, widthScaled / 2, heightScaled);
//    drawTexturedModalRect(xScaled + widthScaled / 2, yScaled, 200 - widthScaled / 2, 46 + hoverState * 20, widthScaled / 2, heightScaled);
//    mouseDragged(mc, mouseX, mouseY);
//
//    int color = 14737632;
//    if (packedFGColour != 0) {
//      color = packedFGColour;
//    } else if (!enabled) {
//      color = 10526880;
//    } else if (hovered) {
//      color = 16777120;
//    }
//
//    FontRenderer fr = mc.fontRenderer;
//    drawCenteredString(fr, displayString, xScaled + widthScaled / 2, yScaled + (heightScaled - 8) / 2, color);
//
//    GlStateManager.popMatrix();
//  }
//
//}
