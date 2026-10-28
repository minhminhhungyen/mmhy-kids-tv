package com.minhminhhungyen.fakekids;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/** Màn hình giả của YouTube Kids (chỉ để thử trên máy ảo CI). */
public class FakeKidsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView t = new TextView(this);
        t.setText("FAKE YOUTUBE KIDS");
        t.setTextSize(48);
        t.setTextColor(Color.WHITE);
        t.setBackgroundColor(Color.rgb(200, 30, 30));
        t.setGravity(Gravity.CENTER);
        setContentView(t);
    }
}
