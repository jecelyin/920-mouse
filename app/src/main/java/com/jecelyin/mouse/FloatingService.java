package com.jecelyin.mouse;

import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

/**
 * @author:Jack Tony
 * <p/>
 * 重要：注意要申请权限！！！！
 * <!-- 悬浮窗的权限 -->
 * <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
 * @tips :思路：
 * 1.获得一个windowManager类
 * 2.通过wmParams设置好windows的各种参数
 * 3.获得一个视图的容器，找到悬浮窗视图的父控件，比如linearLayout
 * 4.将父控件添加到WindowManager中去
 * 5.通过这个父控件找到要显示的悬浮窗图标，并进行拖动或点击事件的设置
 * @date :2014-9-25
 */
public class FloatingService extends Service {
    /**
     * 悬浮窗控件
     */
    ViewGroup mLayout;
    ImageView mfloatingIv;
    /**
     * 悬浮窗的布局
     */
    LayoutParams wmParams;
    LayoutInflater inflater;

    private Dialog mDialog;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initWindow();//设置窗口的参数
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        initFloating();//设置悬浮窗图标
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            // 移除悬浮窗口
            mDialog.dismiss();
            mDialog = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////

    /**
     * 初始化windowManager
     */
    private void initWindow() {
//		mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        wmParams = getParams();//设置好悬浮窗的参数
        // 悬浮窗默认显示以左上角为起始坐标
        wmParams.gravity = Gravity.START | Gravity.TOP;
        //悬浮窗的开始位置，因为设置的是从左上角开始，所以屏幕左上角是x=0;y=0
        wmParams.x = 0;
        wmParams.y = 350;
//		//得到容器，通过这个inflater来获得悬浮窗控件
//		inflater = LayoutInflater.from(getApplication());
//		// 获取浮动窗口视图所在布局
//		mfloatingIv = (ImageView) inflater.inflate(R.layout.floating_layout, null);
//		// 添加悬浮窗的视图
//		mWindowManager.addView(mfloatingIv, wmParams);

        mDialog = new FloatDialog(this, R.style.TransparentDialog);
        mDialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ERROR));
        mDialog.getWindow().setAttributes(wmParams);

        //得到容器，通过这个inflater来获得悬浮窗控件
        inflater = LayoutInflater.from(getApplication());
        // 获取浮动窗口视图所在布局
        mLayout = (ViewGroup) inflater.inflate(R.layout.floating_layout, null);
        mfloatingIv = (ImageView) mLayout.findViewById(R.id.floating_imageView);

        // 添加悬浮窗的视图
        mDialog.setContentView(mLayout);

        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
    }

    /**
     * 对windowManager进行设置
     *
     * @return
     */
    public LayoutParams getParams() {
        wmParams = new LayoutParams();
        //设置window type 下面变量2002是在屏幕区域显示，2003则可以显示在状态栏之上
        //wmParams.type = LayoutParams.TYPE_PHONE;
        //wmParams.type = LayoutParams.TYPE_SYSTEM_ALERT;
        wmParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.TRANSPARENT;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        //wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        //设置可以显示在状态栏上
        // 如果设置了WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE，弹出的View收不到Back键的事件
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCH_MODAL |
                LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        wmParams.dimAmount = 0;

        //设置悬浮窗口长宽数据
        wmParams.width = LayoutParams.WRAP_CONTENT;
        wmParams.height = LayoutParams.WRAP_CONTENT;

        return wmParams;
    }

    static class FloatDialog extends Dialog {
        private final Intent intent;
        //开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
        private int mTouchStartX, mTouchStartY, mTouchCurrentX, mTouchCurrentY;
        //开始时的坐标和结束时的坐标（相对于自身控件的坐标）
        private int mStartX, mStartY, mStopX, mStopY;

        public FloatDialog(Context context, int themeResId) {
            super(context, themeResId);
            intent = new Intent(getContext(), DialogFloatingService.class);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    mStartX = (int) event.getX();
                    mStartY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mTouchCurrentX = (int) event.getRawX();
                    mTouchCurrentY = (int) event.getRawY();
                    LayoutParams lp = getWindow().getAttributes();
                    lp.x += mTouchCurrentX - mTouchStartX;
                    lp.y += mTouchCurrentY - mTouchStartY;
                    getWindow().setAttributes(lp);

                    mTouchStartX = mTouchCurrentX;
                    mTouchStartY = mTouchCurrentY;
                    break;
                case MotionEvent.ACTION_UP:
                    mStopX = (int) event.getX();
                    mStopY = (int) event.getY();
                    //System.out.println("|X| = "+ Math.abs(mStartX - mStopX));
                    //System.out.println("|Y| = "+ Math.abs(mStartY - mStopY));
                    if (Math.abs(mStartX - mStopX) == 0 && Math.abs(mStartY - mStopY) == 0) {
                        if (DialogFloatingService.isRunning) {
                            getContext().stopService(intent);
                        } else {
                            getContext().startService(intent);
                        }

                    }
                    break;
            }
            return super.onTouchEvent(event);
        }
    }

}
