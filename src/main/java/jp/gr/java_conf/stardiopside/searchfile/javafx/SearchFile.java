package jp.gr.java_conf.stardiopside.searchfile.javafx;

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
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("SearchFile.fxml"));
        Parent parent = loader.load();
        SearchFileCcontroller controller = loader.getController();
        controller.setStage(primaryStage);
        primaryStage.setScene(new Scene(parent));
        primaryStage.setTitle("ファイルの検索");
        primaryStage.show();
    }
}
