package org.netcook.android.tools;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTraceHelper {

	public static String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		t.printStackTrace(pw);
		pw.flush();
		sw.flush();
		return sw.toString();
	}

}
