package de.femtopedia.util.mcadl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;

public class MCArchiveDownloader {

    private static final String SLUG = "modloader";

    public static void main(String[] args) throws Throwable {
        Gson gson = new GsonBuilder().create();
        Mod mod = gson.fromJson(new InputStreamReader(
                new BufferedInputStream(new URL("https://mcarchive.net/api/v1/mods/by_slug/" + SLUG)
                        .openConnection().getInputStream())), Mod.class);
        mod.downloadTo(Path.of("mcadl"));
    }

}
