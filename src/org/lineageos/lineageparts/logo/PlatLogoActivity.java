/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2017-2018 The LineageOS Project
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

package org.lineageos.lineageparts.logo;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import org.lineageos.lineageparts.R;

public class PlatLogoActivity extends Activity {
    private FrameLayout mLayout;
    private TimeAnimator mAnim;
    private PBackground mBG;

    private long mTouchHeld = 0;
    private int mTapCount;

    // Color matrix to force the logo to be white regardless of input color.
    private static final float[] WHITE = {
            1,     1,     1,    0,    255, // red
            1,     1,     1,    0,    255, // green
            1,     1,     1,    0,    255, // blue
            0,     0,     0,    1,      0  // alpha
    };
    private static final int BASE_SCALE = 50; // magic number scale multiple. Looks good on all DPI
    private static final long LONG_PRESS_TIMEOUT= new Long(ViewConfiguration.getLongPressTimeout());

    private class PBackground extends Drawable {
        private float mRadius, mX, mY, mDP;
        private int[] mPalette;
        private int mDarkest;
        private float mOffset;

        // LineageOS logo drawable
        private Drawable mLogo;
        private Drawable mBackground;
        public PBackground(Context context) {
            //randomizePalette();
            // LineageOS logo
            Log.v("PlatLogoActivity", "trying to load megamendung");
            mLogo = context.getResources().getDrawable(R.drawable.background_nenggala);
            mLogo.setColorFilter(new ColorMatrixColorFilter(WHITE)); // apply color filter
            mLogo.setBounds(0, 0, 360, 180); // Aspect ratio 2:1
            Log.v("PlatLogoActivity", "megamendung loaded");
            //mBackground = context.getResources().getDrawable(R.drawable.megamendung);
            //Log.v("PlatLogoActivity", "megamendung loaded");
        }

        /**
         * set inner radius of circles
         */
        public void setRadius(float r) {
            this.mRadius = Math.max(48 * mDP, r);
        }

        /**
         * move the circles
         */
        public void setPosition(float x, float y) {
            this.mX = x;
            this.mY = y;
        }

        /**
         * for animating the circles
         */
        public void setOffset(float o) {
            this.mOffset = o;
        }

        /**
         * rough luminance calculation
         * https://www.w3.org/TR/AERT/#color-contrast
         */
        public float lum(int rgb) {
            return ((Color.red(rgb) * 299f) + (Color.green(rgb) * 587f)
                    + (Color.blue(rgb) * 114f)) / 1000f;
        }

        /**
         * create a random evenly-spaced color palette
         * guaranteed to contrast!
         * PS: This is a lie :(
         */
        public void randomizePalette() {
            final int slots = 2 + (int)(Math.random() * 2);
            float[] color = new float[] { (float) Math.random() * 360f, 1f, 1f };
            mPalette = new int[slots];
            mDarkest = 0;
            for (int i = 0; i < slots; i++) {
                mPalette[i] = Color.HSVToColor(color);
                color[0] += 360f / slots;
                if (lum(mPalette[i]) < lum(mPalette[mDarkest])) mDarkest = i;
            }

            final StringBuilder str = new StringBuilder();
            for (int c : mPalette) {
                str.append(String.format("#%08x ", c));
            }
            Log.v("PlatLogoActivity", "color palette: " + str);
        }


        @Override
        public void draw(Canvas canvas) {
            if (mDP == 0) mDP = getResources().getDisplayMetrics().density;
            final float width = canvas.getWidth();
            final float height = canvas.getHeight();
            final float inner_w = mRadius * 0.667f;
            if (mRadius == 0) {
                setPosition(width / 2, height / 2);
                setRadius(width / 7);
            }
            final Paint paint = new Paint();
            canvas.translate(mX, mY);

            float w = Math.max(canvas.getWidth(), canvas.getHeight())  * 1.414f;
            paint.setStyle(Paint.Style.FILL);

            // Draw LineageOS Logo drawable
            Log.v("PlatLogoActivity", "draw megamendung on canvas");
            canvas.save();
            {
                canvas.translate((-360 / 2) * mRadius / BASE_SCALE,
                        (-180 / 2) * mRadius / BASE_SCALE);
                canvas.scale(mRadius / BASE_SCALE, mRadius / BASE_SCALE);
                mLogo.draw(canvas);
            }
            canvas.restore();

            // Disable until we get a stage 2 easter egg
            // check if a long press event has occured
            // checkLongPressTimeout();
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }
    }

    // Long press event checker for onTouch
    private void checkLongPressTimeout() {
        if (mTouchHeld > 0 && mTapCount >= 5) {
            if (System.currentTimeMillis() - mTouchHeld >= LONG_PRESS_TIMEOUT) {
                // reset
                mTouchHeld = 0;
                mTapCount = 0;

                // Launch the Easter Egg
                mLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            startActivity(new Intent("org.lineageos.lineageparts.EASTER_EGG")
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                    .addCategory("com.android.internal.category.PLATLOGO"));
                        } catch (ActivityNotFoundException ex) {
                            Log.e("PlatLogoActivity", "No more eggs.");
                        }
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayout = new FrameLayout(this);
        setContentView(mLayout);

        mBG = new PBackground(getApplicationContext());
        mLayout.setBackground(mBG);

        mLayout.setOnTouchListener(new View.OnTouchListener() {
            final PointerCoords pc0 = new PointerCoords();
            final PointerCoords pc1 = new PointerCoords();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // make sure the user doesnt launch stage 2 while zooming
                if (event.getPointerCount() > 1 && mTouchHeld > 0) {
                    mTouchHeld = 0;
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mBG.randomizePalette();
                        if (mTapCount < 5) mTapCount++; // avoid overflow
                        mTouchHeld = System.currentTimeMillis(); // get time for long press
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Two finger zoom gesture
                        if (event.getPointerCount() > 1) {
                            event.getPointerCoords(0, pc0);
                            event.getPointerCoords(1, pc1);
                            mBG.setRadius((float) Math.hypot(pc0.x - pc1.x, pc0.y - pc1.y) / 2f);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // reset
                        if (mTouchHeld > 0) {
                            mTouchHeld = 0;
                        }
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        mBG.randomizePalette();

        mAnim = new TimeAnimator();
        mAnim.setTimeListener(
                new TimeAnimator.TimeListener() {
                    @Override
                    public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                        mBG.setOffset((float) totalTime / 60000f);
                        mBG.invalidateSelf();
                    }
                });

        mAnim.start();
    }

    @Override
    public void onStop() {
        if (mAnim != null) {
            mAnim.cancel();
            mAnim = null;
        }
        super.onStop();
    }
}
