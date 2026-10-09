package top.rongshangs.watchface;

import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

final class AppInfo {
    static final String REPOSITORY = "https://github.com/RongShangs/gallery-watchface-maker";
    static final String RELEASES = REPOSITORY + "/releases";
    static final String LATEST_API = "https://api.github.com/repos/RongShangs/gallery-watchface-maker/releases/latest";
    static final String AUTHOR = "戎Shang";
    static final String GITHUB_NAME = "RongShangs";
    static final String GITHUB = "https://github.com/RongShangs";
    static final String BLOG = "https://rongshangs.top";
    static final String COOLAPK_NAME = "戎Shangs";
    static final String COOLAPK = "https://www.coolapk.com/u/3261403";

    static String version(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    static void browse(Context context, String url) {
        Uri uri = Uri.parse(url);
        if (!"https".equals(uri.getScheme())) return;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "手机没有可用的浏览器", Toast.LENGTH_LONG).show();
        }
    }
}
