package org.netcook.android.sysinfo;

import android.util.Log;

public class SystemInfoBuilder {
	private static final String TAG = "SystemInfoBuilder";

	private static String SYSTEM_INFO_IMPLEMENTATION[] = new String[]{
		"Legacy",
		"V8",
		"V9",
		"V14",
	};

	public String build() {
		int index = 0;
		switch (android.os.Build.VERSION.SDK_INT) {
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
			index = 0;
			break;
		case 8:
			index = 1;
			break;
		case 9:
		case 10:
		case 11:
		case 12:
		case 13:
			index = 2;
			break;
		case 14:
			index = 3;
			break;

		default:
			index = 3;
			break;
		}
		try {
			Class<?> impl = Class.forName("org.netcook.android.sysinfo.SystemInfo" + SYSTEM_INFO_IMPLEMENTATION[index]);
			ISystemInfo systemInfo = (ISystemInfo) impl.newInstance();
			return systemInfo.build();
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "System Info implementation is not found", e);
		} catch (InstantiationException e) {
			Log.e(TAG, "System Info implementation is not found", e);
		} catch (IllegalAccessException e) {
			Log.e(TAG, "System Info implementation is not found", e);
		}
		return "EMPTY";
	}
}
