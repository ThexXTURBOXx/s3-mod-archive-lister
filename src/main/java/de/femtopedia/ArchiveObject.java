package de.femtopedia;

import java.text.DecimalFormat;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.S3Object;

public record ArchiveObject(String name, String url, String fileSize) {

    public static ArchiveObject fromS3Object(S3Object s3Object) {
        return new ArchiveObject(s3Object.key(), s3Object.key(), readableFileSize(s3Object.size()));
    }

    public static ArchiveObject fromCommonPrefix(CommonPrefix commonPrefix) {
        return new ArchiveObject(commonPrefix.prefix(), commonPrefix.prefix(), null);
    }

    /**
     * Taken from <a href="https://stackoverflow.com/a/5599842/5894824">StackOverflow</a>
     */
    public static String readableFileSize(Long size) {
        if (size == null || size <= 0) return "0";
        final int base = 1000;
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(base));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(base, digitGroups)) + " " + units[digitGroups];
    }

}
