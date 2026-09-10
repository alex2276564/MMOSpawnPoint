package uz.alex2276564.mmospawnpoint.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public final class ResourceUtils {

    private ResourceUtils() {
    }

    /**
     * Copy resource from classpath to file (overwrites if exists).
     */
    public static boolean copyResource(ClassLoader resourceLoader,
                                       Logger logger,
                                       String resourcePath,
                                       File destination) {
        try (InputStream inputStream = resourceLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                if (logger != null) {
                    logger.warning("Resource not found: " + resourcePath);
                }
                return false;
            }

            // Ensure parent directory exists
            if (destination.getParentFile() != null) {
                destination.getParentFile().mkdirs();
            }

            // Copy resource to file
            Files.copy(inputStream, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

            if (logger != null) {
                logger.info("Copied resource " + resourcePath + " to " + destination.getName());
            }
            return true;

        } catch (IOException e) {
            if (logger != null) {
                logger.warning("Failed to copy resource " + resourcePath + ": " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Copy resource to file only if destination doesn't exist.
     */
    public static boolean copyResourceIfNotExists(ClassLoader resourceLoader,
                                                  Logger logger,
                                                  String resourcePath,
                                                  File destination) {
        if (destination.exists()) {
            return false;
        }
        return copyResource(resourceLoader, logger, resourcePath, destination);
    }

    /**
     * Force update file from resource (always overwrites destination).
     */
    public static void updateFromResource(ClassLoader resourceLoader,
                                          Logger logger,
                                          String resourcePath,
                                          File destination) {
        copyResource(resourceLoader, logger, resourcePath, destination);
    }

    /**
     * Get resource content as string (UTF-8).
     */
    public static String getResourceAsString(ClassLoader resourceLoader,
                                             Logger logger,
                                             String resourcePath) {
        try (InputStream inputStream = resourceLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                if (logger != null) {
                    logger.warning("Resource not found: " + resourcePath);
                }
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
                return content.toString();
            }

        } catch (IOException e) {
            if (logger != null) {
                logger.warning("Failed to read resource " + resourcePath + ": " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Save string content to file (UTF-8).
     */
    public static boolean saveStringToFile(String content, File destination) {
        try {
            File parent = destination.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            Files.writeString(destination.toPath(), content, StandardCharsets.UTF_8);
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Copy resource to directory with same filename.
     */
    public static void copyResourceToDirectory(ClassLoader resourceLoader,
                                               Logger logger,
                                               String resourcePath,
                                               File directory) {
        String fileName = new File(resourcePath).getName();
        File destination = new File(directory, fileName);
        copyResource(resourceLoader, logger, resourcePath, destination);
    }

    /**
     * Copy multiple resources to directory.
     */
    public static void copyResources(ClassLoader resourceLoader,
                                     Logger logger,
                                     String[] resourcePaths,
                                     File directory) {
        for (String resourcePath : resourcePaths) {
            copyResourceToDirectory(resourceLoader, logger, resourcePath, directory);
        }
    }

    /**
     * Check if resource exists on classpath.
     */
    public static boolean resourceExists(ClassLoader resourceLoader, String resourcePath) {
        try (InputStream inputStream = resourceLoader.getResourceAsStream(resourcePath)) {
            return inputStream != null;
        } catch (IOException e) {
            return false;
        }
    }
}