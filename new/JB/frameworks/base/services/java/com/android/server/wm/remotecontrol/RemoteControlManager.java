package com.android.server.wm.remotecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.android.server.wm.WindowManagerService;

public class RemoteControlManager {
	private Context mContext;
	private Environment mEnvironment;
	private WindowManagerService mService;
	
	private MouseManager mMouseManager;
	private MouseRequestListener mouseListener;
	
	private SoftKeyRequestListener SoftKeyListener;
	
	private ScrollRequestListener ScrollRequestListener;
	
	private WimoRequestListener WimoRequestListener;
	
	private GSensorRequestListener GsensorRequestListener;
	
	public static final int MOUSE_REMOTE_CONTROL_PORT = 56456;
	public static final int SOFTKEY_REMOTE_CONTROL_PORT = MOUSE_REMOTE_CONTROL_PORT + 1;
	public static final int GSENSOR_REMOTE_CONTROL_PORT = MOUSE_REMOTE_CONTROL_PORT + 2;
	
	public RemoteControlManager(Context context, WindowManagerService service){
		mContext = context;
		mService = service;
	}
	
	private BroadcastReceiver mConfigChangeReciever = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.d("RemoteControlManager","BroadcastReceiver:"+intent.getAction());
			if (intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED)){
				if (mEnvironment != null) {
					mEnvironment.onConfigurationChanged(context.getResources().getConfiguration(), context);
				}
				if (mouseListener != null) {
					mouseListener.ConfigChange();
				}
				if (mMouseManager != null) {
					mMouseManager.ConfigChange();
				}
				if (ScrollRequestListener != null) {
					ScrollRequestListener.ConfigChange();
				}
			}
		}
	};
	
	public void startListener(){
		 //remote control thread		
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
		mContext.registerReceiver(mConfigChangeReciever, filter);
		
		mEnvironment = Environment.getInstance();
		mEnvironment.onConfigurationChanged(mContext.getResources().getConfiguration(), mContext);
		
        mMouseManager = new MouseManager(mContext,mService);
		
		mouseListener = new MouseRequestListener(mContext);
		mouseListener.setMouseManager(mMouseManager);
		
		SoftKeyListener = new SoftKeyRequestListener(mContext, mService);
		SoftKeyListener.setMouseManager(mMouseManager);
		
		ScrollRequestListener = new ScrollRequestListener(mContext, mService);
		
		WimoRequestListener = new WimoRequestListener(mContext);
		
		GsensorRequestListener = new GSensorRequestListener();
		
		ControlSocket mMouseControlSocket = new ControlSocket(MOUSE_REMOTE_CONTROL_PORT,"MouseControlSocket");
		mMouseControlSocket.setRequestListener(new ControlSocket.RequestListener() {
			
			@Override
			public void requestRecieved(UDPPacket packet) {
				// TODO Auto-generated method stub
				if (packet == null) return;
				RemoteControlRequest request = new RemoteControlRequest(packet);
				if (request.isBadMsg) {
					if (TypeConstants.TYPE_DEVICE_CHECK_COMMAND == request.getCommandType()){
						RemoteControlRequest deviceRespone = new RemoteControlRequest();
						deviceRespone.setControlType(TypeConstants.TYPE_DEVICE_CHECK);
						deviceRespone.setRequestHost(packet.getRemoteAddress());
						deviceRespone.post(RemoteControlRequest.REMOTE_CONTROL_PORT);
						deviceRespone.close();
					}
					return;
				}
				if(TypeConstants.TYPE_MOUSE_COMMAND == request.getCommandType()){
					mouseListener.requestRecieved(packet);
				}else if (TypeConstants.TYPE_GSENSOR_COMMAND == request.getCommandType()){
					GsensorRequestListener.requestRecieved(packet);
				}else if (TypeConstants.TYPE_SCROLL_COMMAND == request.getCommandType()){
					ScrollRequestListener.requestRecieved(packet);
				}else if(TypeConstants.TYPE_SOFTKEY_COMMAND == request.getCommandType()){
					SoftKeyListener.requestRecieved(packet);
				}else if (TypeConstants.TYPE_WIMO_COMMAND == request.getCommandType()){
					WimoRequestListener.requestRecieved(packet);
				}
			}
		});
		mMouseControlSocket.start();
		
//		SoftKeyListener = new SoftKeyRequestListener(mContext, mService);
//		SoftKeyListener.setMouseManager(mMouseManager);
//		ControlSocket mSoftKeyControlSocket = new ControlSocket(SOFTKEY_REMOTE_CONTROL_PORT, "SoftKeyControlSocket");
//		mSoftKeyControlSocket.setRequestListener(SoftKeyListener);
//		mSoftKeyControlSocket.start();
	}
	
	public SensorManagerService getRemoteSensorManager(){
		if (GsensorRequestListener != null)
			return GsensorRequestListener.getSensorManager();
		return null;
	}
}
