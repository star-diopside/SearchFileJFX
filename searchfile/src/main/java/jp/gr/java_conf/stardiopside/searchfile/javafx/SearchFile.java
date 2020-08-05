package jp.gr.java_conf.stardiopside.searchfile.javafx;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlsfx.dialog.ExceptionDialog;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jp.gr.java_conf.stardiopside.searchfile.javafx.controller.SearchFileController;

@SpringBootApplication
public class SearchFile extends Application {

    private static final Logger logger = Logger.getLogger(SearchFile.class.getName());
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
        var messages = ResourceBundle.getBundle("messages");

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            logger.log(Level.SEVERE, e.getMessage(), e);
            var dialog = new ExceptionDialog(e);
            dialog.setHeaderText(messages.getString("message.uncaughtException"));
            dialog.show();
        });

        var loader = new FXMLLoader(applicationContext.getResource("classpath:SearchFile.fxml").getURL(), messages);
        loader.setControllerFactory(applicationContext::getBean);
        Parent parent = loader.load();
        SearchFileController controller = loader.getController();
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
