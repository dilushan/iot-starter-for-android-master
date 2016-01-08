/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *******************************************************************************/
package com.ibm.iot.android.iotstarter.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import com.ibm.iot.android.iotstarter.IoTStarterApplication;
import com.ibm.iot.android.iotstarter.activities.*;
import com.ibm.iot.android.iotstarter.fragments.IoTFragment;
import com.ibm.iot.android.iotstarter.fragments.LogFragment;
import com.ibm.iot.android.iotstarter.fragments.LoginFragment;
import org.json.JSONException;
import org.json.JSONObject;
import android.net.wifi.*;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Steer incoming MQTT messages to the proper activities based on their content.
 */
public class MessageConductor {

    private final static String TAG = MessageConductor.class.getName();
    private static MessageConductor instance;
    private Context context;
    private IoTStarterApplication app;

    private MessageConductor(Context context) {
        this.context = context;
        app = (IoTStarterApplication) context.getApplicationContext();
    }

    public static MessageConductor getInstance(Context context) {
        if (instance == null) {
            instance = new MessageConductor(context);
        }
        return instance;
    }

    /**
     * Steer incoming MQTT messages to the proper activities based on their content.
     *
     * @param payload The log of the MQTT message.
     * @param topic The topic the MQTT message was received on.
     * @throws JSONException If the message contains invalid JSON.
     */
    public void steerMessage(String payload, String topic) throws JSONException {
        Log.d(TAG, ".steerMessage() entered");
        JSONObject top = new JSONObject(payload);
        JSONObject d = top.getJSONObject("d");

        if (topic.contains(Constants.COLOR_EVENT)) {
            Log.d(TAG, "Color Event");
            int r = d.getInt("r");
            int g = d.getInt("g");
            int b = d.getInt("b");
            // alpha value received is 0.0 < a < 1.0 but Color.agrb expects 0 < a < 255
            int alpha = (int)(d.getDouble("alpha")*255.0);
            if ((r > 255 || r < 0) ||
                    (g > 255 || g < 0) ||
                    (b > 255 || b < 0) ||
                    (alpha > 255 || alpha < 0)) {
                return;
            }

            app.setColor(Color.argb(alpha, r, g, b));

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null && runningActivity.equals(IoTFragment.class.getName())) {
                Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
                actionIntent.putExtra(Constants.INTENT_DATA, Constants.COLOR_EVENT);
                context.sendBroadcast(actionIntent);
            }
        } else if (topic.contains(Constants.LIGHT_EVENT)) {
            app.handleLightMessage();
        } else if (topic.contains(Constants.TEXT_EVENT)) {
            int unreadCount = app.getUnreadCount();
            app.setUnreadCount(++unreadCount);

            // save payload in an arrayList
            List messageRecvd = new ArrayList<String>();
            messageRecvd.add(payload);

            app.getMessageLog().add(d.getString("text"));

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null && runningActivity.equals(LogFragment.class.getName())) {
                Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOG);
                actionIntent.putExtra(Constants.INTENT_DATA, Constants.TEXT_EVENT);
                context.sendBroadcast(actionIntent);
            }

            Intent unreadIntent;
            if (runningActivity.equals(LogFragment.class.getName())) {
                unreadIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOG);
            } else if (runningActivity.equals(LoginFragment.class.getName())) {
                unreadIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
            } else if (runningActivity.equals(IoTFragment.class.getName())) {
                unreadIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
            } else if (runningActivity.equals(ProfilesActivity.class.getName())) {
                unreadIntent = new Intent(Constants.APP_ID + Constants.INTENT_PROFILES);
            } else {
                return;
            }

            String messageText = d.getString("text");
            if (messageText != null) {
                unreadIntent.putExtra(Constants.INTENT_DATA, Constants.UNREAD_EVENT);
                context.sendBroadcast(unreadIntent);
            }
        } else if (topic.contains(Constants.ALERT_EVENT)) {
            // save payload in an arrayList
            int unreadCount = app.getUnreadCount();
            app.setUnreadCount(++unreadCount);

            List messageRecvd = new ArrayList<String>();
            messageRecvd.add(payload);

            app.getMessageLog().add(d.getString("text"));

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null) {
                if (runningActivity.equals(LogFragment.class.getName())) {
                    Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOG);
                    actionIntent.putExtra(Constants.INTENT_DATA, Constants.TEXT_EVENT);
                    context.sendBroadcast(actionIntent);
                }

                Intent alertIntent;
                if (runningActivity.equals(LogFragment.class.getName())) {
                    alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOG);
                } else if (runningActivity.equals(LoginFragment.class.getName())) {
                    alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
                } else if (runningActivity.equals(IoTFragment.class.getName())) {
                    alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
                } else if (runningActivity.equals(ProfilesActivity.class.getName())) {
                    alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_PROFILES);
                } else {
                    return;
                }

                String messageText = d.getString("text");
                if (messageText != null) {
                    alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
                    alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE, d.getString("text"));
                    context.sendBroadcast(alertIntent);
                }
            }
        }else if(topic.contains("connect")){
            Log.d(TAG, "Connect Event");
            String ssid = d.getString("ssid");
            String pw = d.getString("pw");
            Toast.makeText(app.getBaseContext(), "Try to connect "+ssid+" "+pw, Toast.LENGTH_SHORT).show();
            Intent alertIntent;
            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity.equals(LogFragment.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOG);
            } else if (runningActivity.equals(LoginFragment.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
            } else if (runningActivity.equals(IoTFragment.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
            } else if (runningActivity.equals(ProfilesActivity.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_PROFILES);
            } else {
                return;
            }

            //connect wifi
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", pw);

            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            //enable wifi
            wifiManager.disconnect();
            wifiManager.setWifiEnabled(true); // true or false to activate/deactivate wifi

            //remember id
            int netId = wifiManager.addNetwork(wifiConfig);

            wifiManager.enableNetwork(netId, true);
            if(wifiManager.reconnect()){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                WifiInfo info = wifiManager.getConnectionInfo();
                ssid="\""+ssid+"\"";
                if(info.getSSID().equals(ssid)) {
                    alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
                    alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE, "Connected to " + ssid);
                    context.sendBroadcast(alertIntent);
                }else{
                    alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
                    alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE,"Connect to different network "+info.getSSID());
                    context.sendBroadcast(alertIntent);
                }
            }else{
                alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
                alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE,"Couldn't connect to "+ssid);
                context.sendBroadcast(alertIntent);
            }





        }else if(topic.contains("registerFsbl")){
            Log.d(TAG, "registerFsbl");
            Intent alertIntent;
            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity.equals(LogFragment.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOG);
            } else if (runningActivity.equals(LoginFragment.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
            } else if (runningActivity.equals(IoTFragment.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
            } else if (runningActivity.equals(ProfilesActivity.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_PROFILES);
            } else {
                return;
            }
            String messageText = d.toString();
            if (messageText != null) {
                alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
                alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE,messageText);
                context.sendBroadcast(alertIntent);
            }
        }else if(topic.contains("blister-health")){
            Log.d(TAG, "registerFsbl");
            Intent alertIntent;
            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity.equals(LogFragment.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOG);
            } else if (runningActivity.equals(LoginFragment.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
            } else if (runningActivity.equals(IoTFragment.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
            } else if (runningActivity.equals(ProfilesActivity.class.getName())) {
                alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_PROFILES);
            } else {
                return;
            }
            String messageText = d.toString();
            if (messageText != null) {
                alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
                alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE,messageText);
                context.sendBroadcast(alertIntent);
            }
        }




    }
}

