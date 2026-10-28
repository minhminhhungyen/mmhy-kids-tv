package com.minhminhhungyen.kidstv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * TV khởi động xong, hoặc ứng dụng vừa được cập nhật: đặt lại báo thức và bật dịch vụ bảo vệ.
 * Khởi động lại coi như "bật TV": khóa chờ bé giải toán (nếu bật tính năng); cập nhật ứng dụng thì KHÔNG khóa.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i == null ? "" : String.valueOf(i.getAction());
        Dbg.event(c, "boot/replaced " + a);
        if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(a) && Session.powerCycle(c, "khoi dong " + a)) {
            if (!Prefs.parentActive(c)) {
                Home.bringToFront(c, "khoi dong");
            }
        }
        Session.rearm(c);
        GuardEnabler.ensure(c);
    }
}
