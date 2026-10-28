package com.minhminhhungyen.kidstv;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Tiện ích đưa màn hình chính của ứng dụng lên trên và hiện thông báo ngắn. */
final class Home {
    private Home() {
    }

    static void bringToFront(Context c, String why) {
        try {
            Intent i = new Intent(c, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra("why", why);
            c.startActivity(i);
            Dbg.d("bringToFront: " + why);
        } catch (Throwable t) {
            Dbg.d("bringToFront fail: " + t);
        }
    }

    static void toast(Context c, String msg) {
        try {
            Toast.makeText(c.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Dbg.d("toast fail: " + t);
        }
    }
}
