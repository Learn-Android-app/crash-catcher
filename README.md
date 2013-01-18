Android Crash Catcher
=============

Android Crash Catcher is simply a mechanism for catching any exception in android SDK

<img style="position: relative; width: 768px; margin: 0;" src="http://www.netcook.org/images/products/android-crach-catcher.png"/>

---

Installation
-----------

    mvn install


Usage
-----
Add to your pom.xml following:

		<dependency>
			<groupId>org.netcook.android</groupId>
			<artifactId>crashcatcher</artifactId>
			<version>1.0.3</version>
		</dependency>

Add the permission for read logs to AndroidManifest.xml file:
	
		<uses-permission android:name="android.permission.READ_LOGS" />

Add to your base activity CrashCatcherActivity as parent:

    public class BaseActivity extends CrashCatcherActivity {
        @Override
        protected String getRecipient() {
        	return "xxxxx@xxxxx.xxx";
        }
        
        @Override
        protected Class<?> getStartActivityAfterCrached() {
        	return BaseActivity.class;
        }
    }

Add to your base service CrashCatcherService as parent:

    abstract public class AbstractService extends CrashCatcherService {
    
        @Override
        protected Class<?> getStartActivityAfterCrached() {
            return BaseActivity.class;
        }
    }

Add the base activity to AndroidManifest.xml file:
	
		<activity android:name="BaseActivity" />

Profit!

