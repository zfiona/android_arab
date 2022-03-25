package com.yixun.tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.AppInviteDialog;
import com.facebook.share.widget.ShareDialog;
import com.yixun.message.Message;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

public class SocialUtil {
    @SuppressLint("StaticFieldLeak")
    private static SocialUtil sInstance;
    public static SocialUtil getInstance() {
        if (sInstance == null) {
            sInstance = new SocialUtil();
        }
        return sInstance;
    }

    private final FacebookCallback<LoginResult> loginCallback;
    private final FacebookCallback<Sharer.Result> shareCallback;
    private CallbackManager mFaceBookCallBack;
    private Activity mainActivity;

    private SocialUtil() {
        this.loginCallback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken token = loginResult.getAccessToken();
                Message.UnityMessage("OnLoginSuccess", token.getToken());
            }

            @Override
            public void onCancel() {
                Message.UnityMessage("OnLoginFail", "cancel");
            }

            @Override
            public void onError(FacebookException e) {
                Message.UnityMessage("OnLoginFail", "error");
                Message.UnityLog(e.getMessage());
            }
        };

        this.shareCallback = new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                //  AppActivity.cocosLog("分享成功");

                Message.UnityMessage("OnShareSuccess", "success");
            }

            @Override
            public void onCancel() {
                Message.UnityLog("分享取消");
            }

            @Override
            public void onError(FacebookException e) {
                Message.UnityLog("分享失败:" + e.getMessage());
            }
        };
    }


    public void InitFB(Activity activity) {
        this.mainActivity = activity;
        Message.UnityLog(printKeyHash(activity));
        FacebookSdk.sdkInitialize(activity.getApplicationContext());
        AppEventsLogger.activateApp(activity.getApplication());

        mFaceBookCallBack = CallbackManager.Factory.create();
        ShareDialog _fbShareDialog = new ShareDialog(activity);
        if (mFaceBookCallBack != null) {
            LoginManager.getInstance().registerCallback(mFaceBookCallBack, this.loginCallback);
            _fbShareDialog.registerCallback(mFaceBookCallBack, this.shareCallback);
        }
    }

    @SuppressLint("PackageManagerGetSignatures")
    public static String printKeyHash(Activity context) {
        PackageInfo packageInfo;
        String key = null;
        try {
            //getting application package name, as defined in manifest
            String packageName = context.getApplicationContext().getPackageName();

            //Retrieving package info
            packageInfo = context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);

            Log.e("Package Name=", context.getApplicationContext().getPackageName());

            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                key = new String(Base64.encode(md.digest(), 0));

                // String key = new String(Base64.encodeBytes(md.digest()));
                System.out.println("facebook printKeyHash Key Hash=" + key);
            }
        } catch (PackageManager.NameNotFoundException e1) {
            Log.e("Name not found", e1.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e("No such an algorithm", e.toString());
        } catch (Exception e) {
            Log.e("Exception", e.toString());
        }

        return key;
    }


    public void faceBookLogin() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            Message.UnityLog("FB已经登陆了");
            Message.UnityMessage("OnLoginSuccess", accessToken.getToken());
            return;
        }
        //"public_profile", "email"
        LoginManager.getInstance().logInWithReadPermissions(this.mainActivity, Arrays.asList("public_profile", "email"));
    }

    /**
     * facebook 分享链接
     *
     * @param linkUrl 链接地址
     */
    public void fbShareLink(String linkUrl) {
        if (ShareDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent linkContent = new ShareLinkContent.Builder()
                    .setContentUrl(Uri.parse(linkUrl))
                  //  .setContentTitle(title)  失效
                  //  .setContentDescription(content)
                    .build();

            ShareDialog.show(this.mainActivity, linkContent);
        }
    }

    /**
     * facebook 分享图片
     *
     * @param imgPath 本地图片地址
     */
    public void fbShareImg(String imgPath) {
        Message.UnityLog("begin facebookShare------>");
        File file = new File(imgPath);
        if (!file.exists()) {
            Message.UnityLog("Share IMG file no exists");
            return;
        }
        Bitmap img = BitmapFactory.decodeFile(imgPath);
        SharePhoto photo = new SharePhoto.Builder()
                .setBitmap(img)
                .build();
        SharePhotoContent content = new SharePhotoContent.Builder()
                .addPhoto(photo)
                .build();

        if (ShareDialog.canShow(SharePhotoContent.class)) {

            ShareDialog.show(this.mainActivity, content);
        } else if (hasPublishPermission()) {
            ShareApi.share(content, this.shareCallback);
        }
    }


    public void fbAppLinkInvite(String appLinkUrl, String previewImageUrl) {
        if (AppInviteDialog.canShow()) {
            AppInviteContent content = new AppInviteContent.Builder()
                    .setApplinkUrl(appLinkUrl)
                    .setPreviewImageUrl(previewImageUrl)
                    .build();
            AppInviteDialog.show(this.mainActivity, content);
        }
       else {
           Message.UnityLog("can not fbAppLinkInvite");
        }

    }

    public void whatAppShare(String imgPath, String content) {
        File file = new File(imgPath);
        if (!file.exists()) {
            Message.UnityLog("Share IMG file no exists");
            return;
        }
        Uri bmpUri = FileUtil.getUriFromFile(this.mainActivity, file);
        Intent shareIntent = new Intent();
        shareIntent.setPackage("com.whatsapp");
        shareIntent.setAction(Intent.ACTION_SEND);
        if (content != null && !content.isEmpty()) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        this.mainActivity.startActivity(Intent.createChooser(shareIntent, "Share Opportunity"));

    }

    private boolean hasPublishPermission() {
        return !Objects.requireNonNull(AccessToken.getCurrentAccessToken()).isExpired()
                && AccessToken.getCurrentAccessToken().getPermissions().contains("publish_actions");
    }


    public void setActivityResult(int requestCode, int resultCode, Intent data) {
        if (mFaceBookCallBack != null) {
            mFaceBookCallBack.onActivityResult(requestCode, resultCode, data);
        }
    }
}
