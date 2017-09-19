package net.torocraft.minecoprocessors.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class BookCreator {

  private static final String PATH = "/assets/minecoprocessors/books/";
  private static final String PAGE_DELIMITER = "~~~";

  public static ItemStack createBook(String name) {
    ItemStack book = null;
    try {
      book = loadBook(name);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (book == null) {
      book = new ItemStack(Items.BOOK);
    }
    return book;
  }

  private static ItemStack loadBook(String name) throws IOException {
    ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
    String line;
    int lineNumber = 1;
    StringBuilder page = newPage();
    BufferedReader reader = openBookReader(name);
    while ((line = reader.readLine()) != null) {
      if (lineNumber == 1) {
        book.setTagInfo("title", new NBTTagString(line));
      } else if (lineNumber == 2) {
        book.setTagInfo("author", new NBTTagString(line));
      } else if (PAGE_DELIMITER.equals(line)) {
        writePage(book, page);
        page = newPage();
      } else {
        page.append(line).append("\n");
      }
      lineNumber++;
    }
    writePage(book, page);
    return book;
  }

  private static BufferedReader openBookReader(String name) throws UnsupportedEncodingException {
    String path = PATH + name + ".txt";
    InputStream is = BookCreator.class.getResourceAsStream(path);
    if (is == null) {
      throw new IllegalArgumentException("Book file not found [" + path + "]");
    }
    return new BufferedReader(new InputStreamReader(is, "UTF-8"));
  }

  private static void writePage(ItemStack book, StringBuilder page) {
    NBTTagList pages = getPagesNbt(book);
    pages.appendTag(createPage(page.toString()));
    if (pages.tagCount() >= 50) {
      throw new IndexOutOfBoundsException("out of book pages");
    }
    book.setTagInfo("pages", pages);
  }

  private static NBTTagList getPagesNbt(ItemStack book) {
    if (book.getTagCompound() == null) {
      book.setTagCompound(new NBTTagCompound());
    }
    return book.getTagCompound().getTagList("pages", 8);
  }

  private static StringBuilder newPage() {
    return new StringBuilder(256);
  }

  private static NBTTagString createPage(String page) {
    return new NBTTagString(ITextComponent.Serializer.componentToJson(new TextComponentString(page)));
  }

}
