package net.torocraft.minecoprocessors.util;

public class ParseException extends Exception {

  private static final long serialVersionUID = 1493829582935568613L;

  public ParseException(String line, String message) {
    super(genMessage(line, message));
  }

  public ParseException(String line, String message, Throwable cause) {
    super(genMessage(line, message), cause);
  }

  private static String genMessage(String line, String message) {
    //return "{" + line + "} " + message;
    return line;
  }
}
