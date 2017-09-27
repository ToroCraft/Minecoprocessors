package net.torocraft.minecoprocessors.util;

public class ParseException extends Exception {

  private static final long serialVersionUID = 1493829582935568613L;

  public String line;
  public String message;
  public int lineNumber;
  public int pageNumber;

  public ParseException(String line, String message) {
    super(genMessage(line, message));
    this.line = line;
    this.message = message;
  }

  public ParseException(String line, String message, Throwable cause) {
    super(genMessage(line, message), cause);
    this.line = line;
    this.message = message;
  }

  private static String genMessage(String line, String message) {
    return line;
  }

}
