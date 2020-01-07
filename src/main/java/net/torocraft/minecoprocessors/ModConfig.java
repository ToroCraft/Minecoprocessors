/*
 * @file ModConfig.java
 * @license GPL
 *
 * Main class for module settings. Handles reading and
 * saving the config file.
 */
package net.torocraft.minecoprocessors;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;


public class ModConfig
{
  private static final String MODID = ModMinecoprocessors.MODID;
  public static final CommonConfig COMMON;
  public static final ServerConfig SERVER;
  public static final ClientConfig CLIENT;
  public static final ForgeConfigSpec COMMON_CONFIG_SPEC;
  public static final ForgeConfigSpec SERVER_CONFIG_SPEC;
  public static final ForgeConfigSpec CLIENT_CONFIG_SPEC;

  static {
    final Pair<CommonConfig, ForgeConfigSpec> common_ = (new ForgeConfigSpec.Builder()).configure(CommonConfig::new);
    COMMON_CONFIG_SPEC = common_.getRight();
    COMMON = common_.getLeft();
    final Pair<ServerConfig, ForgeConfigSpec> server_ = (new ForgeConfigSpec.Builder()).configure(ServerConfig::new);
    SERVER_CONFIG_SPEC = server_.getRight();
    SERVER = server_.getLeft();
    final Pair<ClientConfig, ForgeConfigSpec> client_ = (new ForgeConfigSpec.Builder()).configure(ClientConfig::new);
    CLIENT_CONFIG_SPEC = client_.getRight();
    CLIENT = client_.getLeft();
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static void onLoad(final net.minecraftforge.fml.config.ModConfig config)
  {
    try {
      apply();
    } catch(Exception ex) {
      ModMinecoprocessors.logger().error("Failed to apply config file data {}", config.getFileName());
    }
  }

  public static void onFileChange(final net.minecraftforge.fml.config.ModConfig config)
  {}

  //--------------------------------------------------------------------------------------------------------------------

  public static class ClientConfig
  {
    public final ForgeConfigSpec.IntValue code_book_max_columns_per_line;

    ClientConfig(ForgeConfigSpec.Builder builder)
    {
      builder.comment("Settings not loaded on servers.")
        .push("client");
      {
        code_book_max_columns_per_line = builder
          .translation(MODID + ".config.code_book_max_columns_per_line")
          .comment("Defines the maximum line length in the Code Book.")
          .defineInRange("code_book_max_columns_per_line", 18, 10, 80);
      }
      builder.pop();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static class ServerConfig
  {
    ServerConfig(ForgeConfigSpec.Builder builder)
    {
      builder.comment("Settings not loaded on clients.")
        .push("server");
      builder.pop();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static class CommonConfig
  {
    CommonConfig(ForgeConfigSpec.Builder builder)
    {
      builder.comment("Settings affecting the logical server side, but are also configurable in single player.")
        .push("server");
      // Config definitions go here
      builder.pop();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Cache fields
  //--------------------------------------------------------------------------------------------------------------------

  public static int maxColumnsPerLine = 20;
  public static int maxLinesPerPage = 20;
  public static int codeBookTextColor = 0xFF333333;
  public static int codeBookSelectedTextColor = 0xFFEEEEEE;
  public static int codeBookSelectedBackgroundColor = 0xCC333399;
  public static int codeBookInstructionNoColor = 0xffaaaaaa;

  public static final void apply()
  {
    maxColumnsPerLine = CLIENT.code_book_max_columns_per_line.get();
  }
}
