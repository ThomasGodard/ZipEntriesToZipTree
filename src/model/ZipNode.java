package model;

import java.util.List;
import java.util.zip.ZipEntry;

public record ZipNode (
    String name,
    List<ZipNode> children,
    ZipEntry entry){

    public boolean isDirectory() {
        return this.entry == null;
    }
}
