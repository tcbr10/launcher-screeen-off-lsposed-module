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
                boolean beganFromNonInteractive = (boolean) param.args[2]; // Get the 3rd argument

                // 1. Only care about single press
                if (count != 1) return;

                // 2. CRITICAL FIX: If press started when screen was off, do NOTHING.
                // This lets the system wake up the device naturally.
                if (beganFromNonInteractive) {
                    return;
                }

                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                
                if (tasks != null && !tasks.isEmpty()) {
                    String topPackage = tasks.get(0).topActivity.getPackageName();
                    
                    if ("com.monobogdan.monolaunch".equals(topPackage)) {
                        Object pm = context.getSystemService(Context.POWER_SERVICE);
                        XposedHelpers.callMethod(pm, "goToSleep", SystemClock.uptimeMillis());
                        param.setResult(null); 
                    }
                }
            }
        });
    }
}
