package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static Myutils.ChangeTool.ByteArrToHex;

public class SerialPortDevice {

    private final String TAG = "SerialPortDevice";
    private String mDevicePath = "/dev/ttyS3";
    private int mBaudrate = 9600;
    private boolean serialPortStatus = false; //是否打开串口标志
    //private String data_;
    private boolean threadStatus; //线程状态，为了安全终止线程

    private SerialPort serialPort = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;

    //add by cvte start >>>
    private final ConcurrentLinkedQueue<Byte> mDataQueue = new ConcurrentLinkedQueue<>();
    private final static int SLEEP_TIMEOUT = 30;
    private final static int STATE_PRODUCT_CODE = 1;
    private final static int STATE_MSG_ID = 2;
    private final static int STATE_ANSWER = 3;
    private final static int STATE_LENGTH = 4;
    private final static int STATE_DATA = 5;
    private final static int STATE_CHECKSUM = 6;
    private final static int STATE_END = 7;

    private int mState = STATE_PRODUCT_CODE;
    private boolean mNeedParserFlag = false;
    private boolean isReady = false;

    public SerialPortDevice(String DevicePath, int Baudrate,boolean NeedParserFlag) {
        this.mDevicePath = DevicePath;
        this.mBaudrate = Baudrate;
        this.mNeedParserFlag = NeedParserFlag;
    }

    public void setDisableParseThread(boolean mDisableParseThread) {
        this.mNeedParserFlag = mDisableParseThread;
    }

    public boolean openPort() {
        isReady =false;
        try {
            serialPort = new SerialPort(new File(mDevicePath), mBaudrate, 0);
            this.serialPortStatus = true;
            threadStatus = false; //线程状态
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            if (mNeedParserFlag)
                new ReceiverParserThread().start(); //add by cvte
            new ReadThread().start();
        } catch (IOException e) {
            Log.e(TAG, "openSerialPort: 打开串口异常：" + e.toString());
            return false;
        }
        Log.e(TAG, "openSerialPort: 成功打开串口!!!!!!!!! " + mDevicePath);
        isReady = true;
        return true;
    }

    public boolean isReady() {
        return isReady;
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        try {
            inputStream.close();
            outputStream.close();

            this.serialPortStatus = false;
            this.threadStatus = true; //线程状态
            serialPort.close();
        } catch (IOException e) {
            //Log.e(TAG, "closeSerialPort: 关闭串口异常：" + e.toString());
            return;
        }
        //Log.d(TAG, "closeSerialPort: 关闭串口成功");
    }

    /**
     * 发送串口指令（字符串）
     *
     * @param data String数据指令
     */
    public void sendSerialPort(String data) {
        Log.d(TAG, "sendSerialPort: 发送数据");

        try {
            byte[] sendData = data.getBytes(); //string转byte[]
            //this.data_ = new String(sendData); //byte[]转string
            if (sendData.length > 0) {
                outputStream.write(sendData);
                outputStream.write('\n');
                //outputStream.write('\r'+'\n');
                outputStream.flush();
                Log.d(TAG, "sendSerialPort: 串口数据发送成功");
            }
        } catch (IOException e) {
            //Log.e(TAG, "sendSerialPort: 串口数据发送失败：" + e.toString());
        }

    }

    /**
     * 发送串口指令（字节数组）
     */
    public void sendBuffer(byte[] data) {
        try
        {
            byte[] sendData = data;  //string转byte[]
            //this.data_ = new String(sendData); //byte[]转string
            //if (bytesToInt(Count,0) > 0) {
            outputStream.write(sendData);
            //outputStream.write('\n');
            //outputStream.write('\r'+'\n');
            outputStream.flush();
            Log.d(TAG, "发送数据到"+mDevicePath+":"+ByteArrToHex(data,data.length));
        }
        catch (IOException e)
        {
            Log.e(TAG, "sendSerialPort: 串口数据发送失败：" + e.toString());
        }
    }


    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序。
     *
     * @param ary    byte数组
     * @param offset 从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] ary, int offset) {
        int value;
        value = (int) ((ary[offset] & 0xFF)
                | ((ary[offset + 1] << 8) & 0xFF00)
                | ((ary[offset + 2] << 16) & 0xFF0000)
                | ((ary[offset + 3] << 24) & 0xFF000000));

        return value;
    }


    /**
     * 单开一线程，来读数据
     */

    private class ReadThread extends Thread {
        byte[] buffer = new byte[128];

        @Override
        public void run() {
            super.run();
            //判断进程是否在运行，更安全的结束进程
            while (!threadStatus) {
                try {
                    int size = inputStream.read(buffer);
                    if (size > 0) {
                        //Log.d(TAG, "ReadThread:接收数据|" + size + "：" + ByteArrToHex(buffer, size));
                        if (!mNeedParserFlag) {
                            if(onDataReceiveListener != null)
                              onDataReceiveListener.onDataReceive(buffer, size);
                        }
                        else {
                            for (int i = 0; i < size; i++) {
                                mDataQueue.add(buffer[i]);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "数据读取异常：" + e.toString());
                }
            }

        }
    }

    //add by cvte
    private class ReceiverParserThread extends Thread {
        @Override
        public void run() {
            super.run();
            byte mCurrentByte;
            int len = 0;
            List<Byte> byteArrayList = new ArrayList<>();
            while (true) {
                switch (mState) {
                    case STATE_PRODUCT_CODE: //0x01, 0x01
                        if (mDataQueue.size() >= 2) {
                            byteArrayList.clear();
                            len = 0;
                            mCurrentByte = mDataQueue.remove();
                            if (mCurrentByte == 0x01) {
                                byteArrayList.add(mCurrentByte);
                                Byte aByte = mDataQueue.peek();
                                if (aByte != null) {
                                    if (aByte == 0x01) {
                                        mState++;
                                        mDataQueue.remove();
                                        byteArrayList.add(aByte);
                                    } else {
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        } else {
                            try {
                                sleep(SLEEP_TIMEOUT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case STATE_MSG_ID:
                        if (mDataQueue.size() >= 2) {
                            //1st byte
                            mCurrentByte = mDataQueue.remove();
                            byteArrayList.add(mCurrentByte);
                            //2nd byte
                            mCurrentByte = mDataQueue.remove();
                            byteArrayList.add(mCurrentByte);
                            mState++;
                        } else {
                            try {
                                sleep(SLEEP_TIMEOUT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case STATE_ANSWER:
                        if (mDataQueue.size() >= 1) {
                            mCurrentByte = mDataQueue.remove();
                            byteArrayList.add(mCurrentByte);
                            mState++;
                        } else {
                            try {
                                sleep(SLEEP_TIMEOUT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case STATE_LENGTH:
                        if (mDataQueue.size() >= 2) {
                            //1st byte
                            mCurrentByte = mDataQueue.remove();
                            byteArrayList.add(mCurrentByte);
                            len |= mCurrentByte;
                            //2nd byte
                            mCurrentByte = mDataQueue.remove();
                            byteArrayList.add(mCurrentByte);
                            len |= (mCurrentByte << 8);
                            mState++;
                        } else {
                            try {
                                sleep(SLEEP_TIMEOUT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case STATE_DATA:
                        if (mDataQueue.size() >= len) {
                            for (int k = 0; k < len; k++) {
                                mCurrentByte = mDataQueue.remove();
                                byteArrayList.add(mCurrentByte);
                            }
                            mState++;
                        } else {
                            try {
                                sleep(SLEEP_TIMEOUT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case STATE_CHECKSUM:
                        if (mDataQueue.size() >= 1) {
                            int sum = 0;
                            for (Byte b : byteArrayList) {
                                sum += (b & 0xFF);
                            }
                            sum &= 0xFF;
                            mCurrentByte = mDataQueue.remove();
                            if (sum == (mCurrentByte & 0xFF)) {
                                byteArrayList.add(mCurrentByte);
                                mState++;
                            } else {
                                len = 0;
                                byteArrayList.clear();
                                mState = STATE_PRODUCT_CODE;
                                break;
                            }
                        } else {
                            try {
                                sleep(SLEEP_TIMEOUT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    case STATE_END:
                        if (mDataQueue.size() >= 1) {
                            mCurrentByte = mDataQueue.remove();
                            if ((mCurrentByte & 0xFF) == 0x7E) {
                                byteArrayList.add(mCurrentByte);
                                int cmd_len = byteArrayList.size();
                                byte[] cmd_pkt = new byte[cmd_len];
                                for (int i = 0; i < cmd_len; i++) {
                                    cmd_pkt[i] = byteArrayList.get(i);
                                }
                                // TODO: 2019/6/19 process data here.
                                Log.d(TAG, "Dispatch : " + ByteArrToHex(cmd_pkt));
                                onDataReceiveListener.onDataReceive(cmd_pkt, cmd_len);
                            } else {
                                len = 0;
                                byteArrayList.clear();
                            }
                            mState = STATE_PRODUCT_CODE;
                        } else {
                            try {
                                sleep(SLEEP_TIMEOUT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                }
            }
        }
    }

    public OnDataReceiveListener onDataReceiveListener = null;

    public static interface OnDataReceiveListener {
        public void onDataReceive(byte[] buffer, int size);
    }

    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }

}
