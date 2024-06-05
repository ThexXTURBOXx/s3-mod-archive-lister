package de.femtopedia;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.bundled.CorsPluginConfig;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

public class Main {

    public static void main(String[] args) throws Throwable {
        Properties properties = new Properties();
        properties.load(new FileInputStream("config.properties"));

        String publicUri = properties.getProperty("public_uri");
        String bucketName = properties.getProperty("bucket_name");
        Region region = Region.of(properties.getProperty("region"));
        S3Client s3 = S3Client.builder()
                .endpointOverride(new URI(properties.getProperty("endpoint_uri")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .region(region)
                .build();

        Javalin javalin = Javalin.create(config -> config.bundledPlugins.enableCors(
                        container -> container.addRule(CorsPluginConfig.CorsRule::anyHost)))
                .get("*", ctx -> listPoint(ctx, s3, bucketName, publicUri))
                .start();

        Scanner s = new Scanner(System.in);
        s.nextLine();

        s3.close();
        javalin.stop();
    }

    public static void listPoint(Context ctx, S3Client s3, String bucketName, String publicUri) {
        String prefix = URLDecoder.decode(ctx.path(), StandardCharsets.UTF_8).trim();
        if (prefix.startsWith("/")) prefix = prefix.substring(1);
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";

        ctx.contentType("text/html");
        ctx.result(
                "<table>" +
                "<tr><th>File</th><th>Size</th><tr>" +
                listFolder(s3, prefix, bucketName, publicUri).stream()
                        .sorted(Comparator.comparing(ArchiveObject::isFile)
                                .thenComparing(o -> o.name().toLowerCase(), Comparator.naturalOrder()))
                        .map(ArchiveObject::asHTML)
                        .collect(Collectors.joining()) +
                "</table>");
    }

    public static Set<ArchiveObject> listFolder(S3Client s3, String folder, String bucketName, String publicUri) {
        Set<ArchiveObject> ret = new HashSet<>();

        try {
            ListObjectsV2Request listObjects = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .delimiter("/")
                    .prefix(folder)
                    .build();

            ListObjectsV2Iterable res = s3.listObjectsV2Paginator(listObjects);

            SdkIterable<S3Object> objects = res.contents();
            for (var obj : objects) {
                if (obj.key().equals(folder)) continue;
                ret.add(ArchiveObject.fromS3Object(obj, publicUri));
            }

            SdkIterable<CommonPrefix> prefixes = res.commonPrefixes();
            for (var prefix : prefixes) {
                if (prefix.prefix().equals(folder)) continue;
                ret.add(ArchiveObject.fromCommonPrefix(prefix));
            }
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }

        return ret;
    }

}
