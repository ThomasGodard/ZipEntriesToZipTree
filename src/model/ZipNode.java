package model;

import java.util.ArrayList;
import java.util.zip.ZipEntry;

public record ZipNode (
    String name,
    ArrayList<ZipNode> children,
    ZipEntry entry){

    public boolean isDirectory() {
        return this.entry == null;
    }
}
