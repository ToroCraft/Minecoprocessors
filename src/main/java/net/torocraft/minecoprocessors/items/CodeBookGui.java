/*
 * @file CodeBookGui.java
 * @license GPL
 */
package net.torocraft.minecoprocessors.items;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.torocraft.minecoprocessors.ModConfig;
import net.torocraft.minecoprocessors.ModMinecoprocessors;
import net.torocraft.minecoprocessors.util.StringUtil;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.Label;
import net.torocraft.minecoprocessors.util.ParseException;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;


@OnlyIn(Dist.CLIENT)
public class CodeBookGui extends ContainerScreen<CodeBookContainer>
{
  private static final ResourceLocation BACKGROUND_IMAGE = new ResourceLocation(ModMinecoprocessors.MODID, "textures/gui/codebook_gui.png");
  private static final int MAX_LINES_PER_PAGE = 25;
  private static final int GUI_WIDTH = 180;
  private static final int GUI_HEIGHT = 230;
  private static final int BUTTON_PAGE_CHANGE_PREV_X = 8+20;
  private static final int BUTTON_PAGE_CHANGE_NEXT_X = 116+20;
  private static final int BUTTON_PAGE_CHANGE_Y = 224;
  private static final int BUTTON_PAGE_DELETE_X = 66+20;
  private static final int BUTTON_PAGE_DELETE_Y = 224;
  private static final int CODE_POS_X = 18+30;
  private static final int CODE_POS_Y = 16;
  private static final int CODE_WIDTH = 120;
  private static final int CODE_MARGIN = 10;
  private static final int CODE_POS_X_IP_HINT_OFFSET = -28;
  private static final int PAGE_NUMBER_X = 92;
  private static final int PAGE_NUMBER_Y = 212;
  private static final int BUTTON_CLOSE_X = 153;
  private static final int BUTTON_CLOSE_Y = 7;

  private ImageButton buttonNextPage;
  private ImageButton buttonPreviousPage;
  private ImageButton buttonDeletePage;
  private ImageButton buttonClose;
  private final CodeBookItem.Data data;
  private final List<StringBuilder> lines = new ArrayList<>();
  private final Deque<String> undoBuffer = new ArrayDeque<>();
  private final List<String> redoBuffer = new ArrayList<>();
  private final List<String> tooltip = new ArrayList<>();
  private final List<ParseException> compileError = new ArrayList<>();
  private final List<Integer> instructionIds = new ArrayList<>();
  private final PlayerEntity player;
  private final boolean isEditingCode = true; // currently final, no slots in the GUI
  private int selectionStart = 0;
  private int selectionEnd = 0;
  private boolean hasMouseClicked = false;

  // -------------------------------------------------------------------------------------------------------------------

  private static int getMaxColumns()
  { return MathHelper.clamp(ModConfig.maxColumnsPerLine, 8, 20); }

  private static int getMaxLinesPerPage()
  { return MathHelper.clamp(ModConfig.maxLinesPerPage, 12, MAX_LINES_PER_PAGE); }

  private static int getCodeColor()
  { return ModConfig.codeBookTextColor; }

  private static int getSelectionColor()
  { return ModConfig.codeBookSelectedTextColor; }

  private static int getSelectionBackgroundColor()
  { return ModConfig.codeBookSelectedBackgroundColor; }

  private static int getInstructionNoColor()
  { return ModConfig.codeBookInstructionNoColor; }

  private static int getMaxUndoSteps()
  { return ModConfig.maxUndoSteps; }

  // -------------------------------------------------------------------------------------------------------------------

  public CodeBookGui(CodeBookContainer container, PlayerInventory player_inventory, ITextComponent title)
  {
    super(container, player_inventory, title);
    player = player_inventory.player;
    data = container.getData();
  }

  @Override
  public boolean shouldCloseOnEsc()
  { return false; }

  @Override
  public boolean isPauseScreen()
  { return false; }

  @Override
  public void init()
  {
    super.init();
    xSize = GUI_WIDTH;
    ySize = GUI_HEIGHT;
    guiTop = 2;
    guiLeft = (width-xSize)/2;
    final int x0 = getGuiLeft();
    final int y0 = getGuiTop();
    buttons.clear();
    buttons.add(buttonPreviousPage=addButton(new ImageButton(x0+BUTTON_PAGE_CHANGE_PREV_X,y0+BUTTON_PAGE_CHANGE_Y, 20,12, 220,5, 12, BACKGROUND_IMAGE, (bt)->{
      changePage(-1);
    })));
    buttons.add(buttonNextPage=addButton(new ImageButton(x0+BUTTON_PAGE_CHANGE_NEXT_X,y0+BUTTON_PAGE_CHANGE_Y, 20,12, 220,30, 12, BACKGROUND_IMAGE, (bt)->{
      changePage(1);
    })));
    buttons.add(buttonDeletePage=addButton(new ImageButton(x0+BUTTON_PAGE_DELETE_X,y0+BUTTON_PAGE_DELETE_Y, 12,12, 220,55, 12, BACKGROUND_IMAGE, (bt)->{
      removePage(data.getSelectedPage());
    })));
    buttons.add(buttonClose=addButton(new ImageButton(x0+BUTTON_CLOSE_X,y0+BUTTON_CLOSE_Y, 12,12, 220,80, 12, BACKGROUND_IMAGE, (bt)->{
      saveToServer();
    })));
    rebuildLines();
    minecraft.keyboardListener.enableRepeatEvents(true);
    selectionStart = selectionEnd = Math.max(0, pageCharacterCount()); // Set cursor at the end of the page text when opening.
  }

  @Override
  public void onClose()
  {
    super.onClose();
    minecraft.keyboardListener.enableRepeatEvents(false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void render(MatrixStack mx, int mouseX, int mouseY, float partialTicks)
  {
    if(container.close) { minecraft.player.closeScreen(); return; }
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    tooltip.clear();
    renderBackground(mx);
    buttonPreviousPage.visible = (data.getSelectedPage() > 0) && (data.getPageCount() > 0);
    buttonNextPage.visible = (data.getSelectedPage() < (data.getPageCount()-1)) || (data.getSelectedPage() == (data.getPageCount()-1) && isCurrentProgramNonEmpty());
    buttonDeletePage.visible = (data.getPageCount() > 1) || isCurrentProgramNonEmpty();
    super.render(mx, mouseX, mouseY, partialTicks);
    if(!tooltip.isEmpty()) {
      renderTooltip(mx, new StringTextComponent(String.join("\n", tooltip)), mouseX, mouseY);
    } else {
      renderHoveredTooltip(mx, mouseX, mouseY);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  protected void drawGuiContainerBackgroundLayer(MatrixStack mx, float partialTicks, int mouseX, int mouseY)
  {
    GlStateManager.color4f(1f, 1f, 1f, 1f);
    getMinecraft().getTextureManager().bindTexture(BACKGROUND_IMAGE);
    final int x0=getGuiLeft(), y0=getGuiTop(), w=getXSize(), h=getYSize();
    blit(mx, x0, y0, 0, 0, w, h);
  }

  @Override
  protected void drawGuiContainerForegroundLayer(MatrixStack mx, int mouseX, int mouseY)
  {
    drawProgram(mx, mouseX, mouseY);
    drawPageInfo(mx);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button)
  {
    for(Widget b:buttons) {
      // (Shows the text editor should be now transformed to a widget ...)
      if(b.mouseClicked(mouseX, mouseY, button)) return true;
    }
    hasMouseClicked = true;
    if(isMouseInCodeArea((int)mouseX, (int)mouseY)) {
      final int line = cursorToLine((int)mouseY);
      final int column = cursorToColumn((int)mouseX, (int)mouseY);
      selectionStart = selectionEnd = positionToIndex(line, column);
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button)
  {
    boolean clicked = hasMouseClicked;
    hasMouseClicked = false;
    super.mouseReleased(mouseX, mouseY, button);
    if(clicked && isMouseInCodeArea((int)mouseX, (int)mouseY)) {
      final int line = cursorToLine((int)mouseY);
      final int column = cursorToColumn((int)mouseX, (int)mouseY);
      selectionEnd = positionToIndex(line, column);
      return true;
    }
    return true;
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double mouseX0, double mouseY0)
  {
    boolean clicked = hasMouseClicked;
    if(clicked && isMouseInCodeArea((int)mouseX, (int)mouseY)) {
      final int line = cursorToLine((int)mouseY);
      final int column = cursorToColumn((int)mouseX, (int)mouseY);
      selectionEnd = positionToIndex(line, column);
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, mouseX0, mouseY0);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double direction)
  {
    if(super.mouseScrolled(mouseX, mouseY, direction)) return true;
    if(direction <= -1) {
      changePage(1);
    } else if(direction >= 1) {
      changePage(-1);
    }
    return true;
  }

  @Override
  public boolean charTyped(char typedChar, int keyCode)
  {
    if(super.charTyped(typedChar, keyCode)) return true;
    final int line = indexToLine(getSelectionStart());
    final int column = indexToColumn(getSelectionStart());
    if(!Character.isISOControl(typedChar)) {
      if(!hasControlDown()) {
        undoPush(lines);
      }
      deleteSelection();
      if(lines.get(line).length() < getMaxColumns()) {
        lines.get(line).insert(column, String.valueOf(typedChar));
        selectionStart = selectionEnd = selectionEnd + 1;
      }
      recompile();
      return true;
    }
    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers)
  {
    if(!isEditingCode) return super.keyPressed(keyCode, scanCode, modifiers);
    final int line = indexToLine(getSelectionStart());
    final int column = indexToColumn(getSelectionStart());
    final List<StringBuilder> prev_lines = new ArrayList<>(lines);
    boolean noUndoTracking = false;
    if(hasControlDown()) {
      final String key = GLFW.glfwGetKeyName(keyCode, scanCode);
      if(keyCode==GLFW.GLFW_KEY_A) {
        selectionStart = 0;
        selectionEnd = positionToIndex(Integer.MAX_VALUE, Integer.MAX_VALUE);
      } else if(keyCode==GLFW.GLFW_KEY_C) {
        setClipboardString(selectionToString());
      } else if(keyCode==GLFW.GLFW_KEY_X) {
        Minecraft.getInstance().keyboardListener.setClipboardString(selectionToString());
        deleteSelection();
        recompile();
      } else if(keyCode==GLFW.GLFW_KEY_V) {
        deleteSelection();
        final String[] pastedLines = StringUtil.splitLines(getClipboardString());
        if(!isValidPaste(pastedLines)) {
          return true;
        }
        lines.get(line).insert(indexToColumn(column), pastedLines[0]);
        lines.addAll(line+1, Arrays.stream(pastedLines).
          skip(1).
          map(StringBuilder::new).
          collect(Collectors.toList()));
        selectionStart = selectionEnd = selectionEnd+pastedLines[0].length();
        for(int i = 1; i < pastedLines.length; i++) {
          selectionStart = selectionEnd = selectionEnd+1+pastedLines[i].length();
        }
        recompile();
      } else if("z".equals(key)) {
        noUndoTracking = true;
        undo();
        recompile();
      } else if("y".equals(key)) {
        noUndoTracking = true;
        redo();
        recompile();
      }
    } else if(keyCode == GLFW.GLFW_KEY_ESCAPE) {
      saveToServer();
      return true;
    } else if(keyCode == GLFW.GLFW_KEY_LEFT) {
      if(column > 0 || line > 0) {
        if(hasShiftDown()) {
          selectionEnd = selectionEnd - 1;
        } else {
          selectionStart = selectionEnd = selectionEnd - 1;
        }
      }
    } else if(keyCode == GLFW.GLFW_KEY_RIGHT) {
      if(column < lines.get(line).length() || line < lines.size()) {
        if(hasShiftDown()) {
          selectionEnd = selectionEnd + 1;
        } else {
          selectionStart = selectionEnd = selectionEnd + 1;
        }
      }
    } else if(keyCode == GLFW.GLFW_KEY_UP) {
      final int currLine = indexToLine(selectionEnd);
      if(currLine > 0) {
        final int currColumn = indexToColumn(selectionEnd);
        final int x = columnToX(currLine, currColumn) + 2;
        final int prevLine = currLine - 1;
        final int prevColumn = xToColumn(x, prevLine);
        final int index = positionToIndex(prevLine, prevColumn);
        if(hasShiftDown()) {
          selectionEnd = index;
        } else {
          selectionStart = selectionEnd = index;
        }
      }
    } else if(keyCode == GLFW.GLFW_KEY_PAGE_UP) {
      final int currLine = indexToLine(selectionEnd);
      if(currLine > 0) {
        if(hasShiftDown()) {
          selectionEnd = 0;
        } else {
          selectionStart = selectionEnd = 0;
        }
      }
    } else if(keyCode == GLFW.GLFW_KEY_DOWN) {
      final int currLine = indexToLine(selectionEnd);
      if(currLine < lines.size() - 1) {
        final int currColumn = indexToColumn(selectionEnd);
        final int x = columnToX(currLine, currColumn) + 2;
        final int nextLine = currLine + 1;
        final int nextColumn = xToColumn(x, nextLine);
        final int index = positionToIndex(nextLine, nextColumn);
        if(hasShiftDown()) {
          selectionEnd = index;
        } else {
          selectionStart = selectionEnd = index;
        }
      }
    } else if(keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
      final int currLine = indexToLine(selectionEnd);
      if(currLine < lines.size()-1) {
        final int index = lines.isEmpty() ? 0 : positionToIndex(lines.size()-1, lines.get(lines.size()-1).length());
        if(hasShiftDown()) {
          selectionEnd = index;
        } else {
          selectionStart = selectionEnd = index;
        }
      }
    } else if(keyCode == GLFW.GLFW_KEY_HOME) {
      final int currLine = indexToLine(selectionEnd);
      if(hasShiftDown()) {
        selectionEnd = positionToIndex(currLine, 0);
      } else {
        selectionStart = selectionEnd = positionToIndex(currLine, 0);
      }
    } else if(keyCode == GLFW.GLFW_KEY_END) {
      final int currLine = indexToLine(selectionEnd);
      if(hasShiftDown()) {
        selectionEnd = positionToIndex(currLine, lines.get(currLine).length());
      } else {
        selectionStart = selectionEnd = positionToIndex(currLine, lines.get(currLine).length());
      }
    } else if(keyCode == GLFW.GLFW_KEY_DELETE) {
      if(isEmptyPage(data.getSelectedPage())) {
        removePage(data.getSelectedPage());
      } else if(!deleteSelection()) {
        if(hasShiftDown()) {
          if(lines.size() > 1) {
            lines.remove(line);
          } else {
            lines.get(0).setLength(0);
          }
          selectionStart = selectionEnd = positionToIndex(Math.min(lines.size() - 1, line), 0);
        } else if(column < lines.get(line).length()) {
          lines.get(line).deleteCharAt(column);
        } else if(line < lines.size() - 1) {
          final StringBuilder currLine = lines.get(line);
          final StringBuilder nextLine = lines.get(line + 1);
          if(currLine.length() + nextLine.length() < getMaxColumns()) {
            currLine.append(nextLine);
            lines.remove(line + 1);
          }
        }
      }
      recompile();
    } else if(keyCode == GLFW.GLFW_KEY_BACKSPACE) {
      if(!deleteSelection()) {
        if(column > 0) {
          lines.get(line).deleteCharAt(column - 1);
        } else if(line > 0) {
          final StringBuilder prevLine = lines.get(line - 1);
          final StringBuilder currLine = lines.get(line);
          if(prevLine.length() + currLine.length() < getMaxColumns()) {
            prevLine.append(currLine);
            lines.remove(line);
          }
        }
        selectionStart = selectionEnd = Math.max(0, selectionEnd - 1);
      }
      recompile();
    } else if((keyCode == GLFW.GLFW_KEY_ENTER) || (keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
      deleteSelection();
      if((lines.size() < getMaxLinesPerPage())) {
        final StringBuilder oldLine = lines.get(line);
        final StringBuilder newLine = new StringBuilder();
        if(column < oldLine.length()) {
          newLine.append(oldLine.substring(column));
          oldLine.setLength(column);
        }
        lines.add(line + 1, newLine);
        selectionStart = selectionEnd = selectionEnd + 1;
      }
      recompile();
    } else if(keyCode == GLFW.GLFW_KEY_TAB) {
      deleteSelection();
      if(lines.get(line).length() < getMaxColumns() - 1) {
        lines.get(line).insert(column, "  ");
        selectionStart = selectionEnd = selectionEnd + 2;
      }
      recompile();
    } else {
      noUndoTracking = true;
    }
    if(!noUndoTracking) {
      undoPush(prev_lines);
    }
    return false;
  }

  // -------------------------------------------------------------------------------------------------------------------

  private int drawString(MatrixStack mx, String text, int x, int y, int color)
  { return font.drawString(mx, text, (float)x, (float)y, color); }

  private void drawRect(MatrixStack mx, int x, int y, int w, int h, int color)
  { fill(mx, x,y,w,h,color); }

  void setClipboardString(String text)
  { Minecraft.getInstance().keyboardListener.setClipboardString(text); }

  String getClipboardString()
  { return Minecraft.getInstance().keyboardListener.getClipboardString(); }

  private void saveToServer()
  { saveProgram(); container.save(); }

  private void saveProgram()
  { data.setPage(data.getSelectedPage(), lines.stream().map(StringBuilder::toString).collect(Collectors.toList())); }

  private void rebuildLines()
  {
    if(data.getPageCount() < 1) data.addPage();
    final List<String> program = data.getPage(data.getSelectedPage());
    lines.clear();
    if(!program.isEmpty()) {
      program.forEach(line -> lines.add(new StringBuilder(line)));
      if((program.size() < MAX_LINES_PER_PAGE) && (!program.get(program.size()-1).trim().isEmpty())) {
        lines.add(new StringBuilder());
      }
    }
    recompile();
  }

  private void recompile()
  {
    saveProgram();
    compileError.clear();
    instructionIds.clear();
    final int currentPageNo = data.getSelectedPage();
    final List<List<String>> program = data.getProgram();
    final List<Label> labels = new ArrayList<>();
    for(int pageNumber = 0; (pageNumber < program.size()); pageNumber++) {
      List<String> page = program.get(pageNumber);
      for(int lineNumber = 0; lineNumber < page.size(); lineNumber++) {
        try {
          InstructionUtil.parseLineForLabels(page.get(lineNumber), labels, (short) 0);
        } catch(ParseException ignore) {
        }
      }
    }
    int program_ip = 0;
    for(int pageNumber = 0; (pageNumber < program.size()) && (pageNumber <= currentPageNo); pageNumber++) {
      List<String> page = program.get(pageNumber);
      ArrayList<Integer> pageInstructionIds = new ArrayList<>();
      for(int lineNumber = 0; lineNumber < page.size(); lineNumber++) {
        int ip = -1;
        try {
          if(InstructionUtil.parseLine(page.get(lineNumber), labels, (short) 0) != null) ip = program_ip++;
        } catch (final ParseException e) {
          e.lineNumber = lineNumber;
          e.pageNumber = pageNumber;
          compileError.add(e);
        }
        if(pageNumber == currentPageNo) instructionIds.add(ip);
      }
    }
  }

  private StringBuilder getLine(int index)
  { return (index < lines.size()) ? (lines.get(index)) : (new StringBuilder()); }

  private boolean isCurrentProgramNonEmpty()
  { return (lines.size() > 1) || ((!lines.isEmpty()) && (getLine(0).length() > 0)); }

  private int getSelectionStart()
  { return Math.min(selectionStart, selectionEnd); }

  private int getSelectionEnd()
  { return Math.max(selectionStart, selectionEnd); }

  private boolean intersectsSelection(final int start, final int end)
  { return (start < getSelectionEnd()) && (end > getSelectionStart()); }

  private String selectionToString()
  {
    final int startLine = indexToLine(getSelectionStart());
    final int endLine = indexToLine(getSelectionEnd());
    if(selectionStart == selectionEnd) {
      return getLine(startLine).toString();
    } else {
      final int startColumn = indexToColumn(getSelectionStart());
      final int endColumn = indexToColumn(getSelectionEnd());
      if(startLine == endLine) {
        return getLine(startLine).substring(startColumn, endColumn);
      } else {
        final StringBuilder selection = new StringBuilder();
        selection.append(getLine(startLine).subSequence(startColumn, getLine(startLine).length())).append('\n');
        for(int line = startLine + 1; line < endLine; line++) {
          selection.append(getLine(line).toString()).append('\n');
        }
        selection.append(getLine(endLine).subSequence(0, endColumn)).append('\n');
        return selection.toString();
      }
    }
  }

  private int cursorToLine(final int y)
  { return Math.max(0, Math.min(Math.min(lines.size()-1, getMaxLinesPerPage()), (y-CODE_POS_Y-getGuiTop())/font.FONT_HEIGHT)); }

  private int cursorToColumn(final int x, final int y)
  { return xToColumn(x-getGuiLeft()+2, cursorToLine(y)); }

  private int xToColumn(final int x, final int line)
  { return font.func_238412_a_(getLine(line).toString(), Math.max(0, x-CODE_POS_X)).length(); }

  private int columnToX(final int line, final int column)
  { return CODE_POS_X + font.getStringWidth(getLine(line).substring(0, Math.min(column, getLine(line).length()))); }

  private int positionToIndex(final int line, final int column)
  {
    int index = 0;
    for(int l = 0; l < Math.min(line, lines.size()); l++) index += getLine(l).length() + 1;
    index += Math.min(column, getLine(Math.min(line, lines.size() - 1)).length());
    return index;
  }

  private int indexToLine(final int index)
  {
    int position = 0;
    for(int line = 0; line < lines.size(); line++) {
      position += getLine(line).length() + 1;
      if(position > index) {
        return line;
      }
    }
    return Math.max(0, lines.size()-1);
  }

  private int indexToColumn(final int index)
  {
    if(lines.isEmpty()) return 0;
    int position = 0;
    for(final StringBuilder line : lines) {
      if(position + line.length() + 1 > index) {
        return index - position;
      }
      position += line.length() + 1;
    }
    return getLine(lines.size()-1).length();
  }

  private boolean isMouseInCodeArea(final int mouseX, final int mouseY)
  {
    return (mouseX >= getGuiLeft() + CODE_POS_X - CODE_MARGIN)
        && (mouseX <= getGuiLeft() + CODE_POS_X + CODE_WIDTH + CODE_MARGIN)
        && (mouseY >= getGuiTop()  + CODE_POS_Y - CODE_MARGIN)
        && (mouseY <= getGuiTop()  + CODE_POS_Y + font.FONT_HEIGHT * getMaxLinesPerPage() + CODE_MARGIN);
  }

  private boolean deleteSelection()
  {
    if(selectionStart == selectionEnd) return false;
    final int startLine = indexToLine(getSelectionStart());
    final int endLine = indexToLine(getSelectionEnd());
    final int startColumn = indexToColumn(getSelectionStart());
    final int endColumn = indexToColumn(getSelectionEnd());
    if(startLine == endLine) {
      getLine(startLine).delete(startColumn, endColumn);
    } else {
      getLine(startLine).delete(startColumn, getLine(startLine).length());
      getLine(endLine).delete(0, endColumn);
      getLine(startLine).append(getLine(endLine));
      for(int line = endLine; line > startLine; --line) lines.remove(line);
    }
    selectionStart = selectionEnd = getSelectionStart();
    return true;
  }

  private boolean isValidPaste(final String[] pastedLines)
  {
    final int selectedLine = indexToLine(selectionEnd);
    if(pastedLines.length == 0) return false; // Invalid paste, nothing to paste (this shouldn't even be possible).
    if(pastedLines.length - 1 + lines.size() > getMaxLinesPerPage()) return false; // Invalid paste, too many resulting lines.
    if(pastedLines[0].length() + getLine(selectedLine).length() > getMaxColumns()) return false; // Invalid paste, combined first line and current line too long.
    for(final String pastedLine : pastedLines) {
      if(pastedLine.length() > getMaxColumns()) return false; // Invalid paste, a line is too long.
    }
    return true;
  }

  private boolean isEmptyPage(int pageIndex)
  {
    if((pageIndex < 0) || (pageIndex >= data.getPageCount())) return true;
    for(String s: data.getPage(pageIndex)) {
      if(!s.trim().isEmpty()) return false;
    }
    return true;
  }

  private int pageCharacterCount()
  { return lines.stream().mapToInt(l -> l.toString().length()).sum() + lines.size(); }

  private void changePage(final int delta)
  {
    saveProgram();
    int page_index = MathHelper.clamp(data.getSelectedPage()+delta, 0, data.getPageCount()+1);
    if(page_index >= data.getPageCount()) {
      if((data.getPageCount() < 2) || (!isEmptyPage(data.getPageCount()-1))) data.addPage();
    }
    data.setSelectedPage(page_index);
    rebuildLines();
    selectionStart = selectionEnd = Math.max(0, pageCharacterCount());
    undoBuffer.clear();
    redoBuffer.clear();
  }

  private void removePage(int index)
  {
    data.removePage(index);
    rebuildLines();
    selectionStart = selectionEnd = Math.max(0, pageCharacterCount());
  }

  private void drawProgram(final MatrixStack mx, final int mouseX, final int mouseY)
  {
    int position = 0;
    for(int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
      final StringBuilder line = getLine(lineNumber);
      final int end = position + line.length();
      final int offsetY = lineNumber * font.FONT_HEIGHT;
      final int lineX = CODE_POS_X;
      final int lineY = CODE_POS_Y + offsetY;
      if(selectionStart != selectionEnd && intersectsSelection(position, end)) {
        // Line contains selection, highlight appropriately.
        int currX = lineX;
        // Number of chars before the selection in this line.
        final int prefix = Math.max(0, getSelectionStart() - position);
        // Number of chars selected in this line.
        final int selected = Math.min(line.length() - prefix, getSelectionEnd() - (position + prefix));
        final String prefixText = line.substring(0, prefix);
        drawString(mx, prefixText, currX, lineY, getCodeColor());
        currX += font.getStringWidth(prefixText);
        final String selectedText = line.substring(prefix, prefix + selected);
        final int selectedWidth = font.getStringWidth(selectedText);
        drawRect(mx, currX - 1, lineY - 1, currX + selectedWidth, lineY + font.FONT_HEIGHT - 1, getSelectionBackgroundColor());
        drawString(mx, selectedText, currX, lineY, getSelectionColor());
        currX += selectedWidth;
        final String postfixString = line.substring(prefix + selected);
        drawString(mx, postfixString, currX, lineY, getCodeColor());
      } else {
        // No selection here, just draw the line. Get it? "draw the line"?
        drawString(mx, line.toString(), lineX, lineY, getCodeColor());
      }
      position += line.length() + 1;
      // Instruction hints
      if(lineNumber <= instructionIds.size()) {
        int ip = instructionIds.get(lineNumber);
        if(ip >= 0) {
          drawString(mx, String.format("%03X", (ip & 0x0fff)), CODE_POS_X+CODE_POS_X_IP_HINT_OFFSET, lineY+1, getInstructionNoColor());
        }
      }
    }
    // Part one of error handling, draw red underline, *behind* the blinking cursor.
    if(compileError.size() > 0) {
      for(ParseException exception : compileError) {
        drawError(mx, exception, mouseX, mouseY);
      }
    }
    // Draw selection position in text.
    drawTextCursor(mx);
  }

  private void drawError(MatrixStack mx, ParseException exception, final int mouseX, final int mouseY)
  {
    if(exception.pageNumber != data.getSelectedPage()) return;
    final int localLineNumber = exception.lineNumber;
    final int startX = columnToX(localLineNumber, 0);
    final int rawEndX = columnToX(localLineNumber, getMaxColumns());
    final int startY = CODE_POS_Y + localLineNumber * font.FONT_HEIGHT - 1;
    final int endX = Math.max(rawEndX, startX + (int)font.getStringWidth(" "));
    drawRect(mx,startX - 1, startY + font.FONT_HEIGHT - 1, endX, startY + font.FONT_HEIGHT, 0xFFFF3333);
    if((mouseX >= startX) && (mouseX <= endX) && (mouseY >= startY) && (mouseY <= (startY + font.FONT_HEIGHT))) {
      tooltip.add(exception.message);
    }
  }

  private void drawTextCursor(MatrixStack mx)
  {
    if(System.currentTimeMillis() % 800 <= 400) {
      final int line = indexToLine(selectionEnd);
      final int column = indexToColumn(selectionEnd);
      final StringBuilder sb = (line<lines.size()) ? (getLine(line)) : (new StringBuilder());
      final int x = CODE_POS_X + font.getStringWidth(sb.substring(0, column)) - 1;
      final int y = CODE_POS_Y + line * font.FONT_HEIGHT - 1;
      drawRect(mx,x + 1, y + 1, x + 2 + 1, y + font.FONT_HEIGHT + 1, 0xCC333333);
      drawRect(mx, x, y, x + 2, y + font.FONT_HEIGHT, getSelectionColor());
    }
  }

  private void drawPageInfo(MatrixStack mx)
  {
    String pageInfo = String.format("%d/%d", data.getSelectedPage() + 1, data.getPageCount());
    drawString(mx, pageInfo, PAGE_NUMBER_X - (font.getStringWidth(pageInfo)/2), PAGE_NUMBER_Y, getCodeColor());
  }

  private void undo()
  {
    if(!undoBuffer.isEmpty()) {
      redoBuffer.clear();
      redoBuffer.add(lines.stream().map(s->s.toString()).collect(Collectors.joining("\n")));
      lines.clear();
      lines.addAll( Arrays.stream(StringUtil.splitLines(undoBuffer.pop())).map(s->new StringBuilder(s)).collect(Collectors.toList()));
    }
  }

  private void redo()
  {
    if(!redoBuffer.isEmpty()) {
      lines.clear();
      lines.addAll(Arrays.stream(StringUtil.splitLines(redoBuffer.get(0))).map(s->new StringBuilder(s)).collect(Collectors.toList()));
      redoBuffer.clear();
    }
  }

  private void undoPush(List<StringBuilder> lines)
  {
    undoBuffer.addLast(lines.stream().map(s->s.toString()).collect(Collectors.joining("\n")));
    if(undoBuffer.size() > getMaxUndoSteps()) {
      undoBuffer.removeFirst();
    }
  }

}
