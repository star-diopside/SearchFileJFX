package jp.gr.java_conf.stardiopside.searchfile.javafx.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Searcher implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(Searcher.class.getName());
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ObservableList<Path> results = FXCollections.observableArrayList();
    private ObservableList<Path> readOnlyResults = FXCollections.unmodifiableObservableList(results);
    private BooleanProperty searching = new SimpleBooleanProperty();
    private ObjectProperty<Path> searchingDirectory = new SimpleObjectProperty<>();
    private volatile boolean isCancelled = false;

    public ObservableList<Path> getResults() {
        return readOnlyResults;
    }

    public ReadOnlyBooleanProperty searchingProperty() {
        return searching;
    }

    public boolean isSearching() {
        return searching.get();
    }

    private void setSearching(boolean searching) {
        this.searching.set(searching);
    }

    public ReadOnlyObjectProperty<Path> searchingDirectoryProperty() {
        return searchingDirectory;
    }

    public Path getSearchingDirectory() {
        return searchingDirectory.get();
    }

    private void setSearchingDirectory(Path searchingDirectory) {
        this.searchingDirectory.set(searchingDirectory);
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    public void search(Condition condition) throws FileNotFoundException, PatternSyntaxException {
        if (isSearching()) {
            return;
        }

        if (condition.getDirectory() == null || !Files.isDirectory(condition.getDirectory())) {
            throw new FileNotFoundException(String.valueOf(condition.getDirectory()));
        }

        PathMatcher pathMatcher = condition.getPathMatcher();
        results.clear();
        setSearching(true);
        isCancelled = false;

        FileVisitor<Path> visitor = new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Platform.runLater(() -> setSearchingDirectory(dir));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(file.getFileName())) {
                    Platform.runLater(() -> results.add(file));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                logger.log(Level.WARNING, exc.getMessage(), exc);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    logger.log(Level.WARNING, exc.getMessage(), exc);
                }
                return isCancelled ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }
        };

        executorService.submit(new FutureTask<>(() -> {
            try {
                Files.walkFileTree(condition.getDirectory(), visitor);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new UncheckedIOException(e);
            } finally {
                Platform.runLater(() -> setSearchingDirectory(null));
            }
        }, null) {
            protected void done() {
                Platform.runLater(() -> setSearching(false));
            };
        });
    }

    public void cancel() {
        isCancelled = true;
    }

    public void clear() {
        results.clear();
    }
}
