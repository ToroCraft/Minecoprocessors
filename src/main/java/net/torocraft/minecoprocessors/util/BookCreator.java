package net.torocraft.minecoprocessors.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.torocraft.minecoprocessors.ModMinecoprocessors;


public class BookCreator
{
  private static final String PATH = "/data/minecoprocessors/books/";
  private static final String PAGE_DELIMITER = "~~~";
  private static ItemStack manualBook;

  public static ItemStack getManual()
  {
    try {
      manualBook = loadBook("manual");
    } catch(Exception e) {
      manualBook = new ItemStack(Items.WRITABLE_BOOK);
      ModMinecoprocessors.logger().error("Failed to load manual: " + e.toString());
    }
    return manualBook;
  }

  private static ItemStack loadBook(String name) throws IOException
  {
    ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
    String line;
    int lineNumber = 1;
    StringBuilder page = newPage();
    try (BufferedReader reader = openBookReader(name)) {
      while ((line = reader.readLine()) != null) {
        if (lineNumber == 1) {
          book.setTagInfo("title", StringNBT.valueOf(line));
        } else if (lineNumber == 2) {
          book.setTagInfo("author", StringNBT.valueOf(line));
        } else if (PAGE_DELIMITER.equals(line)) {
          writePage(book, page);
          page = newPage();
        } else {
          page.append(line).append("\n");
        }
        lineNumber++;
      }
    }
    writePage(book, page);
    return book;
  }

  private static BufferedReader openBookReader(String name) throws FileNotFoundException
  {
    String path = PATH + name + ".txt";
    InputStream is = BookCreator.class.getResourceAsStream(path);
    if(is == null) throw new FileNotFoundException("Book file not found [" + path + "]");
    return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
  }

  private static void writePage(ItemStack book, StringBuilder page)
  {
    ListNBT pages = getPagesNbt(book);
    pages.add(createPage(page.toString()));
    book.setTagInfo("pages", pages);
  }

  private static ListNBT getPagesNbt(ItemStack book)
  {
    if(book.getTag() == null) book.setTag(new CompoundNBT());
    return book.getTag().getList("pages", 8);
  }

  private static StringBuilder newPage()
  { return new StringBuilder(256); }

  private static StringNBT createPage(String page)
  { return StringNBT.valueOf(ITextComponent.Serializer.toJson(new StringTextComponent(page))); }

}
