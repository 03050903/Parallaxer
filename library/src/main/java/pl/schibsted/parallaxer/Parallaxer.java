/*
 * Copyright (C) 2013 Manuel Peinado
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
package pl.schibsted.parallaxer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import pl.schibsted.parallaxer.views.ObservableScrollView;
import pl.schibsted.parallaxer.views.ObservableWebViewWithHeader;
import pl.schibsted.parallaxer.views.OnScrollChangedCallback;

@SuppressWarnings("unchecked")
public class Parallaxer {
    protected static final String TAG = "Parallaxer";

    private Toolbar toolbar;

    private Drawable actionBarBackgroundDrawable;
    private FrameLayout headerContainer;
    private int actionBarBackgroundResId;
    private int headerLayoutResId;
    private View headerView;
    private int headerOverlayLayoutResId;
    private View headerOverlayView;
    private int contentLayoutResId;
    private View contentView;
    private LayoutInflater inflater;
    private boolean lightActionBar;
    private int lastDampedScroll;
    private int lastHeaderHeight = -1;
    private boolean firstGlobalLayoutPerformed;
    private FrameLayout marginView;
    private View listViewBackgroundView;
    private boolean allowHeaderTouchEvents = true;
    private boolean useParallax = true;

    public final Parallaxer actionBarBackground(int drawableResId) {
        actionBarBackgroundResId = drawableResId;
        return this;
    }

    public final Parallaxer actionBarBackground(Drawable drawable) {
        actionBarBackgroundDrawable = drawable;
        return this;
    }

    public final Parallaxer headerLayout(int layoutResId) {
        headerLayoutResId = layoutResId;
        return this;
    }

    public final Parallaxer headerView(View view) {
        headerView = view;
        return this;
    }

    public final Parallaxer headerOverlayLayout(int layoutResId) {
        headerOverlayLayoutResId = layoutResId;
        return this;
    }

    public final Parallaxer headerOverlayView(View view) {
        headerOverlayView = view;
        return this;
    }

    public final Parallaxer contentLayout(int layoutResId) {
        contentLayoutResId = layoutResId;
        return this;
    }

    public final Parallaxer contentView(View view) {
        contentView = view;
        return this;
    }

    public final Parallaxer lightActionBar(boolean value) {
        lightActionBar = value;
        return this;
    }

    public final Parallaxer parallax(boolean value) {
        useParallax = value;
        return this;
    }

    public final Parallaxer allowHeaderTouchEvents(boolean value) {
        allowHeaderTouchEvents = value;
        return this;
    }

    public final View createView(Context context) {
        return createView(LayoutInflater.from(context));
    }

    public final View createView(LayoutInflater inflater) {
        //
        // Prepare everything

        this.inflater = inflater;
        if (contentView == null) {
            contentView = inflater.inflate(contentLayoutResId, null);
        }
        if (headerView == null) {
            headerView = inflater.inflate(headerLayoutResId, null, false);
        }

        //
        // See if we are in a ListView, WebView or ScrollView scenario

        ListView listView = (ListView) contentView.findViewById(android.R.id.list);
        View root;
        if (listView != null) {
            root = createListView(listView);
        } else if (contentView instanceof ObservableWebViewWithHeader) {
            root = createWebView();
        } else {
            root = createScrollView();
        }

        if (headerOverlayView == null && headerOverlayLayoutResId != 0) {
            headerOverlayView = inflater.inflate(headerOverlayLayoutResId, marginView, false);
        }
        if (headerOverlayView != null) {
            marginView.addView(headerOverlayView);
        }

        // Use measured height here as an estimate of the header height, later on after the layout is complete 
        // we'll use the actual height
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(LayoutParams.MATCH_PARENT, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.EXACTLY);
        headerView.measure(widthMeasureSpec, heightMeasureSpec);
        updateHeaderHeight(headerView.getMeasuredHeight());

        root.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int headerHeight = headerContainer.getHeight();
                if (!firstGlobalLayoutPerformed && headerHeight != 0) {
                    updateHeaderHeight(headerHeight);
                    firstGlobalLayoutPerformed = true;
                }
            }
        });
        return root;
    }

    public void init(Toolbar toolbar) {
        if (actionBarBackgroundDrawable == null) {
            actionBarBackgroundDrawable = toolbar.getResources().getDrawable(actionBarBackgroundResId);
        }
        toolbar.setBackground(actionBarBackgroundDrawable);
        actionBarBackgroundDrawable.setAlpha(0);
    }

    private View createWebView() {
        ViewGroup webViewContainer = (ViewGroup) inflater.inflate(R.layout.parallaxer__webview_container, null);

        ObservableWebViewWithHeader webView = (ObservableWebViewWithHeader) contentView;
        webView.setOnScrollChangedCallback(mOnScrollChangedListener);

        webViewContainer.addView(webView);

        headerContainer = (FrameLayout) webViewContainer.findViewById(R.id.fab__header_container);
        initializeGradient(headerContainer);
        headerContainer.addView(headerView, 0);

        marginView = new FrameLayout(webView.getContext());
        marginView.setBackgroundColor(Color.TRANSPARENT);
        marginView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        webView.addView(marginView);

        if (allowHeaderTouchEvents) {
            setMarginViewTouchListener();
        }

        return webViewContainer;
    }

    private View createScrollView() {
        ViewGroup scrollViewContainer = (ViewGroup) inflater.inflate(R.layout.parallaxer__scrollview_container, null);

        ObservableScrollView scrollView = (ObservableScrollView) scrollViewContainer.findViewById(R.id.fab__scroll_view);
        scrollView.setOnScrollChangedCallback(mOnScrollChangedListener);

        ViewGroup contentContainer = (ViewGroup) scrollViewContainer.findViewById(R.id.fab__container);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        contentView.setLayoutParams(layoutParams);
        contentContainer.addView(contentView);
        headerContainer = (FrameLayout) scrollViewContainer.findViewById(R.id.fab__header_container);
        initializeGradient(headerContainer);
        headerContainer.addView(headerView, 0);
        marginView = (FrameLayout) contentContainer.findViewById(R.id.fab__content_top_margin);

        if (allowHeaderTouchEvents) {
            setMarginViewTouchListener();
        }

        return scrollViewContainer;
    }

    private OnScrollChangedCallback mOnScrollChangedListener = new OnScrollChangedCallback() {
        public void onScroll(int l, int t) {
            onNewScroll(t);
        }
    };

    private View createListView(ListView listView) {
        ViewGroup contentContainer = (ViewGroup) inflater.inflate(R.layout.parallaxer__listview_container, null);
        contentContainer.addView(contentView);

        headerContainer = (FrameLayout) contentContainer.findViewById(R.id.fab__header_container);
        initializeGradient(headerContainer);
        headerContainer.addView(headerView, 0);

        marginView = new FrameLayout(listView.getContext());
        marginView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, 0));
        listView.addHeaderView(marginView, null, false);

        // Make the background as high as the screen so that it fills regardless of the amount of scroll. 
        listViewBackgroundView = contentContainer.findViewById(R.id.fab__listview_background);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) listViewBackgroundView.getLayoutParams();
        params.height = Utils.getDisplayHeight(listView.getContext());
        listViewBackgroundView.setLayoutParams(params);

        listView.setOnScrollListener(mOnScrollListener);

        if (allowHeaderTouchEvents) {
            setMarginViewTouchListener();
        }

        return contentContainer;
    }

    private OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            View topChild = view.getChildAt(0);
            if (topChild == null) {
                onNewScroll(0);
            } else if (topChild != marginView) {
                onNewScroll(headerContainer.getHeight());
            } else {
                onNewScroll(-topChild.getTop());
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    };
    private int mLastScrollPosition;

    private void onNewScroll(int scrollPosition) {
        if (toolbar == null) return;

        int currentHeaderHeight = headerContainer.getHeight();
        if (currentHeaderHeight != lastHeaderHeight) {
            updateHeaderHeight(currentHeaderHeight);
        }

        int headerHeight = currentHeaderHeight - toolbar.getHeight();
        float ratio = (float) Math.min(Math.max(scrollPosition, 0), headerHeight) / headerHeight;
        int newAlpha = (int) (ratio * 255);
        actionBarBackgroundDrawable.setAlpha(newAlpha);

        addParallaxEffect(scrollPosition);
    }

    private void addParallaxEffect(int scrollPosition) {
        float damping = useParallax ? 0.5f : 1.0f;
        int dampedScroll = (int) (scrollPosition * damping);
        int offset = lastDampedScroll - dampedScroll;
        headerContainer.offsetTopAndBottom(offset);

        if (listViewBackgroundView != null) {
            offset = mLastScrollPosition - scrollPosition;
            listViewBackgroundView.offsetTopAndBottom(offset);
        }

        if (firstGlobalLayoutPerformed) {
            mLastScrollPosition = scrollPosition;
            lastDampedScroll = dampedScroll;
        }
    }

    private void updateHeaderHeight(int headerHeight) {
        LayoutParams params = (LayoutParams) marginView.getLayoutParams();
        params.height = headerHeight;
        marginView.setLayoutParams(params);
        if (listViewBackgroundView != null) {
            FrameLayout.LayoutParams params2 = (FrameLayout.LayoutParams) listViewBackgroundView.getLayoutParams();
            params2.topMargin = headerHeight;
            listViewBackgroundView.setLayoutParams(params2);
        }
        lastHeaderHeight = headerHeight;
    }

    private void initializeGradient(ViewGroup headerContainer) {
        View gradientView = headerContainer.findViewById(R.id.fab__gradient);
        int gradient = R.drawable.parallaxer__gradient;
        if (lightActionBar) {
            gradient = R.drawable.parallaxer__gradient_light;
        }
        gradientView.setBackgroundResource(gradient);
    }

    private void setMarginViewTouchListener() {
        marginView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return headerView.dispatchTouchEvent(event);
            }
        });
    }
}
