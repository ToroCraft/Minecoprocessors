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

	public static boolean getBitInLong(long l, int position) {
		return ((l >> position) & 1) != 0;
	}

	public static long setBitInLong(long l, boolean bit, int position) {
		if (bit) {
			return l | (1 << position);
		}
		return l & ~(1 << position);
	}

	public static byte getByte(int i, int position) {
		return (byte) ((i >> (8 * position)) & 0xff);
	}

	public static int setByte(int i, byte b, int position) {
		if (position > 3) {
			throw new IndexOutOfBoundsException("byte position of " + position);
		}
		int mask = ~(0xff << position * 8);
		i = (mask & i);
		int insert = (int) (b << position * 8) & ~mask;
		return i | insert;
	}

	public static byte getByteInLong(long l, int position) {
		return (byte)((l >> (8 * position)) & 0xff);
	}

	public static long setByteInLong(long l, byte b, int position) {
		if (position > 7) {
			throw new IndexOutOfBoundsException("position of " + position);
		}
		long mask = ~(Long.parseLong("ff", 16) << position * 8);
		l = (mask & l);
		long insert = ((long) b << position * 8) & ~mask;
		return l | insert;
	}

	public static short getShort(long i, int position) {
		return (short) ((i >> (16 * position)) & 0xffff);
	}

	public static long setShort(long i, short b, int position) {
		if (position > 3) {
			throw new IndexOutOfBoundsException("short position of " + position);
		}
		long mask = ~(Long.parseLong("ffff", 16) << (position * 16));
		i = (mask & i);
		long insert = (long) ((long) b << position * 16) & ~mask;
		return i | insert;
	}

	public static void test() {
		testBitMethods();
		testBitInLongMethods();
		testByteMethods();
		testByteInLongMethods();
		testShortMethods();
	}

	private static void testShortMethods() {
		long l = Long.parseLong("0123456789abcdef", 16);

		assert getShort(l, 0) == (short) 0xcdef;
		assert getShort(l, 1) == (short) 0x89ab;
		assert getShort(l, 2) == (short) 0x4567;
		assert getShort(l, 3) == (short) 0x0123;

		l = setShort(l, (short) 0x0ffff, 1);
		assert Long.toString(l, 16).equals("1234567ffffcdef");

		l = setShort(l, (short) 0x01111, 3);
		assert Long.toString(l, 16).equals("11114567ffffcdef");

		l = setShort(l, (short) 0x0aaaa, 2);
		assert Long.toString(l, 16).equals("1111aaaaffffcdef");

		l = setShort(l, (short) 0x09999, 0);
		assert Long.toString(l, 16).equals("1111aaaaffff9999");
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

	private static void testByteInLongMethods() {
		long l = Long.parseLong("0123456789abcdef", 16);

		assert getByteInLong(l, 0) == (byte) 0xef;
		assert getByteInLong(l, 1) == (byte) 0xcd;
		assert getByteInLong(l, 2) == (byte) 0xab;
		assert getByteInLong(l, 3) == (byte) 0x89;
		assert getByteInLong(l, 7) == (byte) 0x01;

		l = setByteInLong(l, (byte) 0x33, 1);
		assert Long.toString(l, 16).equals("123456789ab33ef");

		l = setByteInLong(l, (byte) 0xee, 4);
		assert Long.toString(l, 16).equals("12345ee89ab33ef");

		l = setByteInLong(l, (byte) 0xff, 7);
		assert Long.toUnsignedString(l, 16).equals("ff2345ee89ab33ef");
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

	private static void testBitInLongMethods() {
		long l = Long.parseLong("0110010", 2);

		assert !getBitInLong(l, 0);
		assert getBitInLong(l, 1);
		assert !getBitInLong(l, 2);
		assert !getBitInLong(l, 3);
		assert getBitInLong(l, 4);
		assert getBitInLong(l, 5);
		assert !getBitInLong(l, 6);
		assert !getBitInLong(l, 7);

		l = setBitInLong(l, true, 0);
		assert Long.toBinaryString(l).equals("110011");

		l = setBitInLong(l, true, 2);
		assert Long.toBinaryString(l).equals("110111");

		l = setBitInLong(l, false, 0);
		l = setBitInLong(l, false, 2);
		assert Long.toBinaryString(l).equals("110010");
	}

}
