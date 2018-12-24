package org.lasque.tusdkvideodemo.views.editor.playview.rangeselect;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 颜色矩形父控件
 * @author MirsFang
 */
public class TuSdkMovieColorGroupView extends FrameLayout {
    private static final String TAG = "ColorGroupView";
    /**
     * 颜色集合
     **/
    private List<TuSdkMovieColorRectView> mColorRectList = new ArrayList<>();
    private OnSelectColorRectListener onSelectColorRectListener;

    /** 选择一个ColorRect **/
    public interface OnSelectColorRectListener{
        void onSelectColorRect(TuSdkMovieColorRectView rectView);
    }

    public TuSdkMovieColorGroupView(@NonNull Context context) {
        super(context);
    }

    public TuSdkMovieColorGroupView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        for (int i = 0; i < getChildCount(); i++) {
            TuSdkMovieColorRectView rectView = (TuSdkMovieColorRectView) getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) rectView.getLayoutParams();
            layoutParams.height = height;
            rectView.setLayoutParams(layoutParams);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); i++) {
            TuSdkMovieColorRectView rectView = (TuSdkMovieColorRectView) getChildAt(i);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
            int startPosition = (int) (getMeasuredWidth() * rectView.getStartPercent());
            if (rectView.getDrawDirection() == 0) {
                rectView.layout(startPosition, top, startPosition + rectView.getMeasuredWidth(), bottom);
            } else {
                rectView.layout(startPosition - rectView.getMeasuredWidth(), top, startPosition, bottom);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                float touchX = event.getRawX();
                float touchY = event.getRawY();
                TuSdkMovieColorRectView rectView = getPointColorRect(touchX,touchY);
                if(onSelectColorRectListener != null)onSelectColorRectListener.onSelectColorRect(rectView);
                break;
        }
        return super.onTouchEvent(event);
    }

    public TuSdkMovieColorRectView getPointColorRect(float pointX,float pointY){
        for (TuSdkMovieColorRectView rectView : mColorRectList) {
            if(isTouchPointInView(rectView,pointX,pointY)){
                return rectView;
            }
        }
        return null;
    }

    private boolean isTouchPointInView(View view, float x, float y) {
        if (view == null) {
            return false;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();
        if (y >= top && y <= bottom && x >= left
                && x <= right) {
            return true;
        }
        return false;
    }

    /** 设置颜色选择监听 **/
    public void setOnSelectColorRectListener(OnSelectColorRectListener onSelectColorRectListener){
        this.onSelectColorRectListener = onSelectColorRectListener;
    }

    /**
     * 添加颜色区块View
     **/
    public void addColorRect(TuSdkMovieColorRectView rectView) {
        mColorRectList.add(rectView);
        LayoutParams layoutParams = (LayoutParams) rectView.getLayoutParams();
        if(layoutParams != null)
        this.addView(rectView,layoutParams.width,rectView.getHeight());
        else
            this.addView(rectView,rectView.getWidth(),rectView.getHeight());
    }

    /**
     * 移除一个颜色区块
     **/
    public TuSdkMovieColorRectView removeColorRect(int index) {
        if (index >= mColorRectList.size() || mColorRectList.size() == 0) {
            Log.e(TAG, "Invalid remove index");
            return null;
        }
        this.removeViewAt(index);
        return mColorRectList.remove(index);
    }


    public void removeColorRect(TuSdkMovieColorRectView rectView) {
        if (rectView == null|| mColorRectList.size() == 0) {
            Log.e(TAG, "Invalid remove index");
            return;
        }
        mColorRectList.remove(rectView);
        removeView(rectView);
    }


    /**
     * 移除最后一个颜色区块
     **/
    public TuSdkMovieColorRectView removeLastColorRect() {
        return this.removeColorRect(mColorRectList.size() - 1);
    }

    /**
     * 获取最后一个颜色画块
     * @return
     */
    public TuSdkMovieColorRectView getLastColorRect(){
        if(mColorRectList == null || mColorRectList.size() == 0)return null;
        return mColorRectList.get(mColorRectList.size() -1 );
    }

    /**
     * 更新最后一个色块的颜色
     **/
    public void updateLastWidth(int distance) {
        if (mColorRectList.size() == 0) return;
        mColorRectList.get(mColorRectList.size() - 1).setWidth(Math.abs(distance));
    }

    /** 是否存在色块 **/
    public boolean isContain(TuSdkMovieColorRectView rectView){
        if(rectView == null || !mColorRectList.contains(rectView)) return false;
        return mColorRectList.contains(rectView);
    }

    public void clearAllColorRect() {
        removeAllViews();
        mColorRectList.clear();
    }
}
