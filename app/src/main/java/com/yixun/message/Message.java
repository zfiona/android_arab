package com.yixun.message;

import com.unity3d.player.UnityPlayer;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class Message {
    public static void UnityMessage(String methodName, String param) {
        UnityPlayer.UnitySendMessage("DDOLGameObject", methodName, param);
    }

    public  static  void printException(Exception e){
        StringWriter sw=new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        UnityLog (sw.getBuffer().toString());

    }

    public static void UnityLog(String param) {
        UnityPlayer.UnitySendMessage("DDOLGameObject", "UnityPrint", param);
    }

    public static void UnityCall(String methodName, Map<String,Object> table) {
        String json = new JSONObject(table).toString();
        UnityPlayer.UnitySendMessage("DDOLGameObject", methodName, json);
    }
}
