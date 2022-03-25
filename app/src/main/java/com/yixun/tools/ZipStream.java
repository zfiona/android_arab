package com.yixun.tools;

import java.io.RandomAccessFile;

/**
 * Read Apk comment
 */
public class ZipStream {

    private RandomAccessFile mZipFile;
    private static final long LOCSIG = 0x4034b50;
    private static final int ENDHDR = 22;
    
    private static long MAGIC = 0x06054b50;
    private static long MAGIC_64 = 0x07064b50;
    /**
     * ZipStream construct
     *
     * @param file
     */
    public ZipStream(RandomAccessFile file) {
        mZipFile = file;
    }

    /**
     * Get zip comment
     *
     * @return String comment
     * @throws Exception
     */
    public String getComment() throws Exception
    {
    	long ENDS = MAGIC_64;
        long scanOffset = mZipFile.length() - ENDHDR;
        if (scanOffset < 0) {
            throw new Exception("File too short to be a zip file: " + mZipFile.length());
        }

        mZipFile.seek(0);
        final int headerMagic = Integer.reverseBytes(mZipFile.readInt());
        if (headerMagic == ENDS) {
            throw new Exception("Empty zip archive not supported");
        }
        if (headerMagic != LOCSIG) {
            throw new Exception("Not a zip archive");
        }

        long stopOffset = scanOffset - 65536;
        if (stopOffset < 0) {
            stopOffset = 0;
        }

        while (true) {
            mZipFile.seek(scanOffset);
            if (Integer.reverseBytes(mZipFile.readInt()) == ENDS) {
                break;
            }

            scanOffset--;
            if (scanOffset < stopOffset) {
                throw new Exception("End Of Central Directory signature not found");
            }
        }

        mZipFile.seek(mZipFile.getFilePointer() + ENDHDR - 6);
        byte[] commentLengthByte = new byte[2];
        mZipFile.read(commentLengthByte);
        int commentLength = byte2int(commentLengthByte);

        if (commentLength > 0) {
            byte[] commentBytes = new byte[commentLength];
            mZipFile.read(commentBytes);
            String comment = new String(commentBytes);

            return comment.trim();
        }

        return "";
    }

    /**
     * byte to int
     *
     * @param bytes byte[]
     * @return int
     */
    private static int byte2int(byte[] bytes) {
        return (bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
    }
}