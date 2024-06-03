package de.femtopedia;

import java.io.FileInputStream;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Properties;
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

        listFolder(s3, "", bucketName);
        s3.close();
    }

    public static void listFolder(S3Client s3, String folder, String bucketName) {
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
                System.out.println("\"" + obj.key() + "\" : " + readableFileSize(obj.size()));
            }

            SdkIterable<CommonPrefix> prefixes = res.commonPrefixes();
            for (var prefix : prefixes) {
                if (prefix.prefix().equals(folder)) continue;
                System.out.println("\"" + prefix.prefix() + "\"");
            }

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    /**
     * Taken from <a href="https://stackoverflow.com/a/5599842/5894824">StackOverflow</a>
     */
    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final int base = 1000;
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(base));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(base, digitGroups)) + " " + units[digitGroups];
    }

}
