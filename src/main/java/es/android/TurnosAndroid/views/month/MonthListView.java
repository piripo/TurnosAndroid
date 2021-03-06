/*
 * Copyright (C) 2012 The Android Open Source Project
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

package es.android.TurnosAndroid.views.month;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.fragments.MonthFragment;

import java.util.TimeZone;

public class MonthListView extends ListView {
  private static final String TAG                               = MonthListView.class.getSimpleName();
  // These define the behavior of the fling. Below MIN_VELOCITY_FOR_FLING, do the system fling
  // behavior. Between MIN_VELOCITY_FOR_FLING and MULTIPLE_MONTH_VELOCITY_THRESHOLD, do one month
  // fling. Above MULTIPLE_MONTH_VELOCITY_THRESHOLD, do multiple month flings according to the
  // fling strength. When doing multiple month fling, the velocity is reduced by this threshold
  // to prevent moving from one month fling to 4 months and above flings.
  private static       int    MIN_VELOCITY_FOR_FLING            = 1500;
  private static       int    MULTIPLE_MONTH_VELOCITY_THRESHOLD = 2000;
  private static       int    FLING_VELOCITY_DIVIDER            = 500;
  private static       int    FLING_TIME                        = 1000;

  private VelocityTracker velocityTracker;
  private Time            tempTime;
  private long            downActionTime;
  private Rect            firstViewRect;

  public MonthListView(Context context) {
    super(context);
    init(context);
  }

  public MonthListView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public MonthListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {
    TimeZone timeZone = TimeZone.getDefault();
    float friction = 1.0f;
    setCacheColorHint(0);
    setDivider(null);
    setItemsCanFocus(true);
    setFastScrollEnabled(false);
    setVerticalScrollBarEnabled(false);
    setFadingEdgeLength(0);
    setFriction(ViewConfiguration.getScrollFriction() * friction);
    setBackgroundColor(getResources().getColor(R.color.month_bgcolor));
    firstViewRect = new Rect();
    velocityTracker = VelocityTracker.obtain();
    tempTime = new Time(timeZone.getDisplayName());
    float scale = context.getResources().getDisplayMetrics().density;
    if (scale != 1) {
      MIN_VELOCITY_FOR_FLING *= scale;
      MULTIPLE_MONTH_VELOCITY_THRESHOLD *= scale;
      FLING_VELOCITY_DIVIDER *= scale;
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    return processEvent(ev) || super.onTouchEvent(ev);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    return processEvent(ev) || super.onInterceptTouchEvent(ev);
  }

  private boolean processEvent(MotionEvent ev) {
    switch (ev.getAction() & MotionEvent.ACTION_MASK) {
      // Since doFling sends a cancel, make sure not to process it.
      case MotionEvent.ACTION_CANCEL:
        return false;
      // Start tracking movement velocity
      case MotionEvent.ACTION_DOWN:
        velocityTracker.clear();
        downActionTime = SystemClock.uptimeMillis();
        break;
      // Accumulate velocity and do a custom fling when above threshold
      case MotionEvent.ACTION_UP:
        velocityTracker.addMovement(ev);
        velocityTracker.computeCurrentVelocity(1000);    // in pixels per second
        float yVelocity = velocityTracker.getYVelocity();
        if (Math.abs(yVelocity) > MIN_VELOCITY_FOR_FLING) {
          doFling(yVelocity);
          return true;
        }
        break;
      default:
        velocityTracker.addMovement(ev);
        break;
    }
    return false;
  }

  // Do a "snap to start of month" fling
  private void doFling(float velocityY) {
    // Stop the list-view movement and take over
    MotionEvent cancelEvent = MotionEvent.obtain(downActionTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0);
    onTouchEvent(cancelEvent);

    // Below the threshold, fling one month. Above the threshold, fling according to the speed of the fling.
    int monthsToJump;
    if (Math.abs(velocityY) < MULTIPLE_MONTH_VELOCITY_THRESHOLD) {
      if (velocityY < 0) {
        monthsToJump = 1;
      } else {
        // value here is zero and not -1 since by the time the fling is detected the list moved back one month.
        monthsToJump = 0;
      }
    } else {
      if (velocityY < 0) {
        monthsToJump = 1 - (int) ((velocityY + MULTIPLE_MONTH_VELOCITY_THRESHOLD) / FLING_VELOCITY_DIVIDER);
      } else {
        monthsToJump = -(int) ((velocityY - MULTIPLE_MONTH_VELOCITY_THRESHOLD) / FLING_VELOCITY_DIVIDER);
      }
    }

    // Get the day at the top right corner
    int day = getUpperRightJulianDay();
    // Get the day of the first day of the next/previous month (according to scroll direction)
    tempTime.setJulianDay(day);
    tempTime.monthDay = 1;
    tempTime.month += monthsToJump;
    long timeInMillis = tempTime.normalize(false);
    // Since each view is 7 days, round the target day up to make sure the scroll will be  at least one view.
    int scrollToDay = Time.getJulianDay(timeInMillis, tempTime.gmtoff) + ((monthsToJump > 0) ? 6 : 0);

    // Since all views have the same height, scroll by pixels instead of "to position". Compensate for the top view offset from the top.
    View firstView = getChildAt(0);
    int firstViewHeight = firstView.getHeight();
    // Get visible part length
    firstView.getLocalVisibleRect(firstViewRect);
    int topViewVisiblePart = firstViewRect.bottom - firstViewRect.top;
    int viewsToFling = (scrollToDay - day) / 7 - ((monthsToJump <= 0) ? 1 : 0);
    int offset = (viewsToFling > 0) ? -(firstViewHeight - topViewVisiblePart + MonthFragment.LIST_TOP_OFFSET) : (topViewVisiblePart - MonthFragment.LIST_TOP_OFFSET);
    // Fling
    smoothScrollBy(viewsToFling * firstViewHeight + offset, FLING_TIME);
  }

  /**
   * @return the julian day of the day in the upper right corner
   */
  private int getUpperRightJulianDay() {
    WeekView child = (WeekView) getChildAt(0);
    if (child == null) {
      return -1;
    }
    return child.getFirstJulianDay() + MonthFragment.DAYS_PER_WEEK - 1;
  }
}
