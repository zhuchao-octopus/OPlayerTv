package Myutils;

/**
 * Created by WangChaowei on 2017/12/11.
 */

public class ChangeTool {
    //-------------------------------------------------------
    // 判断奇数或偶数，位运算，最后一位是1则为奇数，为0是偶数
    public static int isOdd(int num) {
        return num & 1;
    }

    //-------------------------------------------------------
    //Hex字符串转int
    public static int HexToInt(String inHex) {
        return Integer.parseInt(inHex, 16);
    }

    //-------------------------------------------------------
    //Hex字符串转byte
    public static byte HexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }


    public static  byte[] intToBytes(int in) {
        byte[] b = new byte[4];
        b[3] = (byte) (in & 0xff);
        b[2] = (byte) (in >> 8 & 0xff);
        b[1] = (byte) (in >> 16 & 0xff);
        b[0] = (byte) (in >> 24 & 0xff);
        return b;
    }

    //-------------------------------------------------------
    //1字节转2个Hex字符
    public static String Byte2Hex(Byte inByte) {
        return String.format("%02x", new Object[]{inByte}).toUpperCase();
    }

    public static byte BytesAdd(byte[] inBytArr,int count)
    {
        byte aa=0;

        for(int i =0;i<count;i++)
            aa = (byte) (aa + Byte.valueOf(inBytArr[i]));

        return aa;
    }

    //-------------------------------------------------------
    //字节数组转转hex字符串
    public static String ByteArrToHex(byte[] inBytArr) {
        StringBuilder strBuilder = new StringBuilder();
        for (byte valueOf : inBytArr) {
            strBuilder.append(Byte2Hex(Byte.valueOf(valueOf)));
            strBuilder.append(" ");
        }
        return strBuilder.toString();
    }
    public static String ByteArrToHex(byte[] inBytArr,int count) {
        StringBuilder strBuilder = new StringBuilder();
        int i=0;
        for (byte valueOf : inBytArr) {
            i++;
            strBuilder.append(Byte2Hex(Byte.valueOf(valueOf)));
            strBuilder.append(" ");
            if(i>=count) break;
        }
        return strBuilder.toString();
    }
    public static String ByteArrToHex2(byte[] inBytArr,int count) {
        StringBuilder strBuilder = new StringBuilder();
        int i=0;
        for (byte valueOf : inBytArr) {
            i++;
            strBuilder.append(Byte2Hex(Byte.valueOf(valueOf)));
            //strBuilder.append(" ");
            if(i>=count) break;
        }
        return strBuilder.toString();
    }
    //-------------------------------------------------------
    //字节数组转转hex字符串，可选长度
    public static String ByteArrToHexStr(byte[] inBytArr, int offset, int byteCount) {
        StringBuilder strBuilder = new StringBuilder();
        int j = byteCount;
        for (int i = offset; i < j; i++) {
            strBuilder.append(Byte2Hex(Byte.valueOf(inBytArr[i])));
        }
        return strBuilder.toString();
    }

    //-------------------------------------------------------
    //把hex字符串转字节数组
    public static byte[] HexToByteArr(String inHex) {
        byte[] result;
        int hexlen = inHex.length();
        if (isOdd(hexlen) == 1) {
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = HexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }
}
