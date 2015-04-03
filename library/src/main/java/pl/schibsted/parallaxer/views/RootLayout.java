package pl.schibsted.parallaxer.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import pl.schibsted.parallaxer.R;


public class RootLayout extends FrameLayout {

    private View headerContainer;
    private View listViewBackground;
    private boolean initialized = false;

    public RootLayout(Context context) {
        super(context);
    }

    public RootLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RootLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //at first find headerViewContainer and listViewBackground
        if (headerContainer == null)
            headerContainer = findViewById(R.id.fab__header_container);
        if (listViewBackground == null)
            listViewBackground = findViewById(R.id.fab__listview_background);

        //if there's no headerViewContainer then fallback to standard FrameLayout
        if (headerContainer == null) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }

        if (!initialized) {
            super.onLayout(changed, left, top, right, bottom);
            //if listViewBackground not exists or listViewBackground exists
            //and its top is at headercontainer height then view is initialized
            if (listViewBackground == null || listViewBackground.getTop() == headerContainer.getHeight())
                initialized = true;
            return;
        }

        //get last header and listViewBackground position
        int headerTopPrevious = headerContainer.getTop();
        int listViewBackgroundTopPrevious = listViewBackground != null ? listViewBackground.getTop() : 0;

        //relayout
        super.onLayout(changed, left, top, right, bottom);

        //revert header top position
        int headerTopCurrent = headerContainer.getTop();
        if (headerTopCurrent != headerTopPrevious) {
            headerContainer.offsetTopAndBottom(headerTopPrevious - headerTopCurrent);
        }
        //revert listViewBackground top position
        int listViewBackgroundTopCurrent = listViewBackground != null ? listViewBackground.getTop() : 0;
        if (listViewBackgroundTopCurrent != listViewBackgroundTopPrevious) {
            listViewBackground.offsetTopAndBottom(listViewBackgroundTopPrevious - listViewBackgroundTopCurrent);
        }
    }

}