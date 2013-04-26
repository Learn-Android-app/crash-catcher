package org.netcook.android.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;

import org.netcook.android.security.Crypter;
import org.netcook.android.security.Crypter.EncryptResponse;
import org.netcook.android.sysinfo.SystemInfoBuilder;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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

	private static final String DEFAULT_EMAIL_FROM = "example@example.com";
	private static final String DEFAULT_CRASH_SUBJECT = "Crash report";
	private static final String DEFAULT_SUBJECT = "Report";

	private static final String DEFAULT_SALT = "werwjkfiwef02349oyr";
	private static final String DEFAULT_PASSWORD = "xegFLqN2m";

	private final static String STORAGE_DIRECTORY = Environment.getExternalStorageDirectory().toString();
	private final static String SETTINGS_DIR_PROJECT = STORAGE_DIRECTORY + "/.settings";
	private final static String SETTINGS_DIR_LOG = STORAGE_DIRECTORY + "/.logcat";
	private final static String PATH_TO_LOG = SETTINGS_DIR_LOG + "/logcat.txt";
	private final static String PATH_TO_RESULT = SETTINGS_DIR_PROJECT + "/result.jpg";

	private Crypter nCrypter;

	public class CrashCatcherError extends Error {

		public CrashCatcherError(String detailMessage) {
			super(detailMessage);
		}

		public CrashCatcherError(Throwable throwable) {
			super(throwable);
		}
		
	}

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
			new CrashSendTask().execute();
		}
		if (isEncryptContent()) {
			nCrypter = new Crypter(getPassword(), getSalt());
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onSendLog() {
		new CrashSendTask(true).execute();
	}

	@Override
	public void onReportReady() {

	}

	protected String getPathResult() {
		return PATH_TO_RESULT;
	}
	
	protected String getAttachFileDir(){
		return null;
	}

	protected String getSubject() {
		return DEFAULT_SUBJECT;
	}

	protected String getCrashSubject() {
		return DEFAULT_CRASH_SUBJECT;
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

	protected String getSalt() {
		return DEFAULT_SALT;
	}

	protected String getPassword() {
		return DEFAULT_PASSWORD;
	}

	protected boolean isEncryptContent() {
		return false;
	}

	protected Class<?> getStartActivityAfterCrached() {
		return CrashCatcherActivity.class;
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
		} catch (Exception e) {
			throw new CrashCatcherError("Get logcat failed");
		} finally {
			try { reader.close(); } catch (Exception e) {}
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
			if (isEncryptContent()) {
				EncryptResponse r = nCrypter.encrypt(builder.toString());
				writer.write(r.encrypted);
				writer.write("\n");
				writer.write(r.iv);
			} else {
				writer.write(builder.toString());
			}
			onReportReady();
		} catch (Exception e) {
			Log.e(TAG, "saveLogToFile failed", e);
			throw new CrashCatcherError("Error writing file on device");
		} finally {
			try { writer.close(); } catch (Exception e) { }
		}
	}

	private String getFinalSubject(boolean isMonuallyMode) {
		try {
			String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			return "[" + getPackageName() + " v" + versionName + "] " + (isMonuallyMode ? getSubject() : getCrashSubject()) + (isEncryptContent()?"(ENCRYPTED)":"");
		} catch (Exception e) {
			return "[" + getPackageName() + " NO VERSION] " + getSubject();
		}
	}

	private void initFolder() {
		File tempDir = new File(getPathDirLog());
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
	}

	private class CrashSendTask extends AsyncTask<Void, Void, Boolean> {
		private final boolean isMonuallyMode;
		private StringBuilder defaultBody = new StringBuilder("");

		public CrashSendTask() {
			this(false);
		}

		public CrashSendTask(boolean isMonuallyMode) {
			this.isMonuallyMode = isMonuallyMode;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				captureLog();
			} catch (CrashCatcherError e) {
				defaultBody.append(e.getMessage()).append("\n");
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (isCancelled()) {
				return;
			}
			Intent i = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
			i.setType("message/rfc822");
			i.putExtra(Intent.EXTRA_EMAIL, new String[] { getRecipient() });
			i.putExtra(Intent.EXTRA_SUBJECT, getFinalSubject(isMonuallyMode));

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
			
			attachFiles(filePaths, getAttachFileDir());

			for (String files : filePaths) {
				File fileIn = new File(files);
				Uri u = Uri.fromFile(fileIn);
				uris.add(u);
			}

			if (uris.size() > 0) {
				Log.e(TAG, uris.size() + " ");
				i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			}
			defaultBody.append(getIntent().getStringExtra(RESULT_EXTRA_TEXT));
			try {
				if (!isMonuallyMode) {
					defaultBody
						.append("\n")
						.append(" Error: ")
						.append(getIntent().getStringExtra(TRACE_INFO));
				} else {
					defaultBody
					.append("\n")
					.append("Note: Manually sending");
				}
				String extraText = defaultBody.toString();
				i.putExtra(Intent.EXTRA_TEXT, extraText);
				startActivity(Intent.createChooser(i, "Send report via:"));
			} catch (ActivityNotFoundException ex) {
				Log.e(TAG, "Error", ex);
			}
			if (!isMonuallyMode) {
				finish();
			}
		}
		
		private void attachFiles(ArrayList<String> attachList, String dir){
			Log.d(TAG,"attachFiles, dir: "+dir);
			if(dir == null){
				return;
			}
			File directory = new File(dir);
			if(directory.exists()){
				File[] files = directory.listFiles();
				if(files != null){
					for (File file : files) {
						Log.d(TAG,"attachFiles, file: "+file.getPath());
						attachList.add(file.getPath());
					}
				}
			}
		}
	}

}
