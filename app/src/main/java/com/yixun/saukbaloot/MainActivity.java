package com.yixun.saukbaloot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.multidex.MultiDex;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.unity3d.player.UnityPlayerActivity;
import com.yixun.message.Message;
import com.yixun.pay.BillingClientLifecycle;
import com.yixun.tools.FileUtil;
import com.yixun.tools.IResultBack;
import com.yixun.tools.PermissionUtil;
import com.yixun.tools.SocialUtil;
import com.yixun.tools.ZipStream;

import org.json.JSONArray;
import org.json.JSONException;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Objects;

public class MainActivity extends UnityPlayerActivity {
    private BillingClientLifecycle billingClientLifecycle;
    public static MainActivity instance;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        //InitSoftInput();
        FileUtil.Init(this);
        SocialUtil.getInstance().InitFB(this);
        billingClientLifecycle = BillingClientLifecycle.getInstance(getApplication());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SocialUtil.getInstance().setActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        billingClientLifecycle.dispose();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionUtil.REQUEST_CODE == requestCode) {
            String[] arr = PermissionUtil.getDeniedPermissions(this, permissions);
            boolean accept = null == arr || 0 == arr.length;
            if(accept){
                PermCallBack.onResult(true);
            } else{
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static IResultBack<Boolean> PermCallBack = null;
    public void CheckPermissions(String[] permissions, IResultBack<Boolean> callback){
        PermCallBack = callback;
        if (PermissionUtil.checkPermissions(this, permissions)){
            PermCallBack.onResult(true);
        }
    }

    private static int realKeyboardHeight;
    //??????????????????
    public int GetKeyboardHeight() {
        return realKeyboardHeight;
    }
    //???????????????
    public void InitSoftInput() {
        final Context context = getApplicationContext();
        final View myLayout = getWindow().getDecorView();
        myLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                myLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = myLayout.getRootView().getHeight();
                int heightDiff = screenHeight - r.bottom - r.top;
                if (heightDiff > 100) {
                    int statusBarHeight = 0;
                    try {
                        @SuppressLint("PrivateApi") Class<?> c = Class.forName("com.android.internal.R$dimen");
                        Object obj = c.newInstance();
                        Field field = c.getField("status_bar_height");
                        int x = Integer.parseInt(Objects.requireNonNull(field.get(obj)).toString());
                        statusBarHeight = getResources().getDimensionPixelSize(x);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    realKeyboardHeight = heightDiff - statusBarHeight;
                    myLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    //??????apk??????????????????
    public String GetApkData() {
        String path = getApplicationContext().getPackageResourcePath();
        //UnityLog("path===" + path);
        String mComment = "";
        try {
            File file = new File(path);
            RandomAccessFile apkFile = new RandomAccessFile(file, "r");
            ZipStream zipStream = new ZipStream(apkFile);
            mComment = zipStream.getComment();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mComment;
    }

    //??????App
    public void InstallApp(String apkPath) {
        File apkFile = new File(apkPath);
        if (apkFile.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", apkFile);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            startActivity(intent);
        } else {
            Message.UnityLog("File not exit:" + apkPath);
        }
    }

    //??????webView??????
    public void OpenWebView(String url) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }

    //??????photo??????
    public void OpenPhotoView(int msgCode, boolean needCrop, int cropX, int cropY) {
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putExtra("msgCode", msgCode);
        intent.putExtra("needCrop", needCrop);
        intent.putExtra("cropX", cropX);
        intent.putExtra("cropY", cropY);
        startActivity(intent);
    }

    //?????????????????????
    public void ConnectBilling(String skuArrJson){
        try {
            JSONArray jsonArray = new JSONArray(skuArrJson);
            String[] skuIds = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                skuIds[i]=jsonArray.getString(i);
            }
            billingClientLifecycle.updateSkus(skuIds);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        billingClientLifecycle.startConnection();
    }
    //??????sku??????
    public void QuerySkuDetailsAsync(String skuArrJson) {
        try {
            JSONArray jsonArray = new JSONArray(skuArrJson);
            String[] skuIds=new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                skuIds[i]=jsonArray.getString(i);
            }
            billingClientLifecycle.updateSkus(skuIds);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        billingClientLifecycle.querySkuDetails();
    }
    //????????????
    public void LaunchPurchaseFlow(String skuId) {
       billingClientLifecycle.tryLaunchBilling(MainActivity.this,skuId);
    }
    //????????????
    public void ConsumePurchase(String skuId){
        billingClientLifecycle.consumePurchase(skuId);
    }
}
