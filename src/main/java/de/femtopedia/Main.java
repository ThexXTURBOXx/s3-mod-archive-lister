package de.femtopedia;

import io.javalin.Javalin;
import io.javalin.plugin.bundled.CorsPluginConfig;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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

        String bucketName = properties.getProperty("bucket_name");
        Region region = Region.of(properties.getProperty("region"));
        S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        properties.getProperty("access_key"),
                        properties.getProperty("secret_key"))))
                .endpointOverride(new URI(properties.getProperty("endpoint_uri")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .region(region)
                .build();

        Javalin javalin = Javalin.create(config -> config.bundledPlugins.enableCors(
                        container -> container.addRule(CorsPluginConfig.CorsRule::anyHost)))
                .get("/{prefix}", ctx -> ctx.result(
                        listFolder(s3, ctx.pathParam("prefix"), bucketName).toString()))
                .start();

        Scanner s = new Scanner(System.in);
        s.nextLine();

        s3.close();
        javalin.stop();
    }

    public static List<ArchiveObject> listFolder(S3Client s3, String folder, String bucketName) {
        List<ArchiveObject> ret = new ArrayList<>();

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
                ret.add(ArchiveObject.fromS3Object(obj));
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
