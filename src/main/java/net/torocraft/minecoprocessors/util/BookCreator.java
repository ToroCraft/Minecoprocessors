package net.torocraft.minecoprocessors.util;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import javax.annotation.Nullable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class BookCreator {

  private static final String PATH = "/assets/minecoprocessors/books/";

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

  // TODO don't treat double breaks a new pages
  // TODO add a page break delimiter
  // TODO line breaks should be included in the rendered book

  @Nullable
  private static ItemStack loadBook(String name) throws IOException {
    ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
    String line;
    int lineNumber = 1;
    StringBuilder page = newPage();
    BufferedReader reader = openBookReader(name);
    while ((line = reader.readLine()) != null) {
      page = processLine(lineNumber, line, page, book);
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

  private static StringBuilder processLine(int lineNumber, String line, StringBuilder page, ItemStack book) {
    line = line.trim();

    if (lineNumber < 4 && line.length() == 0) {
      return page;
    }

    if (lineNumber == 1) {
      book.setTagInfo("title", new NBTTagString(line));
    } else if (lineNumber == 2) {
      book.setTagInfo("author", new NBTTagString(line));
    } else {
      page = processContentLine(line, page, book);
    }

    return page;
  }

  private static StringBuilder processContentLine(String line, StringBuilder page, ItemStack book) {
    if (line.length() == 0) {
      //writePage(book, page);
      //page = newPage();
    }

    //TODO line limit on page, are these limits really needed?

    String[] words = line.split("\\s+");

    for (String word : words) {

      if (page.length() + word.length() > 255) {
        writePage(book, page);
        page = newPage();

      } else if (page.length() > 0) {
        page.append(" ");
      }

      page.append(word);
    }

    return page;
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
    NBTTagList pages = book.getTagCompound().getTagList("pages", 8);
    if (pages == null) {
      pages = new NBTTagList();
    }
    return pages;
  }

  private static StringBuilder newPage() {
    return new StringBuilder(256);
  }

  private static NBTTagString createPage(String page) {
    return new NBTTagString(ITextComponent.Serializer.componentToJson(new TextComponentString(page)));
  }

}
