package com.apical.ipcamtest;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class MyTextView extends android.support.v7.widget.AppCompatTextView {
    public MyTextView(Context context) {
        super(context);
        setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
    }
    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
    }
    public MyTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
    }
}
