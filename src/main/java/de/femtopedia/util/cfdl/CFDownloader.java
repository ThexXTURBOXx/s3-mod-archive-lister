package de.femtopedia.util.cfdl;

import io.github.matyrobbrt.curseforgeapi.CurseForgeAPI;
import io.github.matyrobbrt.curseforgeapi.request.Requests;
import io.github.matyrobbrt.curseforgeapi.request.helper.RequestHelper;
import io.github.matyrobbrt.curseforgeapi.schemas.mod.Mod;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
        final RequestHelper helper = new RequestHelper(cfApi);
        Mod mod = cfApi.makeRequest(Requests.getMod(MOD_ID)).get();
        Path modPath = Path.of("cfdl", mod.name());
        helper.listModFiles(mod).get().forEachRemaining(f -> {
            Path versionPath = modPath.resolve(f.gameVersions().stream()
                    .filter(v -> v.matches("(?:[0-9]+\\.)+[0-9]+") || v.startsWith("Beta") || v.endsWith("-Snapshot"))
                    .map(v -> v.replaceFirst("Beta ", "b").replace("-Snapshot", ""))
                    .max(Comparator.comparing(v -> new ComparableVersion(v.replaceFirst("b", "")))).orElseThrow());
            Path filePath = versionPath.resolve(f.fileName());
            if (Files.exists(filePath)) {
                System.err.println("File already exists: " + filePath + " for " + f.displayName());
                return;
            }
            try {
                f.download(filePath);
            } catch (IOException e) {
                System.err.println("Error downloading file: " + filePath + " for " + f.displayName());
            }
            System.out.println("Successfully downloaded: " + f.displayName() + " (" + f.fileName() + ")");
        });
    }

}
