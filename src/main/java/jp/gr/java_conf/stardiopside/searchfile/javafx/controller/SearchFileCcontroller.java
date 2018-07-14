package jp.gr.java_conf.stardiopside.searchfile.javafx.controller;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

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
import javafx.scene.control.ButtonType;
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
    private ResourceBundle messages;
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
        this.messages = resources;

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
            alert.setContentText(messages.getString("message.directoryNotFound"));
            alert.showAndWait();
        } catch (PatternSyntaxException exc) {
            logger.log(Level.FINE, exc.getMessage(), exc);
            Alert alert = new Alert(AlertType.WARNING);
            alert.setHeaderText(messages.getString("message.searchConditionError"));
            alert.setContentText(exc.getMessage());
            alert.showAndWait();
        }
    }

    private void changedSearchingDirectory(ObservableValue<? extends Path> observable, Path oldValue, Path newValue) {
        if (newValue == null) {
            if (searcher.getResults().isEmpty()) {
                statusProperty.set(messages.getString("message.searchResult.empty"));
            } else {
                statusProperty.set(MessageFormat.format(messages.getString("message.searchResult.found"),
                        searcher.getResults().size()));
            }
        } else {
            statusProperty.set(MessageFormat.format(messages.getString("message.searchingDirectory"), newValue));
        }
    }

    @FXML
    private void onClearResults(ActionEvent e) {
        Alert alert = new Alert(AlertType.CONFIRMATION, messages.getString("message.clearResultConfirmation"),
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
            searcher.clear();
            statusProperty.set(messages.getString("message.clearResults"));
        });
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
        String result = searcher.getResults().stream().map(Path::toString)
                .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
        StringSelection selection = new StringSelection(result);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        statusProperty
                .set(MessageFormat.format(messages.getString("message.copyResults"), searcher.getResults().size()));
    }
}
