package net.torocraft.minecoprocessors.util;

public class ByteUtil {

	public static boolean getBit(byte b, int position) {
		return ((b >> position) & 1) != 0;
	}

	public static byte setBit(byte b, boolean bit, int position) {
		if (bit) {
			return (byte) (b | (1 << position));
		}
		return (byte) (b & ~(1 << position));
	}

	public static byte getByte(int i, int position) {
		return (byte) ((i >> (8 * position)) & 0xff);
	}

	public static int setByte(int i, byte b, int position) {
		if (position > 3) {
			throw new IndexOutOfBoundsException("int byte position of " + position);
		}
		int mask = ~(0xff << position * 8);
		i = (mask & i);
		int insert = (int) (b << position * 8) & ~mask;
		return i | insert;
	}

	public static void test() {
		testBitMethods();
		testByteMethods();
	}

	private static void testByteMethods() {
		int i = 0x12345678;

		assert getByte(i, 0) == 0x78;
		assert getByte(i, 1) == 0x56;
		assert getByte(i, 2) == 0x34;
		assert getByte(i, 3) == 0x12;

		i = setByte(i, (byte) 0xee, 1);
		assert i == 0x1234ee78;

		i = setByte(i, (byte) 0xff, 3);
		assert i == 0xff34ee78;

		i = setByte(i, (byte) 0xcc, 2);
		assert i == 0xffccee78;

		i = setByte(i, (byte) 0xaa, 0);
		assert i == 0xffcceeaa;
	}

	private static void testBitMethods() {
		byte b = (byte) 0xff;
		b = setBit(b, false, 0);
		assert b == (byte) 0xfe;
		assert getBit(b, 7);
		assert getBit(b, 1);
		assert !getBit(b, 0);

		b = setBit(b, false, 7);
		assert b == (byte) 0x7e;
		assert !getBit(b, 7);

		b = setBit(b, true, 7);
		b = setBit(b, true, 0);
		assert b == (byte) 0xff;
	}

	private static String s(byte b) {
		return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
	}

	private static void check(boolean assertion) {
		if (!assertion) {
			throw new AssertionError();
		}
	}
}
