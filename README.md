Android Crash Catcher
=============

Android Crash Catcher is simply a mechanism for catching any exception in android SDK


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