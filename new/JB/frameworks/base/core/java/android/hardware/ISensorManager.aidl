/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    ISensorManager
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2011-12-17 ����01:03:04  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2011-12-17      xwf         1.0         create
*******************************************************************/

package android.hardware;

import android.hardware.SensorParcel;

/**
 * System private interface to the remote sensor manager.
 *
 * {@hide}
 */
interface ISensorManager {

    boolean injectSensorEvent(in float[] values, int accuracy, long timestamp, int sensorType);
    boolean setRemoteSensorEnabled(boolean enable);
    
    boolean getRemoteSensorEnabled();
    SensorParcel obtainSensorEvent(int queue);
    int createSensorQueue();
    void destroySensorQueue(int queue);
    
}