package com.yixun.chat;

import android.Manifest;
import android.app.Activity;
import android.text.TextUtils;

import com.yixun.message.Message;
import com.yixun.saukbaloot.MainActivity;
import com.yixun.tools.IResultBack;
import com.yixun.tools.PermissionUtil;

import cn.rongcloud.voiceroom.api.RCVoiceRoomEngine;
import cn.rongcloud.voiceroom.api.callback.RCVoiceRoomCallback;
import cn.rongcloud.voiceroom.model.RCVoiceRoomInfo;

public class RongSpeech extends Activity {
    private static boolean isInit = false;
    private static final String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};

    public static void Init(String appKey) {
        if (isInit){ return; }
        MainActivity.instance.CheckPermissions(permissions,(accept)->{
            if(accept){
                isInit = true;
                RCVoiceRoomEngine.getInstance().initWithAppKey(MainActivity.instance.getApplication(), appKey);
            }
        });
    }

    private static void Connect(String token, IResultBack<Boolean> callback){
        if(!isInit) return;
        //先断开连接
        RCVoiceRoomEngine.getInstance().disconnect();
        //连接
        RCVoiceRoomEngine.getInstance().connectWithToken(token, new RCVoiceRoomCallback() {
                @Override
                public void onSuccess() {
                    Message.UnityLog("connect success");
                    callback.onResult(true);
                }
                @Override
                public void onError(int code, String s) {
                    String info = "connect fail：\n【" + code + "】" + s;
                    Message.UnityLog(info);
                    callback.onResult(false);
                }
            }
        );
    }

    static RCVoiceRoomInfo roomInfo;
    public static void CreateAndJoinRoom(String token,String roomId){
        if(!isInit) return;
        if (TextUtils.isEmpty(roomId)) {
            Message.UnityLog("voice room id is null");
            return;
        }
        if (roomInfo == null){
            roomInfo = new RCVoiceRoomInfo();
            roomInfo.setRoomName("Room_" + roomId);
            roomInfo.setSeatCount(4);
            roomInfo.setLockAll(false);
            roomInfo.setMuteAll(false);
            roomInfo.setFreeEnterSeat(true);
        }
        Connect(token,success->{
            if (success){
                RCVoiceRoomEngine.getInstance().createAndJoinRoom(roomId, roomInfo, new RCVoiceRoomCallback() {
                    @Override
                    public void onSuccess() {
                        Message.UnityLog("createRoom success");
                        //房主上麦 index = 0
                        //EnterSeat(0);
                    }
                    @Override
                    public void onError(int code, String message) { Message.UnityLog("createRoom fail :" + message); }
                });
            }
        });
    }

    public static void JoinRoom(String token,String roomId){
        if(!isInit) return;
        Connect(token,success->{
            if (success){
                RCVoiceRoomEngine.getInstance().joinRoom(roomId, new RCVoiceRoomCallback() {
                    @Override
                    public void onSuccess() {
                        Message.UnityLog("joinRoom success");
                    }
                    @Override
                    public void onError(int code, String message) { Message.UnityLog("joinRoom fail: " + message); }
                });
            }
        });
    }

    public static void LeaveRoom(){
        if(!isInit) return;
        RCVoiceRoomEngine.getInstance().leaveRoom(new RCVoiceRoomCallback() {
            @Override
            public void onSuccess() {
                Message.UnityLog("leaveRoom success");
                RCVoiceRoomEngine.getInstance().disconnect();
            }
            @Override
            public void onError(int code, String message) {
                String info = "leaveRoom fail [" + code + "]:" + message;
                Message.UnityLog(info);
            }
        });
    }

    public static void KickFromSeat(String userId){
        if(!isInit) return;
        RCVoiceRoomEngine.getInstance().kickUserFromSeat(userId, new DefaultRoomCallback("抱下麦", null));
    }

    public static void KickFromRoom(String userId){
        if(!isInit) return;
        RCVoiceRoomEngine.getInstance().kickUserFromRoom(userId, new DefaultRoomCallback("抱出房", null));
    }

    //player
    public static void EnterSeat(int index){
        if(!isInit) return;
        RCVoiceRoomEngine.getInstance().enterSeat(index, new DefaultRoomCallback("上麦", null));
    }

    public static void SwitchSeatTo(int index){
        if(!isInit) return;
        RCVoiceRoomEngine.getInstance().switchSeatTo(index, new DefaultRoomCallback("换麦", null));
    }

    public static void ExitSeat(){
        if(!isInit) return;
        RCVoiceRoomEngine.getInstance().leaveSeat(new DefaultRoomCallback("下麦", null));
    }

    public static void MuteAll(boolean mute){
        if(!isInit) return;
        String action = mute ? "全麦静音" : "全麦取消静音";
        RCVoiceRoomEngine.getInstance().muteOtherSeats(mute, new DefaultRoomCallback(action, null));
    }

    public static void MuteSeat(int index,boolean mute){
        if(!isInit) return;
        String action = mute ? "麦位静音" : "取消麦位静音";
        RCVoiceRoomEngine.getInstance().muteSeat(index, mute, new DefaultRoomCallback(action, null));
    }


    private static class DefaultRoomCallback implements RCVoiceRoomCallback {
        private final IResultBack<Boolean> resultBack;
        private final String action;

        DefaultRoomCallback(String action, IResultBack<Boolean> resultBack) {
            this.resultBack = resultBack;
            this.action = action;
        }

        @Override
        public void onSuccess() {
            if (null != resultBack) resultBack.onResult(true);
            if (!TextUtils.isEmpty(action))
                Message.UnityLog(action + "成功");
        }

        @Override
        public void onError(int i, String s) {
            String info = action + "fail [" + i + "]:" + s;
            if (!TextUtils.isEmpty(action))
                Message.UnityLog(info);
            if (null != resultBack)
                resultBack.onResult(false);
        }
    }
}



