module org.dockfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.web;
    requires java.desktop;
    requires java.logging;

    exports org.dockfx;
    exports org.dockfx.pane;
    exports org.dockfx.pane.skin;
    exports org.dockfx.viewControllers;

    opens org.dockfx.demo to javafx.graphics;
    opens org.dockfx to javafx.base, javafx.fxml;
}
