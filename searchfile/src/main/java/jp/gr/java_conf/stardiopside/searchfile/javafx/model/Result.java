package jp.gr.java_conf.stardiopside.searchfile.javafx.model;

import java.nio.file.Path;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class Result {

    private final ReadOnlyObjectWrapper<Path> path;
    private final ObjectBinding<Path> fileName;
    private final ObjectBinding<Path> directoryName;
    private final StringBinding extension;

    public Result(Path path) {
        this.path = new ReadOnlyObjectWrapper<Path>(path);
        this.fileName = Bindings.createObjectBinding(() -> getPath().getFileName(), this.path);
        this.directoryName = Bindings.createObjectBinding(() -> getPath().getParent(), this.path);
        this.extension = Bindings.createStringBinding(() -> {
            String fileName = getPath().getFileName().toString();
            int index = fileName.lastIndexOf('.');
            return index == -1 ? "" : fileName.substring(index + 1);
        }, this.path);
    }

    public ReadOnlyObjectProperty<Path> pathProperty() {
        return path.getReadOnlyProperty();
    }

    public Path getPath() {
        return path.get();
    }

    public ObjectBinding<Path> fileNameBinding() {
        return fileName;
    }

    public Path getFileName() {
        return fileName.get();
    }

    public ObjectBinding<Path> directoryNameBinding() {
        return directoryName;
    }

    public Path getDirectoryName() {
        return directoryName.get();
    }

    public StringBinding extensionBinding() {
        return extension;
    }

    public String getExtension() {
        return extension.get();
    }
}
