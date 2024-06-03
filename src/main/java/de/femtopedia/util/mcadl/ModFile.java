package de.femtopedia.util.mcadl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public record ModFile(String name, String page_url, String redirect_url, String direct_url, String archive_url) {

    private static final Map<Predicate<URL>, Function<URL, URL>> RESOLVERS = Map.of(
            url -> url.getHost().contains("mediafire.com"), ModFile::resolveMediaFire
    );

    public void downloadTo(Path path) throws IOException {
        File dest = path.resolve(name).toFile();

        if (archive_url != null && !archive_url.isEmpty()) {
            download(new URL(archive_url), dest);
            return;
        }

        if (tryDownload(direct_url, dest)) return;
        if (tryDownload(redirect_url, dest)) return;
        if (tryDownload(page_url, dest)) return;

        System.err.println("Needs manual download: " + name + " ( " + direct_url + " OR " + redirect_url + " OR " + page_url + " )");
    }

    private boolean tryDownload(String urlStr, File dest) throws IOException {
        if (urlStr != null && !urlStr.isEmpty()) {
            URL url = new URL(urlStr);
            var list =
                    RESOLVERS.entrySet().stream().filter(e -> e.getKey().test(url)).map(Map.Entry::getValue).toList();
            for (var f : list) {
                URL newUrl = f.apply(url);
                if (newUrl == null) continue;
                download(newUrl, dest);
                return true;
            }
        }
        return false;
    }

    private void download(URL url, File dest) throws IOException {
        if (dest.exists()) {
            System.err.println("File already exists: " + dest);
            return;
        }

        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        System.out.println("Successfully downloaded: " + name);
    }

    private static URL resolveMediaFire(URL url) {
        try {
            Element body = Jsoup.parse(url, 15000).body();
            Element downloadButton = body.getElementById("downloadButton");
            if (downloadButton != null) {
                String href = downloadButton.attr("href");
                return href.isBlank() ? null : new URL(href);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

}
