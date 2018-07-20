package jp.gr.java_conf.stardiopside.searchfile.javafx.model;

import java.awt.Desktop;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Searcher implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(Searcher.class.getName());
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ObservableList<Path> results = FXCollections.observableArrayList();
    private ReadOnlyObjectWrapper<ObservableList<Path>> readOnlyResults = new ReadOnlyObjectWrapper<>(
            FXCollections.unmodifiableObservableList(results));
    private ReadOnlyBooleanWrapper searching = new ReadOnlyBooleanWrapper();
    private ReadOnlyObjectWrapper<Path> searchingDirectory = new ReadOnlyObjectWrapper<>();
    private volatile boolean isCancelled = false;

    public ReadOnlyObjectProperty<ObservableList<Path>> resultsProperty() {
        return readOnlyResults.getReadOnlyProperty();
    }

    public ObservableList<Path> getResults() {
        return readOnlyResults.get();
    }

    public ReadOnlyBooleanProperty searchingProperty() {
        return searching.getReadOnlyProperty();
    }

    public boolean isSearching() {
        return searching.get();
    }

    private void setSearching(boolean searching) {
        this.searching.set(searching);
    }

    public ReadOnlyObjectProperty<Path> searchingDirectoryProperty() {
        return searchingDirectory.getReadOnlyProperty();
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
            @Override
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

    public RemoveResult moveToTrash(Collection<Path> files) {
        ArrayList<Path> deletedFiles = new ArrayList<>();
        ArrayList<Path> errorFiles = new ArrayList<>();

        Desktop desktop = Desktop.getDesktop();
        files.forEach(file -> {
            if (desktop.moveToTrash(file.toFile())) {
                deletedFiles.add(file);
            } else {
                errorFiles.add(file);
            }
        });

        results.removeAll(deletedFiles);
        return new RemoveResult(deletedFiles, errorFiles);
    }

    public RemoveResult delete(Collection<Path> files) {
        ArrayList<Path> deletedFiles = new ArrayList<>();
        ArrayList<Path> errorFiles = new ArrayList<>();

        files.forEach(file -> {
            try {
                Files.delete(file);
                deletedFiles.add(file);
            } catch (IOException ex) {
                logger.log(Level.WARNING, ex.getMessage(), ex);
                errorFiles.add(file);
            }
        });

        results.removeAll(deletedFiles);
        return new RemoveResult(deletedFiles, errorFiles);
    }

    public static class RemoveResult {
        private List<Path> deletedFiles;
        private List<Path> errorFiles;

        private RemoveResult(List<Path> deletedFiles, List<Path> errorFiles) {
            this.deletedFiles = Collections.unmodifiableList(deletedFiles);
            this.errorFiles = Collections.unmodifiableList(errorFiles);
        }

        public List<Path> getDeletedFiles() {
            return deletedFiles;
        }

        public List<Path> getErrorFiles() {
            return errorFiles;
        }
    }
}
