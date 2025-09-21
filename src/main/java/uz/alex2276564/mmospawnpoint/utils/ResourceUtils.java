package uz.alex2276564.mmospawnpoint.utils;

import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ResourceUtils {

    /**
     * Copy resource to file (overwrites if exists)
     */
    public static boolean copyResource(MMOSpawnPoint plugin, String resourcePath, File destination) {
        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream == null) {
                plugin.getLogger().warning("Resource not found: " + resourcePath);
                return false;
            }

            // Ensure parent directory exists
            if (destination.getParentFile() != null) {
                destination.getParentFile().mkdirs();
            }

            // Copy resource to file
            Files.copy(inputStream, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

            plugin.getLogger().info("Copied resource " + resourcePath + " to " + destination.getName());
            return true;

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to copy resource " + resourcePath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Copy resource to file only if destination doesn't exist
     */
    public static boolean copyResourceIfNotExists(MMOSpawnPoint plugin, String resourcePath, File destination) {
        if (destination.exists()) {
            return false;
        }
        return copyResource(plugin, resourcePath, destination);
    }

    /**
     * Force update file from resource (always deletes old and copies new)
     */
    public static boolean updateFromResource(MMOSpawnPoint plugin, String resourcePath, File destination) {
        if (destination.exists()) {
            destination.delete();
        }
        return copyResource(plugin, resourcePath, destination);
    }

    /**
     * Get resource content as string
     */
    public static String getResourceAsString(MMOSpawnPoint plugin, String resourcePath) {
        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream == null) {
                plugin.getLogger().warning("Resource not found: " + resourcePath);
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read resource " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Save string content to file
     */
    public static boolean saveStringToFile(String content, File destination) {
        try {
            if (destination.getParentFile() != null) {
                destination.getParentFile().mkdirs();
            }

            Files.writeString(destination.toPath(), content, StandardCharsets.UTF_8);
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Copy resource to directory with same filename
     */
    public static boolean copyResourceToDirectory(MMOSpawnPoint plugin, String resourcePath, File directory) {
        String fileName = new File(resourcePath).getName();
        File destination = new File(directory, fileName);
        return copyResource(plugin, resourcePath, destination);
    }

    /**
     * Copy multiple resources to directory
     */
    public static void copyResources(MMOSpawnPoint plugin, String[] resourcePaths, File directory) {
        for (String resourcePath : resourcePaths) {
            copyResourceToDirectory(plugin, resourcePath, directory);
        }
    }

    /**
     * Check if resource exists
     */
    public static boolean resourceExists(MMOSpawnPoint plugin, String resourcePath) {
        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            return inputStream != null;
        } catch (IOException e) {
            return false;
        }
    }
}