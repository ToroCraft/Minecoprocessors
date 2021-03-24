/*
 * @file CodeBookItem.java
 * @license GPL
 */
package net.torocraft.minecoprocessors.items;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.util.InputMappings;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;
import net.torocraft.minecoprocessors.util.StringUtil;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;


public final class CodeBookItem extends WrittenBookItem
{
  public CodeBookItem(Item.Properties properties)
  { super(properties); }

  // --------------------------------------------------------------------------------------------------------------
  // Item
  // --------------------------------------------------------------------------------------------------------------

  @Override
  @OnlyIn(Dist.CLIENT)
  public boolean hasEffect(ItemStack stack)
  { return false; }

  @Override
  public boolean isEnchantable(ItemStack stack)
  { return false; }

  @Override
  public boolean isBookEnchantable(ItemStack stack, ItemStack book)
  { return false; }

  @Override
  public boolean showDurabilityBar(ItemStack stack)
  { return false; }

  @Override
  public boolean canPlayerBreakBlockWhileHolding(BlockState state, World worldIn, BlockPos pos, PlayerEntity player)
  { return false; }

  @Override
  public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment)
  { return false; }

  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag)
  {
    Data data = Data.loadFromStack(stack);
    if(data.getProgram().isEmpty()) {
      // Empty book tooltip
      tooltip.add(new TranslationTextComponent("item.minecoprocessors.code_book.tooltip.noprogram"));
      tooltip.add(new TranslationTextComponent("item.minecoprocessors.code_book.tooltip"));
    } else if(
      // CTRL+SHIFT -> show first lines
      (InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
       InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT))
      &&
      (InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) ||
       InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL))
    ) {
      String txt = data.getPage(0).stream().limit(10).filter(s->!s.isEmpty()).collect(Collectors.joining("\n")).trim();
      if(!txt.isEmpty()) tooltip.add(new StringTextComponent(txt));
    } else {
      // Name and page count.
      String program_name = data.getProgramName();
      if(!program_name.isEmpty()) {
        tooltip.add(new TranslationTextComponent("item.minecoprocessors.code_book.tooltip.program", new Object[]{program_name}));
      }
      tooltip.add(new TranslationTextComponent("item.minecoprocessors.code_book.tooltip.numpages", new Object[]{String.format("%d",data.getPageCount())}));
      tooltip.add(new TranslationTextComponent("item.minecoprocessors.code_book.tooltip"));
    }
  }

  @Override
  public ActionResultType onItemUse(ItemUseContext context)
  { return ActionResultType.PASS; } // prevent Lectern usage.

  @Override
  public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
  {
    ItemStack stack = player.getHeldItem(hand);
    if(!world.isRemote()) {
      NetworkHooks.openGui((ServerPlayerEntity) player, new INamedContainerProvider() {
        @Override
        public ITextComponent getDisplayName()
        { return new TranslationTextComponent(getRegistryName().toString()); }

        @Override
        public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player)
        { return new CodeBookContainer(id, inventory, player); }
      });
    }
    return new ActionResult<>(ActionResultType.SUCCESS, stack);
  }

  // --------------------------------------------------------------------------------------------------------------
  // Specific
  // --------------------------------------------------------------------------------------------------------------

  // Data (Wrapper for list of pages stored in the code book.)
  public static class Data
  {
    private static final String TAG_PAGES = "pages";
    private static final String TAG_SELECTED = "selected";
    private final List<List<String>> pages = new ArrayList<>();
    private int selectedPage = 0;

    // --------------------------------------------------------------------- //

    public String getProgramName()
    {
      for(List<String> page:pages) {
        for(String line:page) {
          String name = line.trim();
          if(name.isEmpty()) continue;
          return (name.charAt(0) != ';') ? ("") : (name.substring(1).trim());
        }
      }
      return "";
    }

    /**
     * Get the page currently selected in the book.
     *
     * @return the index of the selected page.
     */
    public int getSelectedPage()
    { return selectedPage; }

    /**
     * Set which page is currently selected.
     *
     * @param index the new selected index.
     */
    public void setSelectedPage(final int index)
    {
      this.selectedPage = index;
      validateSelectedPage();
    }

    /**
     * Get the number of pages stored in the book.
     *
     * @return the number of pages stored in the book.
     */
    public int getPageCount()
    { return pages.size(); }

    /**
     * Get the code on the specified page.
     *
     * @param index the index of the page to get the code of.
     * @return the code on the page.
     */
    public List<String> getPage(final int index)
    { return Collections.unmodifiableList(pages.get(index)); }

    /**
     * Add a new, blank page to the book.
     */
    public void addPage()
    {
      pages.add(Collections.singletonList(""));
      setSelectedPage(pages.size() - 1);
    }

    /**
     * Overwrite a page at the specified index.
     *
     * @param page the index of the page to overwrite.
     * @param code the code of the page.
     */
    public void setPage(final int page, final List<String> code)
    { pages.set(page, new ArrayList<>(code)); }

    /**
     * Remove a page from the book.
     *
     * @param index the index of the page to remove.
     */
    public void removePage(final int index)
    {
      pages.remove(index);
      validateSelectedPage();
    }

    /**
     * Get the complete program of the selected page, taking into account the <code>#BWTM</code> preprocessor macro allowing programs to span multiple pages.
     *
     * @return the full program starting on the current page.
     */
    public List<List<String>> getProgram()
    { return Collections.unmodifiableList(pages); }

    public List<String> getContinuousProgram()
    {
      final List<String> program = new ArrayList<>();
      for(int i = 0; i < pages.size(); i++) program.addAll(getPage(i));
      return program;
    }

    /**
     * Load data from the specified NBT tag.
     *
     * @param nbt the tag to load the data from.
     */
    public void readNBT(final CompoundNBT nbt)
    {
      pages.clear();
      final ListNBT pagesNbt = nbt.getList(TAG_PAGES, net.minecraftforge.common.util.Constants.NBT.TAG_STRING);
      for(int index = 0; index < pagesNbt.size(); index++) {
        pages.add(Arrays.asList(StringUtil.splitLines(pagesNbt.getString(index))));
      }
      selectedPage = nbt.getInt(TAG_SELECTED);
      validateSelectedPage();
    }

    /**
     * Store the data to the specified NBT tag.
     *
     * @param nbt the tag to save the data to.
     */
    public CompoundNBT writeNBT(final CompoundNBT nbt)
    {
      final ListNBT pagesNbt = new ListNBT();
      int removed = 0;
      for(int index = 0; index < pages.size(); index++) {
        final List<String> program = pages.get(index);
        if((program.size() > 1) || (program.get(0).length() > 0)) {
          pagesNbt.add(StringNBT.valueOf(String.join("\n", program)));
        } else if(index < selectedPage) {
          removed++;
        }
      }
      nbt.put(TAG_PAGES, pagesNbt);
      nbt.putInt(TAG_SELECTED, selectedPage-removed);
      return nbt;
    }

    // --------------------------------------------------------------------- //

    private void validateSelectedPage()
    { selectedPage = Math.max(0, Math.min(pages.size() - 1, selectedPage)); }

    private boolean areAllPagesEqual(final List<List<String>> newPages, final int startPage)
    {
      for(int offset = 0; offset < newPages.size(); offset++) {
        final List<String> have = pages.get(startPage + offset);
        final List<String> want = newPages.get(offset);
        if(!Objects.equals(have, want)) return false;
      }
      return true;
    }

    // --------------------------------------------------------------------- //

    /**
     * Load code book data from the specified NBT tag.
     *
     * @param nbt the tag to load the data from.
     * @return the data loaded from the tag.
     */
    public static Data loadFromNBT(@Nullable final CompoundNBT nbt)
    {
      final Data data = new Data();
      if(nbt != null) data.readNBT(nbt);
      return data;
    }

    /**
     * Load code book data from the specified item stack.
     *
     * @param stack the item stack to load the data from.
     * @return the data loaded from the stack.
     */
    public static Data loadFromStack(final ItemStack stack)
    { return loadFromNBT(stack.getTag()); }

    /**
     * Save the specified code book data to the specified item stack.
     *
     * @param stack the item stack to save the data to.
     * @param data the data to save to the item stack.
     */
    public static void saveToStack(final ItemStack stack, final Data data)
    {
      CompoundNBT nbt = stack.getTag();
      if(nbt == null) stack.setTag(nbt = new CompoundNBT());
      data.writeNBT(nbt);
    }
  }
}
