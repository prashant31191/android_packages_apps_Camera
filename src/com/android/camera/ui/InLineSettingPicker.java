/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.camera.R;
import com.android.camera.ListPreference;

import java.util.Formatter;

/* A one-line camera setting that includes a title (ex: Picture size), a
   previous button, the current value (ex: 5MP), and a next button. Other
   setting popup window includes several InLineSettingPicker. */
public class InLineSettingPicker extends RelativeLayout {
    private final String TAG = "InLineSettingPicker";
    // The view that shows the name of the setting. Ex: Picture size
    private TextView mTitle;
    // The view that shows the current selected setting. Ex: 5MP
    private TextView mEntry;
    private Button mPrevButton, mNextButton;
    private ListPreference mPreference;
    private boolean mNext, mPrevious;
    private int mIndex;
    private String mKey;
    private Listener mListener;
    // Scene mode can override the original preference value.
    private String mOverrideValue;

    static public interface Listener {
        public void onSettingChanged();
    }

    private Handler mHandler;
    private final Runnable mRunnable = new Runnable() {
        public void run() {
            if (mNext) {
                if (changeIndex(mIndex - 1)) {
                    mHandler.postDelayed(this, 100);
                }
            } else if (mPrevious) {
                if (changeIndex(mIndex + 1)) {
                    mHandler.postDelayed(this, 100);
                }
            }
        }
    };

    public InLineSettingPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandler = new Handler();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        OnTouchListener nextTouchListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (mOverrideValue != null) return true;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!mNext && changeIndex(mIndex - 1)) {
                        mNext = true;
                        // Give bigger delay so users can change only one step.
                        mHandler.postDelayed(mRunnable, 300);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    mNext = false;
                }
                return false;
            }
        };

        OnTouchListener previousTouchListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (mOverrideValue != null) return true;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!mPrevious && changeIndex(mIndex + 1)) {
                        mPrevious = true;
                        // Give bigger delay so users can change only one step.
                        mHandler.postDelayed(mRunnable, 300);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    mPrevious = false;
                }
                return false;
            }
        };

        mNextButton = (Button) findViewById(R.id.increment);
        mNextButton.setOnTouchListener(nextTouchListener);
        mPrevButton = (Button) findViewById(R.id.decrement);
        mPrevButton.setOnTouchListener(previousTouchListener);
        mEntry = (TextView) findViewById(R.id.current_setting);
        mTitle = (TextView) findViewById(R.id.title);
    }

    public void initialize(ListPreference preference) {
        mPreference = preference;
        mIndex = mPreference.findIndexOfValue(mPreference.getValue());
        updateView();
    }

    private boolean changeIndex(int index) {
        if (index >= mPreference.getEntryValues().length || index < 0) return false;
        mIndex = index;
        mPreference.setValueIndex(mIndex);
        if (mListener != null) {
            mListener.onSettingChanged();
        }
        updateView();
        return true;
    }

    private void updateView() {
        if (mOverrideValue == null) {
            mEntry.setText(mPreference.getEntry());
            mNextButton.setVisibility(mIndex == 0 ? View.INVISIBLE : View.VISIBLE);
            mPrevButton.setVisibility(mIndex == mPreference.getEntryValues().length - 1
                    ? View.INVISIBLE : View.VISIBLE);
        } else {
            int index = mPreference.findIndexOfValue(mOverrideValue);
            if (index != -1) {
                mEntry.setText(mPreference.getEntries()[index]);
            } else {
                // Avoid the crash if camera driver has bugs.
                Log.e(TAG, "Fail to find override value=" + mOverrideValue);
                mPreference.print();
            }
            mNextButton.setVisibility(View.INVISIBLE);
            mPrevButton.setVisibility(View.INVISIBLE);
        }
    }

    public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }

    public void overrideSettings(String value) {
        mOverrideValue = value;
        updateView();
    }
}
