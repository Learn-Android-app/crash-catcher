package org.netcook.android.tools;

public interface CrashCatchable {
	String TRACE_INFO = "TRACE_INFO";
	String HAS_CRASHED = "HAS_CRASHED";

	void onSendLog();
}
