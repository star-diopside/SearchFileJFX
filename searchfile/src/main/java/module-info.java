module jp.gr.java_conf.stardiopside.searchfile.javafx {
    opens jp.gr.java_conf.stardiopside.searchfile.javafx;
    opens jp.gr.java_conf.stardiopside.searchfile.javafx.controller;
    opens jp.gr.java_conf.stardiopside.searchfile.javafx.model;

    requires java.desktop;
    requires java.logging;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires spring.beans;
    requires spring.boot.autoconfigure;
    requires spring.boot;
    requires spring.context;
    requires spring.core;
}
