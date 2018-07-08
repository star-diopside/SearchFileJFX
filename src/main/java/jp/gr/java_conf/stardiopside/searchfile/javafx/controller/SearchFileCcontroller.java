package jp.gr.java_conf.stardiopside.searchfile.javafx.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

import org.controlsfx.control.StatusBar;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import jp.gr.java_conf.stardiopside.searchfile.javafx.model.Condition;
import jp.gr.java_conf.stardiopside.searchfile.javafx.model.Searcher;
import jp.gr.java_conf.stardiopside.searchfile.javafx.util.PathStringConverter;

public class SearchFileCcontroller implements Initializable {

    private static final Logger logger = Logger.getLogger(SearchFileCcontroller.class.getName());
    private Condition condition = new Condition();
    private Searcher searcher = new Searcher();
    private StringProperty statusProperty = new SimpleStringProperty();

    private Stage stage;

    @FXML
    private TextField textDirectory;

    @FXML
    private Button buttonDirectory;

    @FXML
    private TextField textFileName;

    @FXML
    private ToggleGroup searchType;

    @FXML
    private Button buttonSearch;

    @FXML
    private Button buttonClearResults;

    @FXML
    private ListView<Path> foundFiles;

    @FXML
    private StatusBar statusBar;

    @FXML
    private Label osName;

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue.booleanValue() && !newValue.booleanValue()) {
                searcher.close();
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Bindings.bindBidirectional(textDirectory.textProperty(), condition.directoryProperty(),
                new PathStringConverter());
        condition.filePatternProperty().bind(textFileName.textProperty());
        condition.matchTypeProperty().bind(searchType.selectedToggleProperty());
        foundFiles.setItems(searcher.getResults());
        buttonDirectory.defaultButtonProperty().bind(textDirectory.focusedProperty());
        buttonSearch.textProperty().bind(Bindings.createStringBinding(() -> searcher.isSearching() ? "検索中止" : "検索開始",
                searcher.searchingProperty()));
        buttonSearch.defaultButtonProperty().bind(textDirectory.focusedProperty().not());
        buttonClearResults.disableProperty().bind(searcher.searchingProperty());
        statusBar.textProperty().bind(statusProperty);
        searcher.searchingDirectoryProperty().addListener(this::changedSearchingDirectory);
        osName.setText(System.getProperty("os.name"));
    }

    @FXML
    private void onChooseDirectory(ActionEvent e) {
        DirectoryChooser chooser = new DirectoryChooser();
        if (condition.getDirectory() != null && Files.isDirectory(condition.getDirectory())) {
            chooser.setInitialDirectory(condition.getDirectory().toFile());
        }
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            condition.setDirectory(dir.toPath());
        }
    }

    @FXML
    private void onSearch(ActionEvent e) {
        try {
            if (searcher.isSearching()) {
                searcher.cancel();
            } else {
                searcher.search(condition);
            }
        } catch (FileNotFoundException exc) {
            logger.log(Level.FINE, exc.getMessage(), exc);
            Alert alert = new Alert(AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("検索ディレクトリが存在しません。検索条件を見直してください。");
            alert.showAndWait();
        } catch (PatternSyntaxException exc) {
            logger.log(Level.FINE, exc.getMessage(), exc);
            Alert alert = new Alert(AlertType.WARNING);
            alert.setHeaderText("ファイル検索パターンに誤りがあります。検索条件を見直してください。");
            alert.setContentText(exc.getMessage());
            alert.showAndWait();
        }
    }

    private void changedSearchingDirectory(ObservableValue<? extends Path> observable, Path oldValue, Path newValue) {
        if (newValue == null) {
            if (searcher.getResults().isEmpty()) {
                statusProperty.set("ファイルが見つかりませんでした。");
            } else {
                statusProperty.set(searcher.getResults().size() + " 個のファイルが見つかりました。");
            }
        } else {
            statusProperty.set(newValue.toString() + " を検索中...");
        }
    }

    @FXML
    private void onClearResults(ActionEvent e) {
        searcher.clear();
    }

    @FXML
    private void onSelectAll(ActionEvent e) {
        logger.info(e.toString());
    }

    @FXML
    private void onClearSelection(ActionEvent e) {
        logger.info(e.toString());
    }

    @FXML
    private void onDeleteSelectedFile(ActionEvent e) {
        logger.info(e.toString());
    }

    @FXML
    private void onCopyResults(ActionEvent e) {
        logger.info(e.toString());
    }
}
