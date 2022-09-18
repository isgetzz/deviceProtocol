package com.cc.control.protocol;

import java.nio.ByteBuffer;

public class CRC16 {

    /// <summary>
    ///通用CRC16校验和
    /// </summary>
    /// <param name="Quantic">多项式0xA001</param>
    /// <param name="intData">初始值0xFFFF</param>

    public static short GeneralCRCFun(ByteBuffer ptr, int length) {
        int CRC = 0xffff;
        int POLYNOMIAL = 0xa001;
        int i, j;
        for (i = 0; i < length; i++) {
            CRC ^= ((int) ptr.get(i) & 0x000000ff);
            for (j = 0; j < 8; j++) {
                if ((CRC & 1) != 0) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }
        return shortTransposition(CRC);
    }


    public static short shortTransposition(int value) {
        return (short) ((value << 8 | value >> 8) & 0xffff);

    }

    public static String stringTransposition(int value) {
        return String.format("%04X", (value << 8 | value >> 8) & 0xffff);
    }

    public static long GetModBusCRC(ByteBuffer v, int len) {
        long functionReturnValue = 0;
        long i = 0;

        long J = 0;
        byte[] d = null;

        long CRC = 0;
        CRC = 0xffffL;
        for (i = 0; i <= len - 1; i++) {
            CRC = (CRC / 256) * 256L + (CRC % 256L) ^ v.get((int) i);
            for (J = 0; J <= 7; J++) {
                long d0 = 0;
                d0 = CRC & 1L;
                CRC = CRC / 2;
                if (d0 == 1)
                    CRC = CRC ^ 0xa001L;
            }
        }
        CRC = CRC % 65536;
        functionReturnValue = CRC;
        return functionReturnValue;
    }


    public static long GetModBusCRC(String DATA) {
        long functionReturnValue = 0;
        long i = 0;

        long J = 0;
        int[] v = null;
        byte[] d = null;
        v = strToToHexByte(DATA);

        long CRC = 0;
        CRC = 0xffffL;
        for (i = 0; i <= (v).length - 1; i++) {
            CRC = (CRC / 256) * 256L + (CRC % 256L) ^ v[(int) i];
            for (J = 0; J <= 7; J++) {
                long d0 = 0;
                d0 = CRC & 1L;
                CRC = CRC / 2;
                if (d0 == 1)
                    CRC = CRC ^ 0xa001L;
            }
        }
        CRC = CRC % 65536;
        functionReturnValue = CRC;
        return functionReturnValue;
    }

    private static int[] strToToHexByte(String hexString) {
        hexString = hexString.replace(" ", "");

        if ((hexString.length() % 2) != 0) {
            hexString += " ";
        }

        int[] returnBytes = new int[hexString.length() / 2];

        for (int i = 0; i < returnBytes.length; i++)
            returnBytes[i] = (0xff & Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16));
        return returnBytes;
    }

}   