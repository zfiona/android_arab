package com.yixun.chat;

import static com.yixun.chat.Utils.sendCommCallbackMessage;

import android.content.Context;

import io.rong.push.PushType;
import io.rong.push.notification.PushMessageReceiver;
import io.rong.push.notification.PushNotificationMessage;


public class RCPushReceiver extends PushMessageReceiver {
    @Override
    public boolean onNotificationMessageArrived(Context context, PushType pushType, PushNotificationMessage message) {
        sendCommCallbackMessage("pushmessage-arrived",Convert.toJson(message,pushType));

        return false;
    }

    @Override
    public boolean onNotificationMessageClicked(Context context, PushType pushType, PushNotificationMessage message) {
        sendCommCallbackMessage("pushmessage-clicked",Convert.toJson(message,pushType));
        return false;
    }
}
