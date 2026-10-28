package com.minhminhhungyen.kidstv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Báo thức hết giờ và nhắc trước 5 phút / 1 phút. */
public class TimeUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i == null ? null : i.getAction();
        Dbg.d("alarm " + a);
        if (Session.ACT_END.equals(a)) {
            if (Session.checkExpired(c)) {
                if (!Prefs.parentActive(c)) {
                    Home.bringToFront(c, "timeup");
                }
            } else {
                Session.rearm(c);
            }
        } else if (Session.ACT_WARN5.equals(a)) {
            if (Session.state(c) == Session.RUNNING) {
                Home.toast(c, "Còn 5 phút nữa là hết giờ xem TV");
            }
        } else if (Session.ACT_WARN1.equals(a)) {
            if (Session.state(c) == Session.RUNNING) {
                Home.toast(c, "Còn 1 phút nữa là hết giờ xem. Bé chuẩn bị nghỉ nhé!");
            }
        }
    }
}
