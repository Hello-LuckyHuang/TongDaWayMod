package com.hxzhitang.tongdaway.tools;

import java.math.BigInteger;

public class TDWRandom {
    public static long getSeedByXZ(int x, int z) {
        return convertToLong(x + "" + z);
    }

    public static long getSeedByXZ(long oldSeed, int x, int z) {
        return convertToLong(oldSeed + "" + x + z);
    }

    private static long convertToLong(String str) {
        char targetChar = '-';
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == targetChar) {
                count++;
            }
        }
        str = str.replace("-", "");
        if (count % 2 == 1) {
            str = "-" + str;
        }
        BigInteger bigInt = new BigInteger(str);
        return bigInt.longValue();
    }
}
