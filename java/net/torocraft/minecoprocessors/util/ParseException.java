package net.torocraft.minecoprocessors.util;

public class ParseException extends Exception {
	public ParseException(String line, String message) {
		super(line + " :: " + message);
	}

	public ParseException(String line, String message, Throwable cause) {
		super(line + " :: " + message, cause);
	}
}
