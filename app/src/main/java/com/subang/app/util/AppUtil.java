package com.subang.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.subang.api.SubangAPI;
import com.subang.api.UserAPI;
import com.subang.app.activity.R;
import com.subang.app.bean.AppEtc;
import com.subang.bean.AppInfo;
import com.subang.bean.Result;
import com.subang.domain.User;
import com.subang.util.WebConst;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Qiang on 2015/10/31.
 */
public class AppUtil {

    private static int NETWORK_TIP_INTERVAL = 30000;//30s

    private static boolean isNetworkTip = true;
    private static Timer timer;    //调度timerTask
    private static TimerTask timerTask;        //30s后可以再次提示网络错误

    //把用户信息（app配置）保存在磁盘
    public static void saveConf(Context context, User user) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string
                .file_user), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("cellnum", user.getCellnum());
        editor.putString("password", user.getPassword());
        editor.commit();
        AppConf.invalidate();
        SubangAPI.invalidate();
    }

    //从磁盘删除用户信息（app配置）
    public static void deleteConf(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string
                .file_user), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
        AppConf.invalidate();
        SubangAPI.invalidate();
    }

    //配置app，使用AppConf前调用此函数。如果没有配置，则配置
    public static boolean conf(Context context) {
        if (AppConf.isConfed()) {
            return true;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string
                .file_user), Context.MODE_PRIVATE);
        String basePath = context.getFilesDir().getAbsolutePath() + "/";
        String cellnum = sharedPreferences.getString("cellnum", null);
        String password = sharedPreferences.getString("password", null);
        if (cellnum != null) {
            AppConf.basePath = basePath;
            AppConf.cellnum = cellnum;
            AppConf.password = password;
            return true;
        }
        return false;
    }

    //配置api，使用api前调用此函数。如果没有配置，则配置
    public static void confApi(Context context) {
        if (SubangAPI.isConfed()) {
            return;
        }
        conf(context);
        SubangAPI.conf(WebConst.USER, AppConf.cellnum, AppConf.basePath);
    }

    public static AppEtc getEtc(Context context) {
        AppEtc etc = new AppEtc();
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string
                .file_etc), Context.MODE_PRIVATE);
        etc.setFirst(sharedPreferences.getBoolean("first", true));
        return etc;
    }

    public static void saveEtc(Context context, AppEtc etc) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string
                .file_etc), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("first", etc.isFirst());
        editor.commit();
    }


    public static boolean checkNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }
        return networkInfo.isAvailable();
    }

    public static void networkTip(Context context) {
        if (!isNetworkTip) {
            return;
        }
        Toast toast = Toast.makeText(context, R.string.err_network, Toast.LENGTH_SHORT);
        toast.show();
        isNetworkTip = false;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                isNetworkTip = true;
            }
        };
        if (timer == null) {
            timer = new Timer();
        }
        timer.schedule(timerTask, NETWORK_TIP_INTERVAL);
    }

    public static void tip(Context context, String info) {
        Toast toast = Toast.makeText(context, info, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void tip(Context context, @StringRes int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        toast.show();
    }

    //后台执行
    public static boolean setLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        String bestProvider = locationManager.getBestProvider(new Criteria(), true);
        Location location = null;
        try {
            location = locationManager.getLastKnownLocation(bestProvider);
            if (location == null) {
                List<String> providers = locationManager.getAllProviders();
                for (String provider : providers) {
                    location = locationManager.getLastKnownLocation(provider);
                    if (location != null) {
                        break;
                    }
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (location == null) {
            return false;
        }
        com.subang.domain.Location myLocation = new com.subang.domain.Location();
        myLocation.setLatitude(Double.toString(location.getLatitude()));
        myLocation.setLongitude(Double.toString(location.getLongitude()));
        Result result = UserAPI.setLocation(myLocation);
        if (result == null || !result.getCode().equals(Result.OK)) {
            return false;
        }
        return true;
    }

    public static AppInfo getAppInfo(Context context) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Integer version = packageInfo.versionCode;
        AppInfo appInfo = new AppInfo(AppInfo.USER_USER, AppInfo.OS_ANDROID, version);
        return appInfo;
    }
}
