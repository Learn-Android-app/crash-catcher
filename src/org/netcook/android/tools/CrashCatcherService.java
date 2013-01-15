package org.netcook.android.tools;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class CrashCatcherService extends Service implements CrashCatchable {
	private static final String TAG = "CrashCatcherService";

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread paramThread, final Throwable e) {
				final String stackTrace = StackTraceHelper.getStackTrace(e);
				Log.e(TAG, "Error: " + stackTrace);
				Intent crashedIntent = new Intent(CrashCatcherService.this, getStartActivityAfterCrached());
				crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				crashedIntent.putExtra(TRACE_INFO, stackTrace);
				crashedIntent.putExtra(HAS_CRASHED, true);
				crashedIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(crashedIntent);
				System.exit(0);
			}
		});
		super.onCreate();
	}

	protected Class<?> getStartActivityAfterCrached() {
		return CrashCatcherActivity.class;
	}
}
