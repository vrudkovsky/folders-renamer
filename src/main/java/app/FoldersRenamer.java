/**
 * @autor Victor Rudkovsky
 * Created on 23.05.22023
 */

package app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class FoldersRenamer {
    public static void main(String[] args) {
        String FOLDER_PATH = args[0];
        renameSubfolders(FOLDER_PATH);
    }

    public static void renameSubfolders(String folderPath) {
        try {
            List<File> subfolders = Files.list(Paths.get(folderPath))
                    .parallel()
                    .map(Path::toFile)
                    .filter(File::isDirectory)
                    .collect(Collectors.toList());

            ForkJoinPool customThreadPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            customThreadPool.submit(() ->
                    subfolders.parallelStream()
                            .forEach(subfolder -> {
                                String prefix = subfolder.getName().substring(0, 1);
                                String newName = readXmlFile(subfolder, prefix);
                                if (newName != null) {
                                    renameSubfolder(subfolder, newName);
                                }
                            })
            ).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readXmlFile(File subfolder, String prefix) {
        try {
            File xmlFile;
            if (prefix.equals("b")) {
                xmlFile = getXmlFileB(subfolder);
            } else if (prefix.equals("e")) {
                xmlFile = getXmlFileE(subfolder);
            } else {
                return null;
            }

            Document document = Jsoup.parse(xmlFile, "UTF-8");
            Elements elements = document.select("attribute[type=title]");
            if (!elements.isEmpty()) {
                Element titleElement = elements.first();
                return titleElement.text();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static File getXmlFileB(File subfolder) {
        return new File(subfolder, "." + subfolder.getName() + ".xml");
    }

    private static File getXmlFileE(File subfolder) {
        Path parentPath = subfolder.getParentFile().toPath();
        return parentPath.resolve(subfolder.getName() + ".xml").toFile();
    }

    private static void renameSubfolder(File subfolder, String newName) {
        String oldName = subfolder.getName();
        File newSubfolder = new File(subfolder.getParent(), newName);
        if (subfolder.renameTo(newSubfolder)) {
            System.out.println("Renamed: " + oldName + " -> " + newName);
            renameSubfolders(newSubfolder.getAbsolutePath());
        } else {
            System.out.println("Failed to rename: " + oldName);
        }
    }
}
