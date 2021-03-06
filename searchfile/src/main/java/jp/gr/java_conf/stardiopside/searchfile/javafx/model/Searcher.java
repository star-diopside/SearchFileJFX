package jp.gr.java_conf.stardiopside.searchfile.javafx.model;

import java.awt.Desktop;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.stream.Collectors;

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
import javafx.collections.transformation.SortedList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Searcher implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(Searcher.class.getName());
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ObservableList<Result> results = FXCollections.observableArrayList();
    private ReadOnlyObjectWrapper<SortedList<Result>> sortedResults = new ReadOnlyObjectWrapper<>(
            new SortedList<>(results));
    private ReadOnlyBooleanWrapper searching = new ReadOnlyBooleanWrapper();
    private ReadOnlyObjectWrapper<Path> searchingDirectory = new ReadOnlyObjectWrapper<>();
    private volatile boolean cancelled = false;

    public ReadOnlyObjectProperty<SortedList<Result>> resultsProperty() {
        return sortedResults.getReadOnlyProperty();
    }

    public SortedList<Result> getResults() {
        return sortedResults.get();
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

        var pathMatcher = condition.getPathMatcher();
        results.clear();
        setSearching(true);
        cancelled = false;

        var visitor = new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Platform.runLater(() -> setSearchingDirectory(dir));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(file.getFileName())) {
                    Platform.runLater(() -> results.add(new Result(file)));
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
                return cancelled ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }
        };

        executorService.submit(new FutureTask<>(() -> {
            try {
                Files.walkFileTree(condition.getDirectory().toAbsolutePath().normalize(), visitor);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new UncheckedIOException(e);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw e;
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
        cancelled = true;
    }

    public void clear() {
        results.clear();
    }

    public RemoveResult moveToTrash(Collection<Path> files) {
        var deletedFiles = new ArrayList<Path>();
        var errorFiles = new ArrayList<Path>();

        var desktop = Desktop.getDesktop();
        var targetFiles = results.stream().map(Result::getPath).collect(Collectors.toSet());
        targetFiles.retainAll(files);
        targetFiles.forEach(file -> {
            if (desktop.moveToTrash(file.toFile())) {
                deletedFiles.add(file);
            } else {
                errorFiles.add(file);
            }
        });

        results.removeIf(result -> deletedFiles.contains(result.getPath()));
        return new RemoveResult(deletedFiles, errorFiles);
    }

    public RemoveResult delete(Collection<Path> files) {
        var deletedFiles = new ArrayList<Path>();
        var errorFiles = new ArrayList<Path>();

        var targetFiles = results.stream().map(Result::getPath).collect(Collectors.toSet());
        targetFiles.retainAll(files);
        targetFiles.forEach(file -> {
            try {
                Files.delete(file);
                deletedFiles.add(file);
            } catch (IOException ex) {
                logger.log(Level.WARNING, ex.getMessage(), ex);
                errorFiles.add(file);
            }
        });

        results.removeIf(result -> deletedFiles.contains(result.getPath()));
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
