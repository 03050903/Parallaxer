package pl.schibsted.parallaxer.views;


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * @author Cyril Mottier with modifications from Manuel Peinado
 */
public class ObservableScrollView extends ScrollView implements ObservableScrollable {

    private OnScrollChangedCallback onScrollChangedListener;
    private OnScrollableMotionEventCallback onScrollableMotionEventListener;

    public ObservableScrollView(Context context) {
        super(context);
    }

    public ObservableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObservableScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (onScrollChangedListener != null) {
            onScrollChangedListener.onScroll(l, t);
        }
    }

    @Override
    public void setOnScrollChangedCallback(OnScrollChangedCallback callback) {
        onScrollChangedListener = callback;
    }

    public void setOnScrollableMotionEventListener(OnScrollableMotionEventCallback onScrollableMotionEventListener) {
        this.onScrollableMotionEventListener = onScrollableMotionEventListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (onScrollableMotionEventListener != null) {
            onScrollableMotionEventListener.onMotionEvent(event);
        }
        return super.onTouchEvent(event);
    }
}