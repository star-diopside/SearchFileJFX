package jp.gr.java_conf.stardiopside.searchfile.javafx;

import java.util.ResourceBundle;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jp.gr.java_conf.stardiopside.searchfile.javafx.controller.SearchFileCcontroller;

@SpringBootApplication
public class SearchFile extends Application {

    private ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        applicationContext = new SpringApplicationBuilder(getClass()).headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        ResourceBundle messages = ResourceBundle.getBundle("messages");
        FXMLLoader loader = new FXMLLoader(applicationContext.getResource("classpath:SearchFile.fxml").getURL(),
                messages);
        loader.setControllerFactory(applicationContext::getBean);
        Parent parent = loader.load();
        SearchFileCcontroller controller = loader.getController();
        controller.setStage(primaryStage);
        primaryStage.setScene(new Scene(parent));
        primaryStage.setTitle(messages.getString("SearchFile.title"));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        applicationContext.close();
    }
}
