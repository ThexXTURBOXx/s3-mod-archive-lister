package de.femtopedia.util.cfdl;

import io.github.matyrobbrt.curseforgeapi.CurseForgeAPI;
import io.github.matyrobbrt.curseforgeapi.request.Requests;
import io.github.matyrobbrt.curseforgeapi.request.query.PaginationQuery;
import io.github.matyrobbrt.curseforgeapi.schemas.file.File;
import io.github.matyrobbrt.curseforgeapi.schemas.mod.Mod;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import org.apache.maven.artifact.versioning.ComparableVersion;

public class CFDownloader {

    private static final int MOD_ID = 251457;

    public static void main(String[] args) throws Throwable {
        Properties properties = new Properties();
        properties.load(new FileInputStream("config.properties"));

        final CurseForgeAPI cfApi = CurseForgeAPI.builder()
                .apiKey(properties.getProperty("cf_key"))
                .build();
        Mod mod = cfApi.makeRequest(Requests.getMod(MOD_ID)).get();
        Path modPath = Path.of("cfdl", mod.name());
        List<File> files = new ArrayList<>();
        for (int i = 0; ; ++i) {
            var fs = cfApi.makeRequest(Requests.getModFiles(MOD_ID, null, PaginationQuery.of(i * 50, 50))).get();
            if (fs.isEmpty()) break;
            files.addAll(fs);
        }
        for (File f : files) {
            Path versionPath = modPath.resolve(f.gameVersions().stream()
                    .filter(v -> v.matches("(?:[0-9]+\\.)+[0-9]+") || v.startsWith("Beta") || v.endsWith("-Snapshot"))
                    .map(v -> v.replaceFirst("Beta ", "b").replace("-Snapshot", ""))
                    .max(Comparator.comparing(v -> new ComparableVersion(v.replaceFirst("b", "")))).orElseThrow());
            Path filePath = versionPath.resolve(f.fileName());
            if (Files.exists(filePath)) {
                System.err.println("File already exists: " + filePath + " for " + f.displayName());
                continue;
            }
            f.download(filePath);
            System.out.println("Successfully downloaded: " + f.displayName() + " (" + f.fileName() + ")");
        }
    }

}
