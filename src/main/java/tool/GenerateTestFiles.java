package tool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class GenerateTestFiles {
    public static void main(String[] args) {

        String folderPath = "./src/documents/ls";
        int fileCount = 100;

        File folder = new File(folderPath);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                System.out.println("Folder created: " + folderPath);
            } else {
                System.err.println("Failed to create folder: " + folderPath);
                return;
            }
        }

        // 生成文件
        for (int i = 1; i <= fileCount; i++) {
            String fileName = "test_file_" + i + ".txt"; // 文件名
            File file = new File(folderPath, fileName);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(generateRandomContent(50));
                System.out.println("Generated file: " + file.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error writing to file: " + file.getAbsolutePath());
            }
        }
    }

    private static String generateRandomContent(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder content = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            content.append(characters.charAt(random.nextInt(characters.length())));
        }

        return content.toString();
    }
}

