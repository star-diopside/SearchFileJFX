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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import jp.gr.java_conf.stardiopside.searchfile.javafx.model.Condition;
import jp.gr.java_conf.stardiopside.searchfile.javafx.model.Searcher;
import jp.gr.java_conf.stardiopside.searchfile.javafx.model.Searcher.RemoveResult;
import jp.gr.java_conf.stardiopside.searchfile.javafx.util.PathStringConverter;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SearchFileCcontroller implements Initializable {

    private static final Logger logger = Logger.getLogger(SearchFileCcontroller.class.getName());
    private final Condition condition;
    private final Searcher searcher;
    private ResourceBundle messages;
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
    private CheckBox checkMoveToTrash;

    @FXML
    private ListView<Path> foundFiles;

    @FXML
    private StatusBar statusBar;

    @FXML
    private Label osName;

    public SearchFileCcontroller(Condition condition, Searcher searcher) {
        this.condition = condition;
        this.searcher = searcher;
    }

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
        foundFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        foundFiles.itemsProperty().bind(searcher.resultsProperty());
        buttonDirectory.defaultButtonProperty().bind(textDirectory.focusedProperty());
        buttonSearch.textProperty().bind(Bindings.createStringBinding(() -> messages.getString(
                searcher.isSearching() ? "SearchFile.buttonSearch.text.stop" : "SearchFile.buttonSearch.text.start"),
                searcher.searchingProperty()));
        buttonSearch.defaultButtonProperty().bind(textDirectory.focusedProperty().not());
        buttonClearResults.disableProperty().bind(searcher.searchingProperty());
        statusBar.textProperty().bind(statusProperty);
        searcher.searchingDirectoryProperty().addListener(this::changedSearchingDirectory);
        osName.setText(System.getProperty("os.name"));
    }

    @FXML
    private void onApplicationExit(ActionEvent e) {
        stage.close();
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
        Alert alert = new Alert(AlertType.CONFIRMATION, messages.getString("message.clearResultsConfirmation"),
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
            searcher.clear();
            statusProperty.set(messages.getString("message.clearResults"));
        });
    }

    @FXML
    private void onSelectAll(ActionEvent e) {
        foundFiles.getSelectionModel().selectAll();
    }

    @FXML
    private void onClearSelection(ActionEvent e) {
        foundFiles.getSelectionModel().clearSelection();
    }

    @FXML
    private void onDeleteSelectedFile(ActionEvent e) {
        String messageKey = checkMoveToTrash.isSelected() ? "message.deleteSelectedFileConfirmation.moveToTrash"
                : "message.deleteSelectedFileConfirmation.delete";
        Alert alert = new Alert(AlertType.CONFIRMATION, messages.getString(messageKey), ButtonType.OK,
                ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
            RemoveResult result;
            if (checkMoveToTrash.isSelected()) {
                result = searcher.moveToTrash(foundFiles.getSelectionModel().getSelectedItems());
            } else {
                result = searcher.delete(foundFiles.getSelectionModel().getSelectedItems());
            }

            String resultMessage = MessageFormat.format(messages.getString("message.deleteSelectedFile.success"),
                    result.getDeletedFiles().size());
            if (!result.getErrorFiles().isEmpty()) {
                resultMessage += MessageFormat.format(messages.getString("message.deleteSelectedFile.error"),
                        result.getErrorFiles().size());
            }
            statusProperty.set(resultMessage);
        });
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
