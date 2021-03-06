/*******************************************************************
 * Company:     Fuzhou Rockchip Electronics Co., Ltd
 * Filename:    MouseManager.java
 * Description:   
 * @author:     fxw@rock-chips.com
 * Create at:   下午10:25:14
 * 
 * Modification History:  
 * Date         Author      Version     Description  
 * ------------------------------------------------------------------  
 * 2011-11-29      xwf         1.0         create
 *******************************************************************/
package com.android.server.wm.remotecontrol;


import java.lang.reflect.Method;

import com.android.server.wm.WindowManagerService;

//import com.android.rockchip.common.util.ReflectUtils;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;

public class MouseManager {
	
	private Context mContext;
	private View mMouseView;
	private int mScreenWidth;
	private int mScreenHeight;
	private float mMouseX;
	private float mMouseY;
	private float mLastX;
	private float mLastY;
	private static final float mScale = 2.0f;
	
	private boolean isMultPointer = false;

	private Environment mEnvironment;
	
	WindowManagerService mService;
	
	public MouseManager(Context context,WindowManagerService service){
		mContext = context;
		mEnvironment = Environment.getInstance();
		mEnvironment.setScreenParameter(mContext);
		mScreenWidth = mEnvironment.getScreenWidth();
		mScreenHeight = mEnvironment.getScreenHeight();
		mMouseX = mScreenWidth/2;
		mMouseY = mScreenHeight/2;
		mService = service;
	}
	
	/**
	 * 添加指针
	 */
	public void addMouse(final boolean visble){

	}
	
	/**
	 * 显示鼠标指针
	 */
	
	public void internalShowMouse(int pointerCount,int[] pointerIds,float[] mouseX, float[] mouseY, final int action, boolean isAbsolute){
		if (pointerCount == 1){
			float newMouseX = 0;
			float newMouseY = 0;
			
			if(isAbsolute){
				newMouseX = mouseX[0];
				newMouseY = mouseY[0];
			}else{
				if (action == MotionEvent.ACTION_MOVE){
					newMouseX = mMouseX + (mouseX[0]-mLastX)*mScale;
					newMouseY = mMouseY + (mouseY[0]-mLastY)*mScale;
				} else {
					newMouseX = mMouseX;
					newMouseY = mMouseY;
				}
				mLastX = mouseX[0];
				mLastY = mouseY[0];
			}

			if(newMouseX>mScreenWidth) newMouseX = mScreenWidth-5;
			if(newMouseY>mScreenHeight) newMouseY = mScreenHeight-5;
			if(newMouseX<0) newMouseX = 0;
			if(newMouseY<0) newMouseY = 0;
			if(Math.floor(mMouseX)!= Math.floor(newMouseX)|| Math.floor(mMouseY)!= Math.floor(newMouseY)){
//				Log.d("MouseManager"," screenSize:"+mScreenWidth+","+mScreenHeight);
				mMouseX = newMouseX;
				mMouseY = newMouseY;
				if (!isAbsolute)
					dispatchMouse(mMouseX, mMouseY);
			}
		}
		
		if (!isAbsolute){
			if ((action & MotionEvent.ACTION_MASK)== MotionEvent.ACTION_POINTER_DOWN){
				int[] pIds = new int[1];
				float[] mX = new float[1];
				float[] mY = new float[1];
				pIds[0] = 0;
				mX[0] = mouseX[0] + 1;
				mY[0] = mouseY[0] + 1;
				injectMultiPoint(1, pIds, mX, mY, MotionEvent.ACTION_DOWN);
			}else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN){
				if (isMultPointer){
					injectMultiPoint(pointerCount,pointerIds,mouseX,mouseY,action);
					isMultPointer = false;
					return;
				}
			}
		} 
		moveMouse(pointerCount,pointerIds,mouseX, mouseY, action, isAbsolute);
	}
	
	/**
	 * 移动鼠标
	 * @param action
	 */
	volatile int cnt = 0;
	volatile int moveZeroCnt=0;
	volatile boolean downFlag = false;
	volatile boolean moveFlag = false;
	protected synchronized void moveMouse(int pointerCount,int[] pointerIds,float[] moveX, float[] moveY, int action, boolean isAbsolute){
		if(isAbsolute || isMultPointer){
			injectMultiPoint(pointerCount,pointerIds,moveX,moveY,action);
			return;
		}
		
//		if(cnt<5){
//			if(cnt!=moveZeroCnt){
//				moveFlag = true;
//			}
//		}else{
//			if(!moveFlag){
//				if(!downFlag){
//					if(action==MotionEvent.ACTION_MOVE){ 
//						Log.d("MouseManager","MouseX: " + mMouseX + "MouseY: " + mMouseY);
//						injectMoveMouse(MotionEvent.ACTION_DOWN);
//						downFlag = true;
//					}
//				}else{
//					injectMoveMouse(action);
//				}
//			}
//		}
		
		if(action == MotionEvent.ACTION_DOWN){
			cnt = 0;
//			moveZeroCnt = 0;
//			downFlag = false;
//			moveFlag = false;
		}else if(action == MotionEvent.ACTION_MOVE){
			cnt++;
//			if(Math.abs(moveX[0])<=5&&Math.abs(moveY[0])<=5){
//				moveZeroCnt++;
//			}
		}else if(action==MotionEvent.ACTION_UP){
//			if(cnt<5&&!downFlag){
			if(cnt<5){
				injectMoveMouse(MotionEvent.ACTION_DOWN);
				injectMoveMouse(MotionEvent.ACTION_UP);
			}
			cnt = 0;
//			moveZeroCnt = 0;
//			downFlag = false;
//			moveFlag = false;
		}
	}
	
	protected void injectMoveMouse(int action){
		long now = SystemClock.uptimeMillis();
		MotionEvent e = MotionEvent.obtain(now, now, action, mMouseX, mMouseY, 0);
		mService.injectPointerEvent(e, false);
	}
	
	protected void injectMultiPoint(int pointerCount,int[] pointerIds,float[] moveX, float[] moveY, int action){
		long now = SystemClock.uptimeMillis();
		PointerCoords[] pointerCoords = new PointerCoords[pointerCount];
		for (int i=0 ; i<pointerCount; i++){
			pointerCoords[i] = new PointerCoords();
			pointerCoords[i].x = moveX[i];
			pointerCoords[i].y = moveY[i];
			pointerCoords[i].pressure = 0.5f;
//			Log.d("MouseManager","pointer at "+i+" :"+
//					" xy:"+moveX[i]+","+moveY[i]+" pointerId:"+pointerIds[i]);
		}
		MotionEvent e = MotionEvent.obtain(now, now, action, pointerCount,
				pointerIds, pointerCoords, 0, 1.0f, 1.0f, 0, 0, 0, 0);
		mService.injectPointerEvent(e, false);
	}
	
	/**
	 * 调用WindowManagerService绘制鼠标
	 * @param mouseX
	 * @param mouseY
	 * @param state
	 */
	private void dispatchMouse(float X, float Y){
//		Long t = System.currentTimeMillis();
		mService.dispatchMousebyCd(X, Y);
//		Log.d("mouseManager","mouse reflect time:"+(System.currentTimeMillis()-t));
	}
	

	
	
	/**
	 * 获取当前鼠标X轴坐标
	 * @return
	 */
	public float getMouseX(){
		return mMouseX;
	}
	
	/**
	 * 获取当前鼠标Y轴坐标
	 * @return
	 */
	public float getMouseY(){
		return mMouseY;
	}
	
	public boolean isMultPointer(int action){
		if ((action & MotionEvent.ACTION_MASK)== MotionEvent.ACTION_POINTER_DOWN)
			isMultPointer = true;
		return isMultPointer;
	}
	
	public void ConfigChange(){
		if (mEnvironment != null) {
			mScreenWidth = mEnvironment.getScreenWidth();
			mScreenHeight = mEnvironment.getScreenHeight();
		}
	}
	
}
