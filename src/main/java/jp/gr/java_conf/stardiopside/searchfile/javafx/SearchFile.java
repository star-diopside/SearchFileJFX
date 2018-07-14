package jp.gr.java_conf.stardiopside.searchfile.javafx;

import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jp.gr.java_conf.stardiopside.searchfile.javafx.controller.SearchFileCcontroller;

public class SearchFile extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        ResourceBundle messages = ResourceBundle.getBundle("messages");
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("SearchFile.fxml"), messages);
        Parent parent = loader.load();
        SearchFileCcontroller controller = loader.getController();
        controller.setStage(primaryStage);
        primaryStage.setScene(new Scene(parent));
        primaryStage.setTitle(messages.getString("SearchFile.title"));
        primaryStage.show();
    }
}
