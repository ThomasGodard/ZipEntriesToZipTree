package utils;

import model.ZipNode;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtils {

    private static final int THRESHOLD_ENTRIES = 10000;
    private static final int THRESHOLD_SIZE = 1000000000; //1GB, uncompressed
    private static final double THRESHOLD_RATIO = 10.0;

    private ZipUtils() {}

    /**
     * Transform a ZipFile into a list of model.ZipNode
     *
     * @param file zip file to convert int list of zip node
     * @return List of zip node
     * @throws IOException if zip file corrupt or exceed thresholds
     */
    public static List<ZipNode> fromZipFile(ZipFile file) throws IOException {
        return process(file);
    }

    private static List<ZipNode> process(ZipFile zipFile) throws IOException {
        List<ZipNode> root = new ArrayList<>();
        var entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();

            zipFileSafetyCheck(zipFile, entry);

            // Split on '/' OR '\', for malformated zip
            var names = entry.getName().split("[\\\\/]");
            var refNode = root;
            for (String name : names) {
                var actualNode = refNode.stream().filter(node -> node.name().equals(name)).findFirst().orElse(null);
                if (actualNode == null) {
                    var isDirectory = entry.isDirectory() || !name.equals(names[names.length - 1]);
                    actualNode = new ZipNode(name, new ArrayList<>(), isDirectory ? null : entry);
                    refNode.add(actualNode);
                }
                refNode = actualNode.children();
            }
        }
        return root;
    }

    /**
     * Safety check, see Sonar rule java:S5042
     *
     * @param zipFile zipFile
     * @param entry entry
     * @throws IOException if thresholds exceed
     */
    private static void zipFileSafetyCheck(ZipFile zipFile, ZipEntry entry) throws IOException {
        int totalSizeArchive = 0;
        int totalEntryArchive = 0;

        try (InputStream in = new BufferedInputStream(zipFile.getInputStream(entry))) {
            totalEntryArchive++;
            int nBytes;
            byte[] buffer = new byte[2048];
            int totalSizeEntry = 0;
            while ((nBytes = in.read(buffer)) > 0) {
                totalSizeEntry += nBytes;
                totalSizeArchive += nBytes;
                double compressionRatio = (double) totalSizeEntry / entry.getCompressedSize();
                if (compressionRatio > THRESHOLD_RATIO) {
                    throw new IOException("ratio between compressed and uncompressed data is highly suspicious, looks like a Zip Bomb Attack");
                }
            }
            if (totalSizeArchive > THRESHOLD_SIZE) {
                throw new IOException("the uncompressed data size is too much for the application resource capacity");
            }
            if (totalEntryArchive > THRESHOLD_ENTRIES) {
                throw new IOException("too much entries in this archive, can lead to inodes exhaustion of the system");
            }
        }
    }
}
