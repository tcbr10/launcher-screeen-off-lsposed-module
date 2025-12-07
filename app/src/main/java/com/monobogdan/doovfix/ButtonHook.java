package com.monobogdan.doovfix;

import android.app.ActivityManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import java.util.List;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ButtonHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("android")) return;

        Class<?> pwmClass = XposedHelpers.findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);

        XposedHelpers.findAndHookMethod(pwmClass, "powerPress", 
                long.class, int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int count = (int) param.args[1];
                if (count != 1) return;

                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                
                if (tasks != null && !tasks.isEmpty()) {
                    String topPackage = tasks.get(0).topActivity.getPackageName();
                    if ("com.monobogdan.monolaunch".equals(topPackage)) {
                        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        pm.goToSleep(SystemClock.uptimeMillis());
                        param.setResult(null); 
                    }
                }
            }
        });
    }
}
