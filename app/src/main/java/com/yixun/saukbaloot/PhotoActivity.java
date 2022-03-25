package com.yixun.saukbaloot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.yixun.message.Message;
import com.yixun.tools.FileUtil;

class MsgDefine{
    public static final int SHOW_SELECT_WINDOW = 1;
    public static final int CHOOSE_PICTURE = 2;
    public static final int TAKE_PICTURE = 3;
}

public class PhotoActivity extends Activity {

    private static final String TAG = "PhotoTools";

    private static final int TAKE_PICTURE = 0;
    private static final int CHOOSE_PICTURE = 1;
    private static final int CROP = 2;
    private static final int CROP_PICTURE = 3;
    private final static String FILE_NAME = "image.png";

    private int msgCode;
    private boolean needCrop;
    private int _cropX = 200;
    private int _cropY = 200;

    private Uri imageUri;
    private String filePath;
    private String cachePath;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent data = getIntent();
        msgCode = data.getIntExtra("msgCode", 1);
        needCrop = data.getBooleanExtra("needCrop", false);
        _cropX = data.getIntExtra("cropX", 200);
        _cropY = data.getIntExtra("cropY", 200);

        InitPath();
        if (!checkPermissionAllGranted(permissions)) {
            requestPermissions(permissions, PERMISSION_CODE);
        } else {
            checkOptCode();
        }
    }
    private void InitPath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cachePath = this.getExternalCacheDir() + "/";
            filePath = this.getExternalFilesDir(null) + "/";
        } else {
            cachePath = this.getCacheDir().getPath() + "/";
            filePath = this.getFilesDir().getPath() + "/";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length <= 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        } else {
            checkOptCode();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            switch (requestCode) {
                case TAKE_PICTURE:
                    uri = imageUri;
                    checkCrop(uri);
                    break;
                case CHOOSE_PICTURE:
                    if (data != null) {
                        uri = data.getData();
                    }
                    checkCrop(uri);
                    break;
                case CROP_PICTURE:
                    Bitmap photo = BitmapFactory.decodeFile(mHeadCachePath);
                    saveAndCallBack(photo);
                    mHeadCachePath = null;
                    break;
                default:
                    break;
            }
        } else {//取消
            finish();
        }
    }

    //回调
    private void saveAndCallBack(Bitmap photo) {
        SaveBitmap(photo,FILE_NAME);
        Message.UnityMessage( "OpenPhotoCallback", FILE_NAME);
        finish();
    }

    private void checkCrop(Uri uri) {
        if (needCrop) {
            cropImage(uri, _cropX, _cropY);
        } else {
            ContentResolver cr = this.getContentResolver();
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                saveAndCallBack(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("图片不存在");
            }
        }
    }

    private void checkOptCode() {
        if (msgCode == MsgDefine.SHOW_SELECT_WINDOW) {
            showSelectWindow();
        } else if (msgCode == MsgDefine.CHOOSE_PICTURE) {
            choosePicture();
        } else if (msgCode == MsgDefine.TAKE_PICTURE) {
            takePicture();
        }
    }

    public Uri getImageUri() {
        String fold= cachePath;
        File file = new File(fold, "temp/" + System.currentTimeMillis() + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        String path = file.getPath();
        Log.e(TAG, "getImageUri path:"+path);
        Uri fileUri = FileUtil.getUriFromFile(this, file);
        return fileUri;
    }

    public void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = getImageUri();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    public void choosePicture() {
        Intent openAlbumIntent = new Intent(Intent.ACTION_PICK);
        openAlbumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(openAlbumIntent, CHOOSE_PICTURE);
    }

    public void showSelectWindow() {
        String selectTitle = "Choose a photo source";
        String selectCancel = "Cancel";
        String selectCamera = "Take a picture";
        String selectPhoto = "From the album";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectTitle);
        builder.setNegativeButton(selectCancel, (arg0, arg1) -> finish());
        builder.setOnCancelListener(arg0 -> finish());
        builder.setItems(new String[]{selectCamera, selectPhoto}, (dialog, which) -> {
            switch (which)
            {
                case TAKE_PICTURE:
                    takePicture();
                    dialog.dismiss();
                    break;
                case CHOOSE_PICTURE:
                    choosePicture();
                    dialog.dismiss();
                    break;
                default:
                    break;
            }
        });
        builder.create().show();
    }

    String mHeadCachePath;
    //截取图片
    private void cropImage(Uri uri, int outputX, int outputY) {
        Intent intent = new Intent("com.android.camera.action.CROP");

        //参考上述知识点1
        intent.setDataAndType(uri, "image/*");
        // 设置裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        //当裁剪比例为1：1 时,部分手机显示为圆形裁剪器
        intent.putExtra("aspectX", outputX);
        intent.putExtra("aspectY", outputY);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);

        mHeadCachePath = cachePath + File.separator + "hb" + File.separator + "headCache";
        File parentPath = new File(mHeadCachePath);
        if (!parentPath.exists()) {
            parentPath.mkdirs();
        }
        File mHeadCacheFile = new File(parentPath, "head.png");
        if (!mHeadCacheFile.exists()) {
            try {
                mHeadCacheFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mHeadCachePath = mHeadCacheFile.getAbsolutePath();
        Log.e(TAG, "mHeadCachePath:"+mHeadCachePath);
        Uri uriPath = Uri.parse("file://" + mHeadCacheFile.getAbsolutePath());
        //  Uri uriPath = FileProviderUtil.getUriFromFile(this, mHeadCacheFile);
        //将裁剪好的图输出到所建文件中
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriPath);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("return-data", true);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivityForResult(intent, PhotoActivity.CROP_PICTURE);
    }

    public void SaveBitmap(Bitmap bitmap,String name) {
        FileOutputStream fOut;
        String path = filePath;
        try {
            File destDir = new File(path);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            //将Bitmap对象写入本地路径中，Unity在去相同的路径来读取这个文件
            fOut = new FileOutputStream(path + "/" + name);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            try {
                fOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        int options = 100;
        while (bytes.toByteArray().length / 1024 > 100) {
            bytes.reset();
            image.compress(Bitmap.CompressFormat.PNG, options, bytes);
            options -= 10;
            if (options < 10) break;
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(bytes.toByteArray());
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }

    //权限
    private final String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int PERMISSION_CODE = 0x00010001;

    private boolean checkPermissionAllGranted(String[] permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "hasPermissions: API version < M, returning true by default");
            return true;
        }
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}