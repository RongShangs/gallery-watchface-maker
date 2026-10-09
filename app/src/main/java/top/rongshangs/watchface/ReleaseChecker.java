package top.rongshangs.watchface;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Public GitHub Releases API; no credentials or background polling. */
final class ReleaseChecker {
    interface Source { Release fetch(String currentVersion) throws IOException; }
    static final class CheckException extends IOException {
        CheckException(String message) { super(message); }
    }
    static final class Release {
        final String tag, notes, pageUrl, apkUrl;
        Release(String tag, String notes, String pageUrl, String apkUrl) {
            this.tag = tag; this.notes = notes; this.pageUrl = pageUrl; this.apkUrl = apkUrl;
        }
    }

    static Release fetch(String currentVersion) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(AppInfo.LATEST_API).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "GalleryWatchfaceMaker/" + currentVersion);
        try {
            int status = connection.getResponseCode();
            if (status == 404) throw new CheckException("暂时还没有正式发布的版本，可稍后重试或打开发布页面。");
            if (status == 403 || status == 429) throw new CheckException("GitHub 暂时限制了查询频率，请稍后重试，或直接打开发布页面。");
            if (status != 200) throw new CheckException("GitHub 暂时无法查询（" + status + "），请稍后重试。");
            try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int n;
                while ((n = in.read(buffer)) != -1) {
                    if (out.size() + n > 1024 * 1024) throw new CheckException("版本信息过大，请直接打开发布页面。");
                    out.write(buffer, 0, n);
                }
                return parse(out.toString(StandardCharsets.UTF_8.name()));
            }
        } finally {
            connection.disconnect();
        }
    }

    static Release parse(String json) throws CheckException {
        try {
            JSONObject object = new JSONObject(json);
            if (object.optBoolean("draft") || object.optBoolean("prerelease"))
                throw new CheckException("暂时没有可用的正式版本，请打开发布页面查看。");
            String tag = object.getString("tag_name").trim();
            if (tag.isEmpty()) throw new JSONException("Missing version");
            String page = object.optString("html_url", AppInfo.RELEASES);
            if (!page.startsWith(AppInfo.RELEASES + "/tag/")) page = AppInfo.RELEASES;
            String apk = null; JSONArray assets = object.optJSONArray("assets");
            if (assets != null) for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.optString("name").toLowerCase(Locale.ROOT);
                String url = asset.optString("browser_download_url");
                if (name.endsWith(".apk") && !name.contains("unsigned") && !name.contains("debug")
                        && url.startsWith(AppInfo.RELEASES + "/download/")) {
                    apk = url;
                    if (name.startsWith("gallerywatchfacemaker")) break;
                }
            }
            return new Release(tag, object.optString("body", ""), page, apk);
        } catch (JSONException e) {
            throw new CheckException("无法读取版本信息，请稍后重试或打开发布页面。");
        }
    }

    static final Pattern VERSION = Pattern.compile("^[vV]?(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$");
    // null means that the release uses a tag whose order cannot be established.
    static Integer compare(String candidate, String current) {
        Matcher a = VERSION.matcher(candidate), b = VERSION.matcher(current);
        if (!a.matches() || !b.matches()) return null;
        try {
            for (int i = 1; i <= 3; i++) {
                long x = a.group(i) == null ? 0 : Long.parseLong(a.group(i));
                long y = b.group(i) == null ? 0 : Long.parseLong(b.group(i));
                if (x != y) return Long.compare(x, y);
            }
            String x = a.group(4), y = b.group(4);
            if (Objects.equals(x, y)) return 0;
            if (x == null) return 1;
            if (y == null) return -1;
            String[] xx = x.split("\\."), yy = y.split("\\.");
            for (int i = 0; i < Math.min(xx.length, yy.length); i++) {
                boolean xn = xx[i].matches("\\d+"), yn = yy[i].matches("\\d+");
                int difference = xn && yn ? Long.compare(Long.parseLong(xx[i]), Long.parseLong(yy[i]))
                        : xn != yn ? (xn ? -1 : 1) : xx[i].compareTo(yy[i]);
                if (difference != 0) return difference;
            }
            return Integer.compare(xx.length, yy.length);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
