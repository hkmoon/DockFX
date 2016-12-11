package org.dockfx.pane;

import java.util.HashSet;
import java.util.Set;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPos;
import org.dockfx.demo.DockFX;

/**
 * Utility class to handle dock panes and undocked windows management.
 */
public class DockPaneManager {
    // load an image to caption the dock nodes
    private static final Image DOCK_IMG = new Image(DockFX.class.getResource("docknode.png").toExternalForm());

    private static final Set<DockPane> DOCK_PANES = new HashSet<>();
    private static final DockPane ROOT_PANE = new DockPane();

    public static DockPane rootPane() {
        return ROOT_PANE;
    }

    private static DockPane createDockPane() {
        final DockPane dockPane = new DockPane();
        DOCK_PANES.add(dockPane);
        return dockPane;
    }

    public static void createUndockedWindow(final DockNode dockNode) {
        undockedWindowImpl(dockNode).show();
    }

    private static Stage undockedWindowImpl(final DockNode dockNode) {
        final DockPane dockPane = DockPaneManager.createDockPane();
        final BorderPane borderPane = new BorderPane();
        borderPane.setCenter(dockPane);
        final Scene scene = new Scene(borderPane);
        final Stage stage = new Stage();
        stage.setScene(scene);

        borderPane.setPrefSize(dockNode.getStage().getWidth(), dockNode.getStage().getHeight());
        dockNode.dock(dockPane, DockPos.LEFT);
        stage.setX(dockNode.getStage().getX());
        stage.setY(dockNode.getStage().getY());
        stage.sizeToScene();
        return stage;
    }

    public static void checkEmptyPanes() {
        DOCK_PANES.forEach(dp -> {
            dp.getChildren().forEach(node -> {
                if (node instanceof ContentSplitPane) {
                    ContentSplitPane csp = (ContentSplitPane) node;
                    if (csp.getChildrenList().isEmpty()) {
                        final Stage stage = (Stage) dp.getScene().getWindow();
                        Platform.runLater(stage::close);
                    }
                }
            });
        });
    }
}