package tool;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class example_file_generation {
    public static void main(String[] args) {
        String fileName = "./src/documents/wc/example.txt";
        int numLines = 1_000_000;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (int i = 1; i <= numLines; i++) {
                writer.write("Line " + i + ": This is a sample text line with some random words and a count number " + i + ".");
                writer.newLine();
            }
            System.out.println("File " + fileName + " generated successfully with " + numLines + " lines.");
        } catch (IOException e) {
            System.err.println("Error while writing the file: " + e.getMessage());
        }
    }
}
