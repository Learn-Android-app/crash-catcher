package org.netcook.android.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.os.Environment;
/**
 * The Simple crash catcher activity for automatic send email report.
 * 
 * <p>
 * For use it need registry this activity to manifest: <activity
 * android:name="org.netcook.android.tools.CrashCatcherActivity" > and override two methods
 * getSubject() and getRecipient()
 * </p>
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */

abstract public class CrashCatcherActivity extends Activity {
	private static final String TAG = "CrashCatcherActivity";
	public static final String TRACE_INFO = "TRACE_INFO";
	public static final String HAS_CRASHED = "HAS_CRASHED";
	
	private static String DEFAULT_BODY = "";
	
	private static final String DEFAULT_EMAIL_FROM = "example@example.com";
	private static final String DEFAULT_SUBJECT = "Crash report";

    public final static String STORAGE_DIRECTORY = Environment.getExternalStorageDirectory().toString();
	public final static String SETTINGS_DIR_PROJECT = STORAGE_DIRECTORY + "/tesseract";
	public final static String SETTINGS_DIR_LOG = STORAGE_DIRECTORY + "/.logcat";
	public final static String PATH_TO_LOG = SETTINGS_DIR_LOG + "/logcat.txt";
	public final static String PATH_TO_RESULT = SETTINGS_DIR_PROJECT + "/result.jpg";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

 		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread paramThread, final Throwable e) {
				Log.e(TAG, "Error: " + getStackTrace(e));
				Intent crashedIntent = new Intent(CrashCatcherActivity.this, CrashCatcherActivity.class);
				crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				crashedIntent.putExtra(TRACE_INFO, getStackTrace(e));
				crashedIntent.putExtra(HAS_CRASHED, true);
				startActivity(crashedIntent);
				System.exit(0);
			}
		});
		if (getIntent().getBooleanExtra(HAS_CRASHED, false)) {
			//setContentView(R.layout.crash_activity);
			new CrashSend().execute();
		}
		super.onCreate(savedInstanceState);
	}

	public class CrashSend extends AsyncTask<Void, Void, Void> {

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
			i.putExtra(Intent.EXTRA_SUBJECT, getSubject());

			ArrayList<Uri> uris = new ArrayList<Uri>();
			ArrayList<String> filePaths = new ArrayList<String>();

			File file = new File(getPathResult());
			if (file.exists()) {
				filePaths.add(getPathResult());
			}

			File file2 = new File(getPathLog());
			if (file2.exists()) {
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
				DEFAULT_BODY += " Error: " + getIntent().getStringExtra(TRACE_INFO);
				i.putExtra(Intent.EXTRA_TEXT, DEFAULT_BODY);
				startActivity(Intent.createChooser(i, "Send crash log..."));
			} catch (ActivityNotFoundException ex) {
				Log.e(TAG, "Error", ex);
			}
			super.onPostExecute(result);
			finish();
		}
	}

	private void captureLog() {
		Process LogcatProc = null;
		BufferedReader reader = null;
		ArrayList<String> log = new ArrayList<String>();
		createDir(getPathDirLog());

		try {
			LogcatProc = Runtime.getRuntime().exec(new String[] { "logcat", "-d" });

			reader = new BufferedReader(new InputStreamReader(LogcatProc.getInputStream()));

			String line;
			long time = System.currentTimeMillis();
			Log.d(TAG, System.currentTimeMillis() + "");
			while ((line = reader.readLine()) != null) {
				log.add(line);
			}
			Log.d(TAG, (System.currentTimeMillis() - time) + "");
			log.add("--SYSTEM--");
			log.add("BOARD:");
			log.add(android.os.Build.BOARD);
			log.add("BOOTLOADER:");
			log.add(android.os.Build.BOOTLOADER);
			log.add("BRAND:");
			log.add(android.os.Build.BRAND);
			log.add("CPU_ABI:");
			log.add(android.os.Build.CPU_ABI);
			log.add("CPU_ABI2");
			log.add(android.os.Build.CPU_ABI2);
			log.add("DEVICE:");
			log.add(android.os.Build.DEVICE);
			log.add("DISPLAY:");
			log.add(android.os.Build.DISPLAY);
			log.add("FINGERPRINT:");
			log.add(android.os.Build.FINGERPRINT);
			log.add("HARDWARE:");
			log.add(android.os.Build.HARDWARE);
			log.add("HOST:");
			log.add(android.os.Build.HOST);
			log.add("ID:");
			log.add(android.os.Build.ID);
			log.add("MANUFACTURER:");
			log.add(android.os.Build.MANUFACTURER);
			log.add("MODEL:");
			log.add(android.os.Build.MODEL);
			log.add("PRODUCT:");
			log.add(android.os.Build.PRODUCT);
			log.add("RADIO:");
			log.add(android.os.Build.RADIO);
			log.add("SERIAL:");
			log.add(android.os.Build.SERIAL);
			log.add("TAGS:");
			log.add(android.os.Build.TAGS);
			log.add("TYPE:");
			log.add(android.os.Build.TYPE);
			log.add("UNKNOWN:");
			log.add(android.os.Build.UNKNOWN);
			log.add("USER:");
			log.add(android.os.Build.USER);
			log.add("TIME:");
			log.add(String.valueOf(android.os.Build.TIME));
		} catch (IOException e) {
			Log.d(TAG, e.toString());
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}

		saveLogToFile(log);
	}

	private void saveLogToFile(ArrayList<String> logString) {
		try {
			File outputFile = new File(getPathLog());
			Writer writer = new BufferedWriter(new FileWriter(outputFile));

			for (String str : logString) {
				writer.write(str);
				writer.write(System.getProperty("line.separator"));
			}

			writer.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			DEFAULT_BODY = "Error writing file; ";
		}
	}

	private String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		t.printStackTrace(pw);
		pw.flush();
		sw.flush();
		return sw.toString();
	}

	protected String getSubject() {

		try {
			String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			return DEFAULT_SUBJECT + " Version: " + versionName;
		} catch (Exception e) {
			return DEFAULT_SUBJECT + " Version: not found";
		}
	}

    protected String getPathResult() {
	    return PATH_TO_RESULT;
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

	private void createDir(String puth) {
		File tessdata = new File(puth);
		if (!tessdata.exists()) {
			tessdata.mkdirs();
		}
	}
}
