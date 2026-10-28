package com.minhminhhungyen.kidstv;

import android.util.Log;

/** Nhật ký cho logcat (thẻ KidsTV). Xem bằng bat 7-XEM-NHAT-KY. */
final class Dbg {
    static final String TAG = "KidsTV";

    private Dbg() {
    }

    static void d(String msg) {
        Log.i(TAG, msg);
    }
}
