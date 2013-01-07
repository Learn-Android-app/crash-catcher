package org.netcook.android.sysinfo;

abstract public class AbstractSystemInfo implements ISystemInfo {
	protected static final String DELIMITER = System.getProperty("line.separator");

	protected void add(StringBuilder sb, String title, String value) {
		sb.append(title)
		.append(":")
		.append(value)
		.append(DELIMITER);

	}
}
