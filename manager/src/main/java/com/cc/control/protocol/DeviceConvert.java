package com.cc.control.protocol;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DeviceConvert {

    /**
     * String求ASCII和
     */
    public static int stringToASCIISum(String str) {
        int sum = 0;
        byte[] bs = str.getBytes();
        for (byte mByte : bs) {
            sum += mByte;
        }
        return sum;
    }

    /**
     * @param src byte
     * @return 校验和四个字节16进制
     */
    public static String bytesToHexSum(byte[] src) {
        long checksum = 0;
        for (byte b : src) {
            checksum += b & 0xff;
        }
        StringBuilder stringBuilder = new StringBuilder();
        String str = Long.toHexString(checksum);
        for (int i = 0; i < (8 - str.length()); i++) {
            stringBuilder.append("0");
        }
        return stringBuilder + str;
    }

    /**
     * mBytes求和返回16进制
     */
    public static String byteSum(String mBytes) {
        int sum = 0;
        for (byte mByte : toByteArray(mBytes)) {
            sum += mByte & 0xff;
        }
        return intToHexString(sum & 0xff);
    }

    /**
     * @param num 数据
     * @param bit 0位开始   11100100 低位到高位 0代表 第0位
     */
    public static int getBit(int num, int bit) {
        return ((num >> bit) & 1);

    }

    public static byte[] toByteArray(String hexString) {
        if (hexString.isEmpty()) {
            return new byte[0];
        } else {
            hexString = hexString.toLowerCase();
            byte[] byteArray = new byte[hexString.length() >> 1];
            int index = 0;

            for (int i = 0; i < hexString.length(); ++i) {
                if (index > hexString.length() - 1) {
                    return byteArray;
                }

                byte highDit = (byte) (Character.digit(hexString.charAt(index), 16) & 255);
                byte lowDit = (byte) (Character.digit(hexString.charAt(index + 1), 16) & 255);
                byteArray[i] = (byte) (highDit << 4 | lowDit);
                index += 2;
            }

            return byteArray;
        }
    }

    public static byte[] stringToBytes(String text) {
        int len = text.length();
        byte[] bytes = new byte[(len + 1) / 2];
        for (int i = 0; i < len; i += 2) {
            int size = Math.min(2, len - i);
            String sub = text.substring(i, i + size);
            bytes[i / 2] = (byte) Integer.parseInt(sub, 16);
        }
        return bytes;
    }

    /**
     * 协议规定 一个字节用两个ascii码表示
     */
    public static byte[] asciiStringToByteArray(String string) {
        byte[] bytes = null;
        try {
            bytes = string.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return bytes;
    }

    /**
     * 固件版本号跟名称,因为部分设备会已0结束所以需要判断
     *
     * @param bArray b
     * @return
     */
    public static String bytesToAsciiString(byte[] bArray) {
        StringBuilder strBuilder = new StringBuilder();
        for (byte b : bArray) {
            if (b != 0)
                strBuilder.append((char) b);
        }
        return strBuilder.toString();
    }

    public static String bytesToHexString(byte[] bArray) {
        StringBuilder sb = new StringBuilder(bArray.length);
        for (byte b : bArray) {
            String sTemp = Integer.toHexString(255 & b);
            if (sTemp.length() < 2) {
                sb.append(0);
            }
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * @param bArray 目标数组
     * @param size   指定长度
     * @return 16进制 Sting
     */
    public static String bytesToHexString(byte[] bArray, int size) {
        StringBuilder strBuf = new StringBuilder(bArray.length);
        String strTemp;
        for (int i = 0; i < size; i++) {
            strTemp = Integer.toHexString(0xFF & bArray[i]);
            if (strTemp.length() < 2) {
                strBuf.append(0);
            }
            strBuf.append(strTemp.toUpperCase());
        }
        return strBuf.toString();
    }

    /**
     * float转化4个byte
     *
     * @param f
     * @return
     */
    public static byte[] floatToBytes(Float f) {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putFloat(f);
        return buf.array();
    }


    /**
     * byte数组数值反传
     *
     * @param data
     * @return
     */
    public static byte[] dataValueRollback(byte[] data) {
        ArrayList<Byte> al = new ArrayList<Byte>();
        for (int i = data.length - 1; i >= 0; i--) {
            al.add(data[i]);
        }

        byte[] buffer = new byte[al.size()];
        for (int i = 0; i <= buffer.length - 1; i++) {
            buffer[i] = al.get(i);
        }
        return buffer;
    }

    public static int byteXor(int num) {
        return (num & 0xFF) ^ (num >> 8 & 0xFF) ^ (num >> 16 & 0xFF) ^ (num >> 24 & 0xFF);
    }

    /**
     * intToHexString
     *
     * @param i
     * @return
     */
    public static String intToHexString(int i) {
        String hex = Integer.toHexString(i & 0xFF);
        if (hex.length() == 1)
            hex = '0' + hex;
        return hex;
    }

    /**
     * @param arg 参数
     * @return StringBuilder 因为无需考虑多线程,性能更加
     */
    public static String intArrToHexString(int... arg) {
        StringBuilder buffer = new StringBuilder();
        for (int data : arg) {
            String hex = Integer.toHexString(data & 0xFF);
            if (hex.length() == 1)
                hex = '0' + hex;
            buffer.append(hex);
        }
        return buffer.toString();
    }

    public static byte[] intToByteShort(int i) {//大小端为两个字节16进制  00 01
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (i & 0xff);
        bytes[1] = (byte) ((i >>> 8) & 0xff);
        return bytes;
    }

    /**
     * 大小端为两个字节16进制 0010 1000
     *
     * @param i 数据
     * @return 返回前两位
     */
    public static String intTo2HexString(int i) {
        return intToHexString(i) + intToHexStringH(i);
    }

    /**
     * 大小端转换
     *
     * @param data 转换值
     * @return 4个 16进制转换
     */
    public static String intTo4HexString(int data) {
        return intToHexString(data) +
                intToHexString((data >> 8 & 0xFF)) +
                intToHexString((data >> 16 & 0xFF)) +
                intToHexString((data >> 24 & 0xFF));
    }

    public static String intToHexStringH(int i) {
        String hex = Integer.toHexString(i >> 8);
        if (hex.length() == 1)
            hex = '0' + hex;
        return hex;
    }

    /**
     * 前两位
     *
     * @param i 1
     * @return
     */
    public static String intToHexString16(int i) {
        String hex = Short.toString((short) (i >> 16 & 0xFF));
        if (hex.length() == 1)
            hex = '0' + hex;
        return hex;
    }

    /**
     * 前四位
     *
     * @param i
     * @return
     */
    public static String intToHexString24(int i) {
        String hex = Short.toString((short) (i >> 24 & 0xFF));
        if (hex.length() == 1)
            hex = '0' + hex;
        return hex;
    }

    /**
     * 把单个字节转换成二进制字符串
     */
    public static String byteToBin(byte b) {
        String zero = "00000000";
        String binStr = Integer.toBinaryString(b & 0xFF); //八位
        if (binStr.length() < 8) {
            binStr = zero.substring(0, 8 - binStr.length()) + binStr;
        }
        return binStr;
    }

}
