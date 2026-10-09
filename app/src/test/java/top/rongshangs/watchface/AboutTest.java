package top.rongshangs.watchface;

import android.content.Intent;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AboutTest {
    static String json(String tag, String assetUrl) {
        return "{\"tag_name\":\"" + tag + "\",\"html_url\":\"" + AppInfo.RELEASES + "/tag/" + tag
                + "\",\"body\":\"改善取景\",\"assets\":[{\"name\":\"GalleryWatchfaceMaker-v1.2.0.apk\",\"browser_download_url\":\"" + assetUrl + "\"}]}";
    }
    void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) { ShadowLooper.idleMainLooper(); Thread.sleep(10); }
        ShadowLooper.idleMainLooper(); assertTrue(condition.getAsBoolean());
    }
    TextView find(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().equals(text)) return (TextView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            TextView found = find(((ViewGroup) view).getChildAt(i), text); if (found != null) return found;
        }
        return null;
    }
    @Test public void versionsCompareNumericallyAndHandlePrereleases() {
        assertTrue(ReleaseChecker.compare("v1.10.0", "1.9.9") > 0);
        assertTrue(ReleaseChecker.compare("v2.0.0", "1.11.0") > 0);
        assertEquals(Integer.valueOf(0), ReleaseChecker.compare("v1.1.0+build5", "1.1"));
        assertTrue(ReleaseChecker.compare("v1.1.0-rc.1", "1.1.0") < 0);
        assertTrue(ReleaseChecker.compare("v1.1.0-rc.10", "1.1.0-rc.2") > 0);
        assertTrue(ReleaseChecker.compare("v1.1.0", "1.1.0-rc.2") > 0);
        assertNull(ReleaseChecker.compare("nightly", "1.1.0"));
        assertNull(ReleaseChecker.compare("v999999999999999999999999999.0", "1.1.0"));
    }
    @Test public void onlyPublishedReleaseAndRepositoryApkAreAccepted() throws Exception {
        String url = AppInfo.RELEASES + "/download/v1.2.0/GalleryWatchfaceMaker-v1.2.0.apk";
        assertEquals(url, ReleaseChecker.parse(json("v1.2.0", url)).apkUrl);
        assertNull(ReleaseChecker.parse(json("v1.2.0", "https://example.com/app.apk")).apkUrl);
        assertThrows(ReleaseChecker.CheckException.class, () -> ReleaseChecker.parse("not json"));
        assertThrows(ReleaseChecker.CheckException.class, () -> ReleaseChecker.parse("{\"tag_name\":\"v1.2.0\",\"draft\":true}"));
        assertThrows(ReleaseChecker.CheckException.class, () -> ReleaseChecker.parse("{\"tag_name\":\"v1.2.0-rc1\",\"prerelease\":true}"));
        ReleaseChecker.Release noApk = ReleaseChecker.parse("{\"tag_name\":\"v1.2.0\",\"html_url\":\"https://example.com\"}");
        assertNull(noApk.apkUrl); assertEquals(AppInfo.RELEASES, noApk.pageUrl);
    }
    @Test public void renamedAppHasBottomAboutEntry() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup(); MainActivity activity = controller.get();
        assertEquals("画廊表盘制作器", activity.getString(R.string.app_name));
        TextView entry = find(activity.getWindow().getDecorView(), "关于 · 检查更新"); assertNotNull(entry); entry.performClick();
        Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(AboutActivity.class.getName(), intent.getComponent().getClassName()); controller.pause().stop().destroy();
    }
    @Test public void authorLinksAndNewReleaseDownloadAreCorrect() throws Exception {
        String url = AppInfo.RELEASES + "/download/v1.2.0/GalleryWatchfaceMaker-v1.2.0.apk";
        ActivityController<AboutActivity> controller = Robolectric.buildActivity(AboutActivity.class); AboutActivity activity = controller.get();
        activity.releaseSource = version -> ReleaseChecker.parse(json("v1.2.0", url)); controller.setup();
        await(() -> activity.checkButton.isEnabled()); assertTrue(activity.updateStatus.getText().toString().contains("发现新版本 v1.2.0")); assertEquals(View.VISIBLE, activity.downloadButton.getVisibility());
        View root = activity.getWindow().getDecorView(); assertNotNull(find(root, "戎Shang"));
        for (String[] link : new String[][]{{"RongShangs", AppInfo.GITHUB}, {"rongshangs.top", AppInfo.BLOG}, {"戎Shangs", AppInfo.COOLAPK}, {"RongShangs/gallery-watchface-maker", AppInfo.REPOSITORY}}) {
            TextView text = find(root, link[0] + "  ↗"); assertNotNull(text); ((View) text.getParent()).performClick();
            assertEquals(link[1], Shadows.shadowOf(activity).getNextStartedActivity().getDataString());
        }
        activity.downloadButton.performClick(); assertEquals(url, Shadows.shadowOf(activity).getNextStartedActivity().getDataString());
        activity.showRelease(ReleaseChecker.parse(json("v1.1.0", "")), "1.1.0");
        root.measure(View.MeasureSpec.makeMeasureSpec(1179, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(2556, View.MeasureSpec.EXACTLY)); root.layout(0, 0, 1179, 2556);
        Bitmap shot = Bitmap.createBitmap(1179, 2556, Bitmap.Config.ARGB_8888); root.draw(new Canvas(shot)); File file = new File(System.getProperty("verification.dir"), "about-screen.png"); file.getParentFile().mkdirs(); try (FileOutputStream out = new FileOutputStream(file)) { shot.compress(Bitmap.CompressFormat.PNG, 100, out); }
        controller.pause().stop().destroy();
    }
    @Test public void sameVersionAndNetworkFailureNeverOfferAnUpdate() throws Exception {
        ActivityController<AboutActivity> controller = Robolectric.buildActivity(AboutActivity.class); AboutActivity activity = controller.get();
        activity.releaseSource = version -> ReleaseChecker.parse(json("v1.1.0", "")); controller.setup(); await(() -> activity.checkButton.isEnabled());
        assertTrue(activity.updateStatus.getText().toString().contains("当前已是最新版本")); assertEquals(View.GONE, activity.downloadButton.getVisibility());
        activity.releaseSource = version -> { throw new IOException("offline"); }; activity.checkButton.performClick(); await(() -> activity.checkButton.isEnabled());
        assertTrue(activity.updateStatus.getText().toString().contains("未能连接 GitHub")); assertEquals(View.GONE, activity.downloadButton.getVisibility());
        activity.releaseSource = version -> ReleaseChecker.parse("{\"tag_name\":\"v1.2.0\"}"); activity.checkButton.performClick(); await(() -> activity.checkButton.isEnabled());
        assertEquals("前往发布页面", activity.downloadButton.getText().toString()); activity.downloadButton.performClick(); assertEquals(AppInfo.RELEASES, Shadows.shadowOf(activity).getNextStartedActivity().getDataString());
        controller.pause().stop().destroy();
    }
}
