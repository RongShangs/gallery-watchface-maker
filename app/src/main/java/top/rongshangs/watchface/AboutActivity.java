package top.rongshangs.watchface;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.util.concurrent.*;

public final class AboutActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    ReleaseChecker.Source releaseSource = ReleaseChecker::fetch;
    TextView updateStatus, notes;
    Button checkButton, downloadButton;
    private String downloadUrl;
    private boolean checking;

    int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    TextView text(String content, int size, int color) {
        TextView view = new TextView(this); view.setText(content); view.setTextSize(size); view.setTextColor(color);
        view.setLineSpacing(dp(4), 1); return view;
    }
    LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    void gap(LinearLayout parent, int height) { parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    GradientDrawable shape(int color) { GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(20)); return bg; }
    Button button(String label) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextSize(14);
        button.setTextColor(MainActivity.BG); button.setBackground(shape(MainActivity.ACCENT)); button.setStateListAnimator(null);
        return button;
    }
    LinearLayout card(LinearLayout parent, String title) {
        LinearLayout view = column(); view.setPadding(dp(20), dp(18), dp(20), dp(18)); view.setBackground(shape(MainActivity.CARD));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(16); parent.addView(view, params);
        TextView heading = text(title, 17, MainActivity.FG); heading.setTypeface(null, Typeface.BOLD); view.addView(heading); gap(view, 12); return view;
    }
    void link(LinearLayout parent, String label, String value, String url) {
        LinearLayout row = column(); row.setPadding(0, dp(10), 0, dp(10)); row.setMinimumHeight(dp(56));
        row.addView(text(label, 12, MainActivity.MUTED)); row.addView(text(value + "  ↗", 15, MainActivity.ACCENT));
        row.setContentDescription(label + "，" + value + "，打开网页"); row.setOnClickListener(v -> AppInfo.browse(this, url)); parent.addView(row);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = column(); root.setBackgroundColor(MainActivity.BG);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        ScrollView scroll = new ScrollView(this); root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout body = column(); body.setPadding(dp(22), dp(12), dp(22), dp(24)); scroll.addView(body);
        TextView back = text("‹  返回制作", 14, MainActivity.ACCENT); back.setGravity(Gravity.CENTER_VERTICAL); body.addView(back, new LinearLayout.LayoutParams(-1, dp(48))); back.setOnClickListener(v -> finish());
        gap(body, 10); TextView title = text("关于", 30, MainActivity.FG); title.setTypeface(null, Typeface.BOLD); body.addView(title);
        gap(body, 6); body.addView(text(getString(R.string.app_name), 18, MainActivity.FG));
        body.addView(text("版本 " + AppInfo.version(this), 13, MainActivity.MUTED));

        LinearLayout update = card(body, "版本更新");
        updateStatus = text("正在查询 GitHub 最新版本…", 14, MainActivity.FG); update.addView(updateStatus);
        notes = text("", 12, MainActivity.MUTED); notes.setVisibility(View.GONE); update.addView(notes); gap(update, 14);
        checkButton = button("检查更新"); update.addView(checkButton, new LinearLayout.LayoutParams(-1, dp(48))); checkButton.setOnClickListener(v -> checkUpdates());
        downloadButton = button("下载新版 APK"); LinearLayout.LayoutParams downloadParams = new LinearLayout.LayoutParams(-1, dp(48)); downloadParams.topMargin = dp(10); update.addView(downloadButton, downloadParams); downloadButton.setVisibility(View.GONE); downloadButton.setOnClickListener(v -> { if (downloadUrl != null) AppInfo.browse(this, downloadUrl); });
        link(update, "GitHub Releases", "查看全部发布版本", AppInfo.RELEASES);

        LinearLayout author = card(body, "作者"); author.addView(text(AppInfo.AUTHOR, 21, MainActivity.FG));
        link(author, "GitHub", AppInfo.GITHUB_NAME, AppInfo.GITHUB);
        link(author, "作者博客", "rongshangs.top", AppInfo.BLOG);
        link(author, "酷安昵称", AppInfo.COOLAPK_NAME, AppInfo.COOLAPK);

        LinearLayout project = card(body, "开源项目");
        link(project, "GitHub 仓库", "RongShangs/gallery-watchface-maker", AppInfo.REPOSITORY);
        link(project, "开源许可", "GPL-3.0", AppInfo.REPOSITORY + "/blob/main/LICENSE");
        gap(body, 20); TextView privacy = text("选图、取景和制作均在本地完成。\n仅检查更新时联网访问 GitHub。", 12, MainActivity.MUTED); privacy.setGravity(Gravity.CENTER); body.addView(privacy);
        setContentView(root); root.requestApplyInsets(); checkUpdates();
    }

    void checkUpdates() {
        if (checking) return;
        checking = true; checkButton.setEnabled(false); checkButton.setText("正在检查…");
        downloadButton.setVisibility(View.GONE); notes.setVisibility(View.GONE); updateStatus.setText("正在查询 GitHub 最新版本…");
        String current = AppInfo.version(this);
        worker.execute(() -> {
            try {
                ReleaseChecker.Release release = releaseSource.fetch(current);
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) showRelease(release, current); });
            } catch (IOException e) {
                String message = e instanceof ReleaseChecker.CheckException ? e.getMessage() : "未能连接 GitHub，请检查网络后重试，或打开发布页面。";
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) { updateStatus.setText(message); finished(); } });
            }
        });
    }

    void showRelease(ReleaseChecker.Release release, String current) {
        downloadButton.setVisibility(View.GONE);
        notes.setVisibility(View.GONE);
        Integer order = ReleaseChecker.compare(release.tag, current);
        updateStatus.setText(order == null ? "发布版本：" + release.tag + "，可打开发布页面确认。"
                : order > 0 ? "发现新版本 " + release.tag + "\n当前版本 " + current : "当前已是最新版本 " + current);
        if (order != null && order > 0) {
            downloadUrl = release.apkUrl == null ? release.pageUrl : release.apkUrl;
            downloadButton.setText(release.apkUrl == null ? "前往发布页面" : "下载新版 APK"); downloadButton.setVisibility(View.VISIBLE);
            if (!release.notes.trim().isEmpty()) {
                String body = release.notes.trim(); notes.setText("\n" + body.substring(0, Math.min(body.length(), 1800)));
                notes.setVisibility(View.VISIBLE);
            }
        }
        finished();
    }
    void finished() { checking = false; checkButton.setEnabled(true); checkButton.setText("重新检查"); }
    @Override protected void onDestroy() { super.onDestroy(); worker.shutdownNow(); }
}
