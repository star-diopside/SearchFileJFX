package jp.gr.java_conf.stardiopside.searchfile.javafx.model;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Toggle;

public class Condition {

    private ObjectProperty<Path> directory = new SimpleObjectProperty<>(Paths.get(".").toAbsolutePath().getParent());
    private StringProperty filePattern = new SimpleStringProperty();
    private ObjectProperty<Toggle> matchType = new SimpleObjectProperty<>();

    public ObjectProperty<Path> directoryProperty() {
        return directory;
    }

    public Path getDirectory() {
        return directory.get();
    }

    public void setDirectory(Path directory) {
        this.directory.set(directory);
    }

    public StringProperty filePatternProperty() {
        return filePattern;
    }

    public String getFilePattern() {
        return filePattern.get();
    }

    public void setFilePattern(String filePattern) {
        this.filePattern.set(filePattern);
    }

    public ObjectProperty<Toggle> matchTypeProperty() {
        return matchType;
    }

    public FileNameMatchType getMatchType() {
        return FileNameMatchType.valueOf(matchType.get().getUserData().toString());
    }

    public PathMatcher getPathMatcher() {
        String pattern = getFilePattern();
        if (pattern == null || pattern.isEmpty()) {
            return path -> true;
        } else {
            return getMatchType().getPathMatcher(pattern);
        }
    }

    public enum FileNameMatchType {
        WILDCARD("glob:"), REGEX("regex:");

        private String syntax;

        private FileNameMatchType(String syntax) {
            this.syntax = syntax;
        }

        public PathMatcher getPathMatcher(String pattern) {
            return FileSystems.getDefault().getPathMatcher(syntax + pattern);
        }
    }
}