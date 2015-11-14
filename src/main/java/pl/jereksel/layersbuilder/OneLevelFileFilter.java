package pl.jereksel.layersbuilder;

import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;

public class OneLevelFileFilter implements IOFileFilter {

    File parentFile;

    public OneLevelFileFilter(File parentFile) {
        this.parentFile = parentFile;
    }

    @Override
    public boolean accept(File file) {
        return file.getParentFile().equals(parentFile);
    }

    @Override
    public boolean accept(File dir, String name) {
        return dir.getParentFile().equals(parentFile);
    }
}
