package net.torocraft.minecoprocessors.blocks;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.gui.MinecoprocessorGuiHandler;
import net.torocraft.minecoprocessors.items.IMetaBlockName;
import net.torocraft.minecoprocessors.items.ItemBlockMeta;
import net.torocraft.minecoprocessors.util.RedstoneUtil;

@Mod.EventBusSubscriber
public class BlockMinecoprocessor extends BlockRedstoneDiode implements ITileEntityProvider, IMetaBlockName {

  protected static final AxisAlignedBB AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.1875D, 1.0D);

  public static final PropertyBool ACTIVE = PropertyBool.create("active");
  public static final PropertyBool OVERCLOCKED = PropertyBool.create("overclocked");

  public static final String NAME = "minecoprocessor";

  private static ResourceLocation REGISTRY_NAME = new ResourceLocation(Minecoprocessors.MODID, NAME);
  private static ResourceLocation REGISTRY_OVERCLOCKED_NAME = new ResourceLocation(Minecoprocessors.MODID, NAME + "_overclocked");

  public static BlockMinecoprocessor INSTANCE = (BlockMinecoprocessor) new BlockMinecoprocessor()
      .setUnlocalizedName(NAME)
      .setRegistryName(REGISTRY_NAME);

  public static ItemBlockMeta ITEM_INSTANCE = (ItemBlockMeta) new ItemBlockMeta(INSTANCE).
      setRegistryName(REGISTRY_NAME);

  @SubscribeEvent
  public static void initBlock(final RegistryEvent.Register<Block> event) {
    event.getRegistry().register(INSTANCE);
  }

  @SubscribeEvent
  public static void initItem(final RegistryEvent.Register<Item> event) {
    event.getRegistry().register(ITEM_INSTANCE);
  }

  public static void preRegisterRenders() {
    ModelBakery.registerItemVariants(ITEM_INSTANCE, REGISTRY_NAME, REGISTRY_OVERCLOCKED_NAME);
  }

  public static void registerRenders() {
    registerRender(REGISTRY_NAME.toString(), 0);
    registerRender(REGISTRY_OVERCLOCKED_NAME.toString(), 4);
  }

  private static void registerRender(String file, int meta) {
    ModelResourceLocation model = new ModelResourceLocation(file, "inventory");
    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(ITEM_INSTANCE, meta, model);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return getMetaFromState(state) & 4;
  }

  @Override
  public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
    items.add(new ItemStack(ITEM_INSTANCE));
    items.add(new ItemStack(ITEM_INSTANCE, 1, 4));
  }

  @Override
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
      EntityLivingBase placer) {
    return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(OVERCLOCKED, Boolean.valueOf((meta & 4) > 0))
        .withProperty(ACTIVE, Boolean.valueOf(false));
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, new IProperty[]{FACING, ACTIVE, OVERCLOCKED});
  }

  @Override
  protected IBlockState getPoweredState(IBlockState unpoweredState) {
    return getUnpoweredState(unpoweredState);
  }

  @Override
  protected IBlockState getUnpoweredState(IBlockState poweredState) {
    Boolean obool = poweredState.getValue(ACTIVE);
    EnumFacing enumfacing = poweredState.getValue(FACING);
    return INSTANCE.getDefaultState().withProperty(FACING, enumfacing).withProperty(ACTIVE, obool);
  }

  /**
   * Convert the given metadata into a BlockState for this Block
   */
  @Override
  public IBlockState getStateFromMeta(int meta) {
    IBlockState state = getDefaultState()
        .withProperty(FACING, EnumFacing.getHorizontal(meta)).withProperty(ACTIVE, Boolean.valueOf((meta & 8) > 0))
        .withProperty(OVERCLOCKED, Boolean.valueOf((meta & 4) > 0));
    return state;
  }

  /**
   * Convert the BlockState into the correct metadata value
   */
  @Override
  public int getMetaFromState(IBlockState state) {
    int i = state.getValue(FACING).getHorizontalIndex();

    if (state.getValue(ACTIVE)) {
      i |= 8;
    }

    if (state.getValue(OVERCLOCKED)) {
      i |= 4;
    }
    return i;
  }

  @Override
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return AABB;
  }

  public BlockMinecoprocessor() {
    super(true);
    this.setDefaultState(
        this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(OVERCLOCKED, Boolean.valueOf(false))
            .withProperty(ACTIVE, Boolean.valueOf(false)));
    setCreativeTab(CreativeTabs.REDSTONE);
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityMinecoprocessor();
  }

  @Override
  public boolean canProvidePower(IBlockState state) {
    return true;
  }

  @Override
  protected void updateState(World worldIn, BlockPos pos, IBlockState state) {
    worldIn.updateBlockTick(pos, this, 0, -1);
  }

  public void onPortChange(World worldIn, BlockPos pos, IBlockState state, int portIndex) {
    notifyNeighborsOnSide(worldIn, pos, RedstoneUtil.convertPortIndexToFacing(state.getValue(FACING).getOpposite(), portIndex));
  }

  protected void notifyNeighborsOnSide(World worldIn, BlockPos pos, EnumFacing side) {
    BlockPos neighborPos = pos.offset(side);
    if (net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(worldIn, pos, worldIn.getBlockState(pos), java.util.EnumSet.of(side), false)
        .isCanceled()) {
      return;
    }
    worldIn.neighborChanged(neighborPos, this, pos);
    worldIn.notifyNeighborsOfStateExcept(neighborPos, this, side.getOpposite());
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    super.updateTick(world, pos, state, rand);
    updateInputPorts(world, pos, state);

    if (world.isRemote) {
      return;
    }

    TileEntityMinecoprocessor te = (TileEntityMinecoprocessor) world.getTileEntity(pos);

    boolean changed = false;

    boolean blockActive = state.getValue(ACTIVE);
    boolean processorActive = !te.getProcessor().isWait() && !te.getProcessor().isFault();

    if (blockActive && !processorActive) {
      state = state.withProperty(ACTIVE, Boolean.valueOf(false));
      changed = true;
    } else if (!blockActive && processorActive) {
      state = state.withProperty(ACTIVE, Boolean.valueOf(true));
      changed = true;
    }

    if (changed) {
      world.setBlockState(pos, state, 2);
    }
  }

  public static void updateInputPorts(World world, BlockPos pos, IBlockState state) {
    if (world.isRemote) {
      return;
    }
    EnumFacing facing = state.getValue(FACING).getOpposite();

    int e = calculateInputStrength(world, pos.offset(EnumFacing.EAST), EnumFacing.EAST);
    int w = calculateInputStrength(world, pos.offset(EnumFacing.WEST), EnumFacing.WEST);
    int n = calculateInputStrength(world, pos.offset(EnumFacing.NORTH), EnumFacing.NORTH);
    int s = calculateInputStrength(world, pos.offset(EnumFacing.SOUTH), EnumFacing.SOUTH);

    int[] values = new int[4];

    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.NORTH)] = n;
    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.SOUTH)] = s;
    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.WEST)] = w;
    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.EAST)] = e;

    ((TileEntityMinecoprocessor) world.getTileEntity(pos)).updateInputPorts(values);
  }

  protected static int calculateInputStrength(World worldIn, BlockPos pos, EnumFacing enumfacing) {
    IBlockState adjacentState = worldIn.getBlockState(pos);
    Block block = adjacentState.getBlock();

    int i = worldIn.getRedstonePower(pos, enumfacing);

    if (i >= 15) {
      return 15;
    }

    int redstoneWirePower = 0;

    if (block == Blocks.REDSTONE_WIRE) {
      redstoneWirePower = adjacentState.getValue(BlockRedstoneWire.POWER);
    }

    return Math.max(i, redstoneWirePower);

  }

  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    if (stack.hasDisplayName()) {
      TileEntity tileentity = worldIn.getTileEntity(pos);

      if (tileentity instanceof TileEntityMinecoprocessor) {
        ((TileEntityMinecoprocessor) tileentity).setName(stack.getDisplayName());
      }
    }
  }

  @Override
  public int getStrongPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
    return state.getWeakPower(blockAccess, pos, side);
  }

  @Override
  public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
    TileEntityMinecoprocessor te = ((TileEntityMinecoprocessor) blockAccess.getTileEntity(pos));

    if (te.getWorld().isRemote) {
      return 0;
    }

    if (RedstoneUtil.isFrontPort(state, side)) {
      return RedstoneUtil.portToPower(te.getFrontPortSignal());
    }

    if (RedstoneUtil.isBackPort(state, side)) {
      return RedstoneUtil.portToPower(te.getBackPortSignal());
    }

    if (RedstoneUtil.isLeftPort(state, side)) {
      return RedstoneUtil.portToPower(te.getLeftPortSignal());
    }

    if (RedstoneUtil.isRightPort(state, side)) {
      return RedstoneUtil.portToPower(te.getRightPortSignal());
    }

    return 0;
  }

  @Override
  public IBlockState withRotation(IBlockState state, Rotation rot) {
    return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
  }

  @Override
  public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
    return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
      float hitY, float hitZ) {

    if (!world.isRemote) {
      player.openGui(Minecoprocessors.INSTANCE, MinecoprocessorGuiHandler.MINECOPROCESSOR_ENTITY_GUI, world, pos.getX(), pos.getY(),
          pos.getZ());
    }

    return true;
  }

  @Override
  protected int getDelay(IBlockState state) {
    return 0;
  }

  @Override
  public boolean isLocked(IBlockAccess worldIn, BlockPos pos, IBlockState state) {
    return true;
  }

  @Override
  protected boolean isAlternateInput(IBlockState state) {
    return true;
  }

  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    notifyNeighbors(worldIn, pos, state);

    TileEntity tileentity = worldIn.getTileEntity(pos);
    if (tileentity instanceof TileEntityMinecoprocessor) {
      InventoryHelper.dropInventoryItems(worldIn, pos, (TileEntityMinecoprocessor) tileentity);
    }
    super.breakBlock(worldIn, pos, state);
  }

  @Override
  public String getSpecialName(ItemStack stack) {
    return getUnlocalizedName() + ((stack.getItemDamage() & 4) == 0 ? "" : "_overclocked");
  }
}
