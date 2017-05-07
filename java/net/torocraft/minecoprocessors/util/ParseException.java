package net.torocraft.minecoprocessors.util;

public class ParseException extends Exception {
	private static final long serialVersionUID = 1493829582935568613L;

	public ParseException(String line, String message) {
		super(line + " :: " + message);
	}

	public ParseException(String line, String message, Throwable cause) {
		super(line + " :: " + message, cause);
	}
}
