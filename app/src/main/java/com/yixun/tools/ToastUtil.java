package com.yixun.tools;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;
 

public class ToastUtil {

    private static Toast mToast;

    public static void makeTextLong(Context mContext, String text) {
        if (mToast != null) {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_LONG);
        } else {
            mToast = Toast.makeText(mContext, text, Toast.LENGTH_LONG);
        }
        mToast.show();
    }

    public static void makeTextShort(Context mContext, String text){
        if (mToast != null) {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        } else {
            mToast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
        }
        mToast.show();
    }
}