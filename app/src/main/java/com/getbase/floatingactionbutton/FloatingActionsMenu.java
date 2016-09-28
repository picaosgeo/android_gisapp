/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.getbase.floatingactionbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.mobile.R;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.animation.AnimatorProxy;


public class FloatingActionsMenu
        extends ViewGroup
{
    public static final int EXPAND_UP    = 0;
    public static final int EXPAND_DOWN  = 1;
    public static final int EXPAND_LEFT  = 2;
    public static final int EXPAND_RIGHT = 3;

    private static final int   ANIMATION_DURATION      = 300;
    private static final float COLLAPSED_PLUS_ROTATION = 0f;
    private static final float EXPANDED_PLUS_ROTATION  = 90f + 45f;

    private int     mAddButtonPlusColor;
    private int     mAddButtonColorNormal;
    private int     mAddButtonColorPressed;
    private int     mAddButtonSize;
    private boolean mAddButtonStrokeVisible;
    private int     mExpandDirection;

    private int mButtonSpacing;
    private int mLabelsMargin;
    private int mLabelsVerticalOffset;

    private boolean mExpanded;

    private AnimatorSet mExpandAnimation   = new AnimatorSet().setDuration(ANIMATION_DURATION);
    private AnimatorSet mCollapseAnimation = new AnimatorSet().setDuration(ANIMATION_DURATION);
    private AddFloatingActionButton mAddButton;
    private RotatingDrawable        mRotatingDrawable;
    private int                     mMaxButtonWidth;
    private int                     mMaxButtonHeight;
    private int                     mLabelsStyle;
    private int                     mButtonsCount;

    private OnFloatingActionsMenuUpdateListener mListener;


    public interface OnFloatingActionsMenuUpdateListener
    {
        void onMenuExpanded();

        void onMenuCollapsed();
    }


    public FloatingActionsMenu(Context context)
    {
        this(context, null);
    }


    public FloatingActionsMenu(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }


    public FloatingActionsMenu(
            Context context,
            AttributeSet attrs,
            int defStyle)
    {
        super(context, attrs, defStyle);
        init(context, attrs);
    }


    private void init(
            Context context,
            AttributeSet attributeSet)
    {
        mButtonSpacing = (int) (getResources().getDimension(R.dimen.fab_actions_spacing) -
                                getResources().getDimension(R.dimen.fab_shadow_radius) -
                                getResources().getDimension(R.dimen.fab_shadow_offset));
        mLabelsMargin = getResources().getDimensionPixelSize(R.dimen.fab_labels_margin);
        mLabelsVerticalOffset = getResources().getDimensionPixelSize(R.dimen.fab_shadow_offset);

        TypedArray attr = context.obtainStyledAttributes(attributeSet, R.styleable.FloatingActionsMenu, 0, 0);
        mAddButtonPlusColor = attr.getColor(R.styleable.FloatingActionsMenu_fab_addButtonPlusIconColor, getColor(android.R.color.white));
        mAddButtonColorNormal = attr.getColor(R.styleable.FloatingActionsMenu_fab_addButtonColorNormal, ControlHelper.getColor(context, R.attr.colorPrimary));
        mAddButtonColorPressed = attr.getColor(R.styleable.FloatingActionsMenu_fab_addButtonColorPressed, ControlHelper.getColor(context, R.attr.colorAccent));
        mAddButtonSize = attr.getInt(R.styleable.FloatingActionsMenu_fab_addButtonSize, FloatingActionButton.SIZE_NORMAL);
        mAddButtonStrokeVisible = attr.getBoolean(R.styleable.FloatingActionsMenu_fab_addButtonStrokeVisible, true);
        mExpandDirection = attr.getInt(R.styleable.FloatingActionsMenu_fab_expandDirection, EXPAND_UP);
        mLabelsStyle = attr.getResourceId(R.styleable.FloatingActionsMenu_fab_labelStyle, 0);
        attr.recycle();

        if (mLabelsStyle != 0 && expandsHorizontally()) {
            throw new IllegalStateException(
                    "Action labels in horizontal expand orientation is not supported.");
        }

        createAddButton(context);
    }


    public void setOnFloatingActionsMenuUpdateListener(OnFloatingActionsMenuUpdateListener listener)
    {
        mListener = listener;
    }


    private boolean expandsHorizontally()
    {
        return mExpandDirection == EXPAND_LEFT || mExpandDirection == EXPAND_RIGHT;
    }


    private static class RotatingDrawable
            extends LayerDrawable
    {
        public RotatingDrawable(Drawable drawable)
        {
            super(new Drawable[] {drawable});
        }


        private float mRotation;


        @SuppressWarnings("UnusedDeclaration")
        public float getRotation()
        {
            return mRotation;
        }


        @SuppressWarnings("UnusedDeclaration")
        public void setRotation(float rotation)
        {
            mRotation = rotation;
            invalidateSelf();
        }


        @Override
        public void draw(Canvas canvas)
        {
            canvas.save();
            canvas.rotate(mRotation, getBounds().centerX(), getBounds().centerY());
            super.draw(canvas);
            canvas.restore();
        }
    }


    private void createAddButton(Context context)
    {
        mAddButton = new AddFloatingActionButton(context)
        {
            @Override
            void updateBackground()
            {
                mPlusColor = mAddButtonPlusColor;
                mColorNormal = mAddButtonColorNormal;
                mColorPressed = mAddButtonColorPressed;
                mStrokeVisible = mAddButtonStrokeVisible;
                super.updateBackground();
            }


            @Override
            public Drawable getIconDrawable()
            {
                final RotatingDrawable rotatingDrawable =
                        new RotatingDrawable(super.getIconDrawable());
                mRotatingDrawable = rotatingDrawable;

                final OvershootInterpolator interpolator = new OvershootInterpolator();

                final ObjectAnimator collapseAnimator = ObjectAnimator.ofFloat(
                        rotatingDrawable, "rotation", EXPANDED_PLUS_ROTATION,
                        COLLAPSED_PLUS_ROTATION);
                final ObjectAnimator expandAnimator = ObjectAnimator.ofFloat(
                        rotatingDrawable, "rotation", COLLAPSED_PLUS_ROTATION,
                        EXPANDED_PLUS_ROTATION);

                collapseAnimator.setInterpolator(interpolator);
                expandAnimator.setInterpolator(interpolator);

                mExpandAnimation.play(expandAnimator);
                mCollapseAnimation.play(collapseAnimator);

                return rotatingDrawable;
            }
        };

        mAddButton.setId(R.id.fab_expand_menu_button);
        mAddButton.setSize(mAddButtonSize);
        mAddButton.setOnClickListener(
                new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        toggle();
                    }
                });

        addView(mAddButton, super.generateDefaultLayoutParams());
    }


    public void addButton(FloatingActionButton button)
    {
        addView(button, mButtonsCount - 1);
        mButtonsCount++;

        if (mLabelsStyle != 0) {
            createLabels();
        }
    }


    public void removeButton(FloatingActionButton button)
    {
        removeView(button.getLabelView());
        removeView(button);
        mButtonsCount--;
    }


    private int getColor(
            @ColorRes
            int id)
    {
        return getResources().getColor(id);
    }


    @Override
    protected void onMeasure(
            int widthMeasureSpec,
            int heightMeasureSpec)
    {
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int width = 0;
        int height = 0;

        mMaxButtonWidth = 0;
        mMaxButtonHeight = 0;
        int maxLabelWidth = 0;

        for (int i = 0; i < mButtonsCount; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            switch (mExpandDirection) {
                case EXPAND_UP:
                case EXPAND_DOWN:
                    mMaxButtonWidth = Math.max(mMaxButtonWidth, child.getMeasuredWidth());
                    height += child.getMeasuredHeight();
                    break;
                case EXPAND_LEFT:
                case EXPAND_RIGHT:
                    width += child.getMeasuredWidth();
                    mMaxButtonHeight = Math.max(mMaxButtonHeight, child.getMeasuredHeight());
                    break;
            }

            if (!expandsHorizontally()) {
                TextView label = (TextView) child.getTag(R.id.fab_label);
                if (label != null) {
                    maxLabelWidth = Math.max(maxLabelWidth, label.getMeasuredWidth());
                }
            }
        }

        if (!expandsHorizontally()) {
            width = mMaxButtonWidth + (maxLabelWidth > 0 ? maxLabelWidth + mLabelsMargin : 0);
        } else {
            height = mMaxButtonHeight;
        }

        switch (mExpandDirection) {
            case EXPAND_UP:
            case EXPAND_DOWN:
                height += mButtonSpacing * (getChildCount() - 1);
                height = adjustForOvershoot(height);
                break;
            case EXPAND_LEFT:
            case EXPAND_RIGHT:
                width += mButtonSpacing * (getChildCount() - 1);
                width = adjustForOvershoot(width);
                break;
        }

        setMeasuredDimension(width, height);
    }


    private int adjustForOvershoot(int dimension)
    {
        return dimension * 12 / 10;
    }


    @Override
    protected void onLayout(
            boolean changed,
            int l,
            int t,
            int r,
            int b)
    {
        switch (mExpandDirection) {
            case EXPAND_UP:
            case EXPAND_DOWN:
                boolean expandUp = mExpandDirection == EXPAND_UP;

                int addButtonY = expandUp ? b - t - mAddButton.getMeasuredHeight() : 0;
                // Ensure mAddButton is centered on the line where the buttons should be
                int addButtonLeft = r - l - mMaxButtonWidth +
                                    (mMaxButtonWidth - mAddButton.getMeasuredWidth()) / 2;
                mAddButton.layout(
                        addButtonLeft, addButtonY, addButtonLeft + mAddButton.getMeasuredWidth(),
                        addButtonY + mAddButton.getMeasuredHeight());

                int labelsRight = r - l - mMaxButtonWidth - mLabelsMargin;

                int nextY = expandUp
                            ? addButtonY - mButtonSpacing
                            : addButtonY + mAddButton.getMeasuredHeight() + mButtonSpacing;

                for (int i = mButtonsCount - 1; i >= 0; i--) {
                    final View child = getChildAt(i);

                    if (child == mAddButton || child.getVisibility() == GONE) {
                        continue;
                    }

                    AnimatorProxy proxy = AnimatorProxy.wrap(child);

                    int childX = addButtonLeft +
                                 (mAddButton.getMeasuredWidth() - child.getMeasuredWidth()) / 2;
                    int childY = expandUp ? nextY - child.getMeasuredHeight() : nextY;

                    float collapsedTranslation = addButtonY - childY;
                    float expandedTranslation = 0f;

                    proxy.setTranslationY(mExpanded ? expandedTranslation : collapsedTranslation);
                    proxy.setAlpha(mExpanded ? 1f : 0f);

                    child.layout(
                            childX, childY, childX + child.getMeasuredWidth(),
                            childY + child.getMeasuredHeight());

                    LayoutParams params = (LayoutParams) child.getLayoutParams();
                    params.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation);
                    params.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation);
                    params.setAnimationsTarget(proxy);

                    View label = (View) child.getTag(R.id.fab_label);
                    if (label != null) {
                        int labelLeft = labelsRight - label.getMeasuredWidth();
                        int labelTop = childY - mLabelsVerticalOffset +
                                       (child.getMeasuredHeight() - label.getMeasuredHeight()) / 2;

                        AnimatorProxy proxyLabel = AnimatorProxy.wrap(label);

                        proxyLabel.setTranslationY(
                                mExpanded ? expandedTranslation : collapsedTranslation);
                        proxyLabel.setAlpha(mExpanded ? 1f : 0f);
                        label.layout(
                                labelLeft, labelTop, labelsRight,
                                labelTop + label.getMeasuredHeight());

                        LayoutParams labelParams = (LayoutParams) label.getLayoutParams();
                        labelParams.mCollapseDir.setFloatValues(
                                expandedTranslation, collapsedTranslation);
                        labelParams.mExpandDir.setFloatValues(
                                collapsedTranslation, expandedTranslation);
                        labelParams.setAnimationsTarget(proxyLabel);
                    }

                    nextY = expandUp
                            ? childY - mButtonSpacing
                            : childY + child.getMeasuredHeight() + mButtonSpacing;
                }
                break;

            case EXPAND_LEFT:
            case EXPAND_RIGHT:
                boolean expandLeft = mExpandDirection == EXPAND_LEFT;

                int addButtonX = expandLeft ? r - l - mAddButton.getMeasuredWidth() : 0;
                // Ensure mAddButton is centered on the line where the buttons should be
                int addButtonTop = b - t - mMaxButtonHeight +
                                   (mMaxButtonHeight - mAddButton.getMeasuredHeight()) / 2;
                mAddButton.layout(
                        addButtonX, addButtonTop, addButtonX + mAddButton.getMeasuredWidth(),
                        addButtonTop + mAddButton.getMeasuredHeight());

                int nextX = expandLeft
                            ? addButtonX - mButtonSpacing
                            : addButtonX + mAddButton.getMeasuredWidth() + mButtonSpacing;

                for (int i = mButtonsCount - 1; i >= 0; i--) {
                    final View child = getChildAt(i);

                    if (child == mAddButton || child.getVisibility() == GONE) {
                        continue;
                    }

                    AnimatorProxy proxy = AnimatorProxy.wrap(child);
                    int childX = expandLeft ? nextX - child.getMeasuredWidth() : nextX;
                    int childY = addButtonTop +
                                 (mAddButton.getMeasuredHeight() - child.getMeasuredHeight()) / 2;

                    float collapsedTranslation = addButtonX - childX;
                    float expandedTranslation = 0f;


                    proxy.setTranslationX(mExpanded ? expandedTranslation : collapsedTranslation);
                    proxy.setAlpha(mExpanded ? 1f : 0f);
                    child.layout(
                            childX, childY, childX + child.getMeasuredWidth(),
                            childY + child.getMeasuredHeight());

                    LayoutParams params = (LayoutParams) child.getLayoutParams();
                    params.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation);
                    params.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation);
                    params.setAnimationsTarget(proxy);

                    nextX = expandLeft
                            ? childX - mButtonSpacing
                            : childX + child.getMeasuredWidth() + mButtonSpacing;
                }

                break;
        }
    }


    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams(super.generateDefaultLayoutParams());
    }


    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs)
    {
        return new LayoutParams(super.generateLayoutParams(attrs));
    }


    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p)
    {
        return new LayoutParams(super.generateLayoutParams(p));
    }


    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p)
    {
        return super.checkLayoutParams(p);
    }


    private static Interpolator sExpandInterpolator      = new OvershootInterpolator();
    private static Interpolator sCollapseInterpolator    = new DecelerateInterpolator(3f);
    private static Interpolator sAlphaExpandInterpolator = new DecelerateInterpolator();


    private class LayoutParams
            extends ViewGroup.LayoutParams
    {

        private ObjectAnimator mExpandDir     = new ObjectAnimator();
        private ObjectAnimator mExpandAlpha   = new ObjectAnimator();
        private ObjectAnimator mCollapseDir   = new ObjectAnimator();
        private ObjectAnimator mCollapseAlpha = new ObjectAnimator();
        private boolean animationsSetToPlay;


        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);

            mExpandDir.setInterpolator(sExpandInterpolator);
            mExpandAlpha.setInterpolator(sAlphaExpandInterpolator);
            mCollapseDir.setInterpolator(sCollapseInterpolator);
            mCollapseAlpha.setInterpolator(sCollapseInterpolator);

            mCollapseAlpha.setPropertyName("alpha");
            mCollapseAlpha.setFloatValues(1f, 0f);

            mExpandAlpha.setPropertyName("alpha");
            mExpandAlpha.setFloatValues(0f, 1f);

            switch (mExpandDirection) {
                case EXPAND_UP:
                case EXPAND_DOWN:
                    mCollapseDir.setPropertyName("translationY");
                    mExpandDir.setPropertyName("translationY");
                    break;
                case EXPAND_LEFT:
                case EXPAND_RIGHT:
                    mCollapseDir.setPropertyName("translationX");
                    mExpandDir.setPropertyName("translationX");
                    break;
            }
        }


        public void setAnimationsTarget(AnimatorProxy view)
        {
            mCollapseAlpha.setTarget(view);
            mCollapseDir.setTarget(view);
            mExpandAlpha.setTarget(view);
            mExpandDir.setTarget(view);

            // Now that the animations have targets, set them to be played
            if (!animationsSetToPlay) {
                mCollapseAnimation.play(mCollapseAlpha);
                mCollapseAnimation.play(mCollapseDir);
                mExpandAnimation.play(mExpandAlpha);
                mExpandAnimation.play(mExpandDir);
                animationsSetToPlay = true;
            }
        }
    }


    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();

        bringChildToFront(mAddButton);
        mButtonsCount = getChildCount();

        if (mLabelsStyle != 0) {
            createLabels();
        }
    }


    private void createLabels()
    {
        Context context = new ContextThemeWrapper(getContext(), mLabelsStyle);

        for (int i = 0; i < mButtonsCount; i++) {
            FloatingActionButton button = (FloatingActionButton) getChildAt(i);
            String title = button.getTitle();

            if (button == mAddButton || title == null ||
                button.getTag(R.id.fab_label) != null) {
                continue;
            }

            TextView label = new TextView(context);
            label.setText(button.getTitle());
            addView(label);

            button.setTag(R.id.fab_label, label);
        }
    }


    public void collapse()
    {
        if (mExpanded) {
            mExpanded = false;
            mCollapseAnimation.start();
            mExpandAnimation.cancel();

            if (mListener != null) {
                mListener.onMenuCollapsed();
            }
        }
    }


    public void toggle()
    {
        if (mExpanded) {
            collapse();
        } else {
            expand();
        }
    }


    public void expand()
    {
        if (!mExpanded) {
            mExpanded = true;
            mCollapseAnimation.cancel();
            mExpandAnimation.start();

            if (mListener != null) {
                mListener.onMenuExpanded();
            }
        }
    }


    public boolean isExpanded()
    {
        return mExpanded;
    }


    @Override
    public Parcelable onSaveInstanceState()
    {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.mExpanded = mExpanded;

        return savedState;
    }


    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mExpanded = savedState.mExpanded;

            if (mRotatingDrawable != null) {
                mRotatingDrawable.setRotation(
                        mExpanded ? EXPANDED_PLUS_ROTATION : COLLAPSED_PLUS_ROTATION);
            }

            super.onRestoreInstanceState(savedState.getSuperState());
        } else {
            super.onRestoreInstanceState(state);
        }
    }


    public static class SavedState
            extends BaseSavedState
    {
        public boolean mExpanded;


        public SavedState(Parcelable parcel)
        {
            super(parcel);
        }


        private SavedState(Parcel in)
        {
            super(in);
            mExpanded = in.readInt() == 1;
        }


        @Override
        public void writeToParcel(
                @NonNull
                Parcel out,
                int flags)
        {
            super.writeToParcel(out, flags);
            out.writeInt(mExpanded ? 1 : 0);
        }


        public static final Creator<SavedState> CREATOR = new Creator<SavedState>()
        {

            @Override
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }


            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }
}
