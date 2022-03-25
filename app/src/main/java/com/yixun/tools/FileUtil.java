package com.yixun.tools;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Hashtable;

public class FileUtil
{
    private static AssetManager sAssetManager;
    private static final Hashtable<Object, Object> mFileTable = new Hashtable<>();

    public static void Init(Activity activity)
    {
        sAssetManager = activity.getAssets();
    }

    public static boolean isFileExists(String paramString)
    {
        boolean exist = false;
        if (mFileTable.containsKey(paramString)) {
            return (boolean) mFileTable.get(paramString);
        }
        if (sAssetManager != null)
        {
            InputStream inputStream;
            try
            {
                inputStream = sAssetManager.open(paramString);
                mFileTable.put(paramString, Boolean.TRUE);
                inputStream.close();
                exist = true;
            }
            catch (Exception exception)
            {
                mFileTable.put(paramString, Boolean.FALSE);
                Log.e("ReadFile exception", exception.toString());
            }
        }
        return exist;
    }

    public static byte[] getBytes(String paramString)
    {
        byte[] arrayOfByte = null;
        if (sAssetManager != null)
        {
            InputStream inputStream;
            try
            {
                inputStream = sAssetManager.open(paramString);
                int i = inputStream.available();
                arrayOfByte = new byte[i];
                inputStream.read(arrayOfByte);
                inputStream.close();
                if (!mFileTable.containsKey(paramString)) {
                    mFileTable.put(paramString, Boolean.TRUE);
                }
            }
            catch (Exception exception)
            {
                if (!mFileTable.containsKey(paramString)) {
                    mFileTable.put(paramString, Boolean.FALSE);
                }
                Log.e("ReadFile exception", exception.toString());
            }
        }
        return arrayOfByte;
    }

    public static String getString(String paramString)
    {
        byte[] arrayOfByte = getBytes(paramString);
        if (arrayOfByte != null) {
            return new String(arrayOfByte);
        }
        return "";
    }

    public static void copyFileFromAssets(String assetsPath, String savePath)
    {
        try
        {
            Log.i("tag", "copyFile() " + assetsPath);
            Log.i("tag", "copyFile() " + savePath);

            InputStream is = sAssetManager.open(assetsPath);
            FileOutputStream fos = new FileOutputStream(new File(savePath));
            byte[] buffer = new byte['Ѐ'];
            int byteCount = 0;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void copyFilesFromAssets(String assetsPath, String savePath)
    {
        try
        {
            String[] fileNames = sAssetManager.list(assetsPath);
            if (fileNames.length > 0)
            {
                File file = new File(savePath);
                file.mkdirs();
                String[] arrayOfString;
                int i = (arrayOfString = fileNames).length;
                for (byte b = 0; b < i;)
                {
                    String fileName = arrayOfString[b];
                    copyFilesFromAssets(String.valueOf(assetsPath) + "/" + fileName,
                            String.valueOf(savePath) + "/" + fileName);
                    b = (byte)(b + 1);
                }
            }
            else
            {
                InputStream is = sAssetManager.open(assetsPath);
                FileOutputStream fos = new FileOutputStream(new File(savePath));
                byte[] buffer = new byte['Ѐ'];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Uri getUriFromFile(Context context, File file){
        Uri fileUri;
        if(Build.VERSION.SDK_INT>=24){
            fileUri= FileProvider.getUriForFile(context, context.getPackageName()+".fileprovider",file);
        }
        else {
            fileUri=Uri.fromFile(file);
        }
        return fileUri;
    }

}
