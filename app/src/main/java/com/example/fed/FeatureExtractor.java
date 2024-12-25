package com.example.fed;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class FeatureExtractor {
    private PackageManager packageManager;

    public FeatureExtractor(Context context) {
        this.packageManager = context.getPackageManager();
    }

    public float[] extractFeatures(String packageName) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName,
                    PackageManager.GET_PERMISSIONS);

            // Extract permissions
            boolean hasSendSms = hasPermission(packageInfo, "android.permission.SEND_SMS");
            boolean hasReadContacts = hasPermission(packageInfo, "android.permission.READ_CONTACTS");
            boolean hasAccessLocation = hasPermission(packageInfo, "android.permission.ACCESS_FINE_LOCATION");
            boolean hasWriteStorage = hasPermission(packageInfo, "android.permission.WRITE_EXTERNAL_STORAGE");

            return new float[]{
                    hasSendSms ? 1.0f : 0.0f,
                    hasReadContacts ? 1.0f : 0.0f,
                    hasAccessLocation ? 1.0f : 0.0f,
                    hasWriteStorage ? 1.0f : 0.0f
            };
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return new float[]{0.0f, 0.0f, 0.0f, 0.0f}; // Default values for unknown apps
        }
    }

    private boolean hasPermission(PackageInfo packageInfo, String permission) {
        if (packageInfo.requestedPermissions != null) {
            for (String p : packageInfo.requestedPermissions) {
                if (p.equals(permission)) {
                    return true;
                }
            }
        }
        return false;
    }
}
