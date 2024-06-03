package de.femtopedia.util.mcadl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record Mod(String name, String slug, List<ModVersion> mod_versions) {

    public void downloadTo(Path path) throws IOException {
        for (ModVersion version : mod_versions) {
            Path versionPath = path.resolve(name);
            Files.createDirectories(versionPath);
            version.downloadTo(versionPath);
        }
    }

}
