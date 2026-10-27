package group16.webapp.common.util;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class FileUtils {

    private FileUtils() {}

    /** "report.PDF" -> "pdf", "noext" -> "" */
    public static String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 || dot == filename.length() - 1 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Remove path separators and unsafe characters. */
    public static String sanitizeFilename(String filename) {
        if (filename == null) return "";
        String name = filename.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static boolean hasAllowedExtension(String filename, Set<String> allowed) {
        return allowed.contains(getExtension(filename));
    }

    /** Random, collision-free name that keeps the original extension. */
    public static String generateUniqueFilename(String originalFilename) {
        String ext = getExtension(originalFilename);
        return UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
    }
}
