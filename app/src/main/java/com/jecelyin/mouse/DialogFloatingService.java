package com.jecelyin.mouse;

import android.app.Dialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.DataOutputStream;
import java.io.IOException;

public class DialogFloatingService extends Service implements View.OnTouchListener {
    //开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
    private int mTouchStartX, mTouchStartY, mTouchCurrentX, mTouchCurrentY;
    //开始时的坐标和结束时的坐标（相对于自身控件的坐标）
    private int mStartX, mStartY, mStopX, mStopY;

    Dialog mDialog;

    LayoutInflater inflater;

    public static boolean isRunning = false;
    private View mChildLayout;
    private int mLayoutWidth;
    private float mLayoutHeight;
    private int mHandlerWidth;
    private float mHandlerHeight;
    private View mCursorView;
    private View mBodyLayout;
    private int mBodyHeight;
    private int movingCount;
    private int mX;
    private int mY;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initWindow();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 初始化
     */
    private void initWindow() {
        if(FloatingService.process == null) {
            try {
                FloatingService.process = Runtime.getRuntime().exec("su");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mDialog = new Dialog(this, R.style.TransparentDialog);
        mDialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));

        //得到容器，通过这个inflater来获得悬浮窗控件
        inflater = LayoutInflater.from(getApplication());
        // 获取浮动窗口视图所在布局
        mBodyLayout = inflater.inflate(R.layout.dialog_layout, null);
        mChildLayout = mBodyLayout.findViewById(R.id.layout);

        mCursorView = mBodyLayout.findViewById(R.id.cursorImageView);

        ImageView iv = (ImageView) mBodyLayout.findViewById(R.id.imageView);
        iv.setOnTouchListener(this);
        iv.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mDialog.dismiss();
                mDialog = null;
                stopSelf();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FloatingService.process = Runtime.getRuntime().exec("su");
                            DataOutputStream os = new DataOutputStream(FloatingService.process.getOutputStream());
                            int statusBarHeight =  getStatusBarHeight();
                            Log.d(DialogFloatingService.class.getName(), "statusBarHeight=" + statusBarHeight);
                            os.writeBytes(String.format("input touchscreen tap %d %d\n", mX, mY + statusBarHeight));
                            os.writeBytes("exit\n");
                            os.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
//                mBodyLayout.setVisibility(View.VISIBLE);
            }
        });

        // 添加悬浮窗的视图
        mDialog.setContentView(mBodyLayout);

        Window win = mDialog.getWindow();
        win.setGravity(Gravity.BOTTOM);
        win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mDialog.setCanceledOnTouchOutside(true);
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isRunning = false;
                stopSelf();
                mDialog = null;
            }
        });
        mDialog.show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartX = (int) event.getRawX();
                mTouchStartY = (int) event.getRawY();
                mStartX = (int) event.getX();
                mStartY = (int) event.getY();
                mLayoutWidth = mChildLayout.getWidth();
                mLayoutHeight = mChildLayout.getHeight();
                mHandlerWidth = v.getWidth();
                mHandlerHeight = v.getHeight();
                mBodyHeight = mBodyLayout.getHeight();
                movingCount = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                mTouchCurrentX = (int) event.getRawX();
                mTouchCurrentY = (int) event.getRawY();
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
                RelativeLayout.LayoutParams lp2 = (RelativeLayout.LayoutParams) mCursorView.getLayoutParams();

                lp.leftMargin += mTouchCurrentX - mTouchStartX;
                lp.topMargin += mTouchCurrentY - mTouchStartY;

                if(lp.leftMargin < 0)
                    lp.leftMargin = 0;
                if(lp.topMargin < 0)
                    lp.topMargin = 0;
                if(lp.topMargin + mHandlerHeight > mLayoutHeight)
                    lp.topMargin = (int)(mLayoutHeight - mHandlerHeight);
                if(lp.leftMargin + mHandlerWidth > mLayoutWidth)
                    lp.leftMargin = mLayoutWidth - mHandlerWidth;

                v.setLayoutParams(lp);

                lp2.removeRule(RelativeLayout.CENTER_IN_PARENT);
                mX = lp2.leftMargin = lp.leftMargin;
                mY = lp2.topMargin = (int)(mBodyHeight * (lp.topMargin / mLayoutHeight));

//                Log.d("moving", String.format("%d * %d / %f = %d", mBodyHeight, lp.topMargin, mLayoutHeight, lp2.topMargin));

                mCursorView.setLayoutParams(lp2);

                mTouchStartX = mTouchCurrentX;
                mTouchStartY = mTouchCurrentY;
                movingCount++;
                break;
            case MotionEvent.ACTION_UP:
                mStopX = (int) event.getX();
                mStopY = (int) event.getY();
                //System.out.println("|X| = "+ Math.abs(mStartX - mStopX));
                //System.out.println("|Y| = "+ Math.abs(mStartY - mStopY));
//                return !(Math.abs(mStartX - mStopX) >= 1 || Math.abs(mStartY - mStopY) >= 1);
                return movingCount > 3;
        }
        return false;
    }
}
