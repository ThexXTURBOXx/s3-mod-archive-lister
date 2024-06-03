package de.femtopedia.util.mcadl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record ModVersion(List<ModFile> files, List<GameVersion> game_versions) {

    public void downloadTo(Path path) throws IOException {
        String version = game_versions.isEmpty() ? "Unsorted" : game_versions.get(game_versions.size() - 1).name();
        for (ModFile file : files) {
            Path versionPath = path.resolve(version);
            Files.createDirectories(versionPath);
            file.downloadTo(versionPath);
        }
    }

}
