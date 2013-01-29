package org.netcook.android.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;

import org.netcook.android.sysinfo.SystemInfoBuilder;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

/**
 * The Simple crash catcher activity for automatic send email report.
 * 
 * <p>
 * For use it need registry this activity to manifest: <activity
 * android:name="org.netcook.android.tools.CrashCatcherActivity" > and override method
 * getRecipient()
 * </p>
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */
public class CrashCatcherActivity extends Activity implements CrashCatchable {
	private static final String TAG = "CrashCatcherActivity";
	
	private StringBuilder defaultBody = new StringBuilder("");
	
	private static final String DEFAULT_EMAIL_FROM = "example@example.com";
	private static final String DEFAULT_SUBJECT = "Crash report";

    public final static String STORAGE_DIRECTORY = Environment.getExternalStorageDirectory().toString();
	public final static String SETTINGS_DIR_PROJECT = STORAGE_DIRECTORY + "/.settings";
	public final static String SETTINGS_DIR_LOG = STORAGE_DIRECTORY + "/.logcat";
	public final static String PATH_TO_LOG = SETTINGS_DIR_LOG + "/logcat.txt";
	public final static String PATH_TO_RESULT = SETTINGS_DIR_PROJECT + "/result.jpg";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

 		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread paramThread, final Throwable e) {
				final String stackTrace = StackTraceHelper.getStackTrace(e);
				Log.e(TAG, "Error: " + stackTrace);
				Intent crashedIntent = new Intent(CrashCatcherActivity.this, getStartActivityAfterCrached());
				crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				crashedIntent.putExtra(TRACE_INFO, stackTrace);
				crashedIntent.putExtra(HAS_CRASHED, true);
				startActivity(crashedIntent);
				System.exit(0);
			}
		});
		if (getIntent().getBooleanExtra(HAS_CRASHED, false)) {
			//setContentView(R.layout.crash_activity);
			new CrashSendTask().execute();
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onSendLog() {
		new CrashSendTask(true).execute();
	}

	private class CrashSendTask extends AsyncTask<Void, Void, Void> {
		private final boolean isMonuallyMode;
	
		public CrashSendTask() {
			this(false);
		}

		public CrashSendTask(boolean isMonuallyMode) {
			this.isMonuallyMode = isMonuallyMode;
		}

		@Override
		protected Void doInBackground(Void... params) {

			captureLog();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Intent i = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
			i.setType("message/rfc822");
			i.putExtra(Intent.EXTRA_EMAIL, new String[] { getRecipient() });
			i.putExtra(Intent.EXTRA_SUBJECT, getFinalSubject());

			ArrayList<Uri> uris = new ArrayList<Uri>();
			ArrayList<String> filePaths = new ArrayList<String>();

			File resultFile = new File(getPathResult());
			if (resultFile.exists()) {
				filePaths.add(getPathResult());
			}

			File logCatFile = new File(getPathLog());
			if (logCatFile.exists()) {
				filePaths.add(getPathLog());
			}

			for (String files : filePaths) {
				File fileIn = new File(files);
				Uri u = Uri.fromFile(fileIn);
				uris.add(u);
			}

			if (uris.size() > 0) {
				Log.e(TAG, uris.size() + " ");
				i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			}

			try {
				if (!isMonuallyMode) {
					defaultBody
						.append(" Error: ")
						.append(getIntent().getStringExtra(TRACE_INFO));
				} else {
					defaultBody.append("No error");
				}
				i.putExtra(Intent.EXTRA_TEXT, defaultBody.toString());
				startActivity(Intent.createChooser(i, "Send crash log..."));
			} catch (ActivityNotFoundException ex) {
				Log.e(TAG, "Error", ex);
			}
			finish();
		}
	}

	private void captureLog() {
		Process LogcatProc = null;
		BufferedReader reader = null;
		StringBuilder log = new StringBuilder();

		initFolder();

		try {
			LogcatProc = Runtime.getRuntime().exec(new String[] { "logcat", "-d" });

			reader = new BufferedReader(new InputStreamReader(LogcatProc.getInputStream()));

			String line;
			long time = System.currentTimeMillis();
			Log.d(TAG, System.currentTimeMillis() + "");
			while ((line = reader.readLine()) != null) {
				log.append(line).append(System.getProperty("line.separator"));
			}
			Log.d(TAG, (System.currentTimeMillis() - time) + "");
			log.append(new SystemInfoBuilder().build());
		} catch (IOException e) {
			Log.d(TAG, "get logcat failed", e);
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException e) {}
			}
		}
		
		if (getBaseContext().checkCallingOrSelfPermission("android.permission.READ_LOGS") 
				!= PackageManager.PERMISSION_GRANTED) {
			defaultBody.append("READ_LOGS access denied ");
		}
		
		saveLogToFile(log);
	}

	private void saveLogToFile(StringBuilder builder) {
		File outputFile = new File(getPathLog());
		if (outputFile.exists()) {
			outputFile.delete();
		}
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(builder.toString());
			
		} catch (IOException e) {
			Log.e(TAG, "saveLogToFile failed", e);
			defaultBody.append("Error writing file on device");
			
		} finally {
			try { writer.close(); } catch (Exception e) { }
		}
	}

	private String getFinalSubject() {
		try {
			String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			return "[" + getSubject() + " v" + versionName + "] " + DEFAULT_SUBJECT;
		} catch (Exception e) {
			return "[" + getSubject() + " NO VERSION] " + DEFAULT_SUBJECT;
		}
	}

	protected String getPathResult() {
		return PATH_TO_RESULT;
	}

	protected String getSubject() {
		return getPackageName();
	}

	protected String getPathLog() {
		return PATH_TO_LOG;
	}

	protected String getPathDirLog() {
		return SETTINGS_DIR_LOG;
	}

	protected String getRecipient() {
		return DEFAULT_EMAIL_FROM;
	}

	protected Class<?> getStartActivityAfterCrached() {
		return CrashCatcherActivity.class;
	}

	private void initFolder() {
		File tempDir = new File(getPathDirLog());
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
	}
}
