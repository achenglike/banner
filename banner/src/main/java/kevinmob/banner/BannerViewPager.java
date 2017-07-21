package kevinmob.banner;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import java.lang.reflect.Field;

/**
 * 创建日期：2017/7/18.
 *
 * @author yangjinhai
 */

public class BannerViewPager extends FrameLayout implements ViewPager.OnPageChangeListener{
    private static final int DELAY_MILLIS = 3000;
    private ViewPager viewPager;
    private LinearLayout pageIndexContainer;
    private GestureDetector gestureDetector;
    boolean isScroll;
    boolean isFastScroll;
    private BannerAdapter adapter;
    private ImageView currentIndex;
    private IBannerItemClick bannerItemClick;
    private Runnable runnable;
    private int leftMargin;
    private int rightMargin;
    private int itemMargin;
    private int pageIndexDrawableResId;
    private int pageIndexGravity;
    private boolean isShowAround;
    public BannerViewPager(Context context) {
        super(context);
        initBannerView();
    }

    public BannerViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(context, attrs);
        initBannerView();
    }

    public BannerViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAttrs(context, attrs);
        initBannerView();
    }

    private void parseAttrs(Context context, AttributeSet attrs){
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BannerViewPager);
        leftMargin = ta.getDimensionPixelSize(R.styleable.BannerViewPager_marginLeft, 0);
        rightMargin = ta.getDimensionPixelSize(R.styleable.BannerViewPager_marginRight, 0);
        itemMargin = ta.getDimensionPixelSize(R.styleable.BannerViewPager_item_margin, 0);
        isShowAround = ta.getBoolean(R.styleable.BannerViewPager_around_visible, true);
        pageIndexDrawableResId = ta.getResourceId(R.styleable.BannerViewPager_point_drawable, 0);
        pageIndexGravity = ta.getInt(R.styleable.BannerViewPager_point_gravity, 4);
        ta.recycle();
    }

    public void setBannerAdapter(BannerAdapter adapter){
        this.adapter = adapter;
        viewPager.setAdapter(adapter);
        fillPageIndex();
        int firstPosition = Integer.MAX_VALUE / 2 - ((Integer.MAX_VALUE / 2) % adapter.getRealCount());
        viewPager.setCurrentItem(firstPosition);
        viewPager.setOffscreenPageLimit(2);
    }

    public void setBannerItemClick(IBannerItemClick itemClick){
        bannerItemClick = itemClick;
    }

    private void fillPageIndex() {
        if (pageIndexContainer.getChildCount() > 0) {
            pageIndexContainer.removeAllViews();
        }
        int size = adapter.getRealCount();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(new ViewGroup.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        int margin = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                getResources().getDisplayMetrics()) + .5f);
        for (int i = 0; i < size; ++i) {
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(new LayoutParams(6, 6));
            imageView.setBackgroundResource(pageIndexDrawableResId == 0 ? R.drawable.point_selecter : pageIndexDrawableResId);
            if (i == 0) {
                currentIndex = imageView;
                imageView.setSelected(true);
            }
            layoutParams.leftMargin = margin;
            layoutParams.rightMargin = margin;
            pageIndexContainer.addView(imageView, layoutParams);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    private void initBannerView(){
        if(isShowAround){
            setClipChildren(false);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
        viewPager = new ViewPager(getContext());

        FrameLayout.LayoutParams vpLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        vpLayoutParams.leftMargin = leftMargin;
        vpLayoutParams.rightMargin = rightMargin;
        viewPager.setPageMargin(itemMargin);

        viewPager.addOnPageChangeListener(BannerViewPager.this);
        viewPager.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        isScroll = true;
                        removeCallbacks(runnable);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE:
                        isScroll = false;
                        isFastScroll = true;
                        /*
                         * 左右滑动未翻页的情况下需要在此触发自动翻页
                         */
                        postDelayed(runnable, DELAY_MILLIS);
                        break;
                }
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });
        addView(viewPager, vpLayoutParams);

        pageIndexContainer = new LinearLayout(getContext());
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()) + .5f);
        switch (pageIndexGravity){
            case 1:
                layoutParams.gravity = Gravity.BOTTOM | Gravity.START;
                layoutParams.leftMargin = margin + leftMargin;
                break;
            case 2:
                layoutParams.gravity = Gravity.BOTTOM | Gravity.END;
                layoutParams.rightMargin = margin + rightMargin;
                break;
            case 4:
                layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                break;
        }

        layoutParams.bottomMargin = margin;
        addView(pageIndexContainer, layoutParams);

        gestureDetector = new GestureDetector(getContext(), new BannerGestureListener());

        runnable = new Runnable(){
            @Override
            public void run() {
                if(!isScroll)
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
            }
        };

        new CustomScroller(getContext(), new AccelerateDecelerateInterpolator()).control(viewPager);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {
        changePageIndex(position);
        removeCallbacks(runnable);
        isFastScroll = false;
        postDelayed(runnable, DELAY_MILLIS);
    }

    private void changePageIndex(int position) {
        int realPosition = adapter.getRealPosition(position);
        if(currentIndex != null){
            currentIndex.setSelected(false);
        }
        currentIndex = (ImageView) pageIndexContainer.getChildAt(realPosition);
        currentIndex.setSelected(true);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public interface IBannerItemClick{
        void onClick(IBannerItem data);
    }

    private final class BannerGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return Math.abs(distanceY) < Math.abs(distanceX);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            int currentPosition = viewPager.getCurrentItem();
            if (null != bannerItemClick)
                bannerItemClick.onClick(adapter.getItem(currentPosition));
            return true;
        }
    }

    class CustomScroller extends Scroller {
        private static  final int DURATION = 1000;
        private static final int DEFAULT_DURATION = 250;

        CustomScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, isFastScroll ? DEFAULT_DURATION : DURATION);
            if(isFastScroll) isFastScroll = false;
        }

        void control(ViewPager viewPager) {
            try {
                Field mField = ViewPager.class.getDeclaredField("mScroller");
                mField.setAccessible(true);
                mField.set(viewPager, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
