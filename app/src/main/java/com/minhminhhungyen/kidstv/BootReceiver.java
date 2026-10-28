package com.minhminhhungyen.kidstv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Sau khi TV khởi động lại (hoặc ứng dụng vừa được cập nhật): đặt lại báo thức và bật dịch vụ bảo vệ. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Dbg.d("boot/replaced " + (i == null ? "" : i.getAction()));
        Session.rearm(c);
        GuardEnabler.ensure(c);
    }
}
