/**
 * @file DockPane.java
 * @brief Class implementing a generic dock pane for the layout of dock nodes.
 *
 * @section License
 *
 * This file is a part of the DockFX Library. Copyright (C) 2015 Robert B. Colton
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.dockfx;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import com.sun.javafx.css.StyleManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javafx.stage.Stage;

import org.dockfx.pane.ContentPane;
import org.dockfx.pane.ContentSplitPane;
import org.dockfx.pane.ContentTabPane;
import org.dockfx.pane.DockNodeTab;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * Base class for a dock pane that provides the layout of the dock nodes. Stacking the dock nodes to
 * the center in a TabPane will be added in a future release. For now the DockPane uses the relative
 * sizes of the dock nodes and lays them out in a tree of SplitPanes.
 *
 * @since DockFX 0.1
 */
public class DockPane extends StackPane implements EventHandler<DockEvent> {
    /**
     * The style this dock node should use on its stage when set to floating.
     */
    private StageStyle stageStyle = StageStyle.TRANSPARENT;
    /**
     * The stage that this dock node is currently using when floating.
     */
    private Stage stage;
    /**
     * The title bar that implements our dragging and state manipulation.
     */
    private DockPaneTitleBar dockTitleBar;
    /**
     * The border pane used when floating to provide a styled custom border.
     */
    private final BorderPane borderPane;
    private final DockPane parentDockPane;

    /**
     * The current root node of this dock pane's layout.
     */
    private Control root;

    /**
     * Whether a DOCK_ENTER event has been received by this dock pane since the last DOCK_EXIT event
     * was received.
     */
    private boolean receivedEnter = false;

    /**
     * The current node in this dock pane that we may be dragging over.
     */
    private Node dockNodeDrag;
    /**
     * The docking area of the current dock indicator button if any is selected. This is either the
     * root or equal to dock node drag.
     */
    private Node dockAreaDrag;
    /**
     * The docking position of the current dock indicator button if any is selected.
     */
    private DockPos dockPosDrag;

    /**
     * The docking area shape with a dotted animated border on the indicator overlay popup.
     */
    private Rectangle dockAreaIndicator;
    /**
     * The timeline used to animate the borer of the docking area indicator shape. Because JavaFX
     * has no CSS styling for timelines/animations yet we will make this private and offer an
     * accessor for the user to programmatically modify the animation or disable it.
     */
    private Timeline dockAreaStrokeTimeline;
    /**
     * The popup used to display the root dock indicator buttons and the docking area indicator.
     */
    private Popup dockIndicatorOverlay;

    /**
     * The grid pane used to lay out the local dock indicator buttons. This is the grid used to lay
     * out the buttons in the circular indicator.
     */
    private GridPane dockPosIndicator;
    /**
     * The popup used to display the local dock indicator buttons. This allows these indicator
     * buttons to be displayed outside the window of this dock pane.
     */
    private Popup dockIndicatorPopup;

    /**
     * CSS pseudo class selector representing whether this node is currently floating.
     */
    private static final PseudoClass FLOATING_PSEUDO_CLASS = PseudoClass.getPseudoClass("floating");
    /**
     * CSS pseudo class selector representing whether this node is currently docked.
     */
    private static final PseudoClass DOCKED_PSEUDO_CLASS = PseudoClass.getPseudoClass("docked");
    /**
     * CSS pseudo class selector representing whether this node is currently maximized.
     */
    private static final PseudoClass MAXIMIZED_PSEUDO_CLASS = PseudoClass.getPseudoClass("maximized");

    /**
     * Boolean property maintaining whether this node is currently maximized.
     *
     * @defaultValue false
     */
    private BooleanProperty maximizedProperty = new SimpleBooleanProperty(false) {

        @Override
        protected void invalidated() {
            DockPane.this.pseudoClassStateChanged(MAXIMIZED_PSEUDO_CLASS, get());
            if (borderPane != null) {
                borderPane.pseudoClassStateChanged(MAXIMIZED_PSEUDO_CLASS, get());
            }

            stage.setMaximized(get());

            // TODO: This is a work around to fill the screen bounds and not overlap the task bar when 
            // the window is undecorated as in Visual Studio. A similar work around needs applied for 
            // JFrame in Swing. http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4737788
            // Bug report filed:
            // https://bugs.openjdk.java.net/browse/JDK-8133330
            if (this.get()) {
                Screen screen = Screen
                        .getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                        .get(0);
                Rectangle2D bounds = screen.getVisualBounds();

                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());

                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
            }
        }

        @Override
        public String getName() {
            return "maximized";
        }
    };

    /**
     * Whether the pane is currently maximized.
     *
     * @param maximized Whether the pane is currently maximized.
     */
    public final void setMaximized(boolean maximized) {
        maximizedProperty.set(maximized);
    }

    public final BooleanProperty maximizedProperty() {
        return maximizedProperty;
    }

    public final boolean isMaximized() {
        return maximizedProperty.get();
    }

    /**
     * Object property maintaining bidirectional state of the caption graphic for this node with the
     * dock title bar or stage.
     *
     * @defaultValue null
     */
    public final ObjectProperty<Node> graphicProperty() {
        return graphicProperty;
    }

    private ObjectProperty<Node> graphicProperty = new SimpleObjectProperty<Node>() {
        @Override
        public String getName() {
            return "graphic";
        }
    };

    public final Node getGraphic() {
        return graphicProperty.get();
    }

    public final void setGraphic(Node graphic) {
        this.graphicProperty.setValue(graphic);
    }

    /**
     * Boolean property maintaining bidirectional state of the caption title for this node with the
     * dock title bar or stage.
     *
     * @defaultValue "Dock"
     */
    public final StringProperty titleProperty() {
        return titleProperty;
    }

    private StringProperty titleProperty = new SimpleStringProperty("Dock Title Bar") {
        @Override
        public String getName() {
            return "title";
        }
    };

    public final String getTitle() {
        return titleProperty.get();
    }

    public final void setTitle(String title) {
        this.titleProperty.setValue(title);
    }

    /**
     * Boolean property maintaining whether this pane is currently closed.
     */
    public final BooleanProperty closedProperty() {
        return closedProperty;
    }

    private BooleanProperty closedProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
        }

        @Override
        public String getName() {
            return "closed";
        }
    };

    public final boolean isClosed() {
        return closedProperty.get();
    }

    /**
     * Base class for a dock indicator button that allows it to be displayed during a dock event and
     * continue to receive input.
     *
     * @since DockFX 0.1
     */
    public class DockPosButton extends Button {

        /**
         * Whether this dock indicator button is used for docking a node relative to the root of the
         * dock pane.
         */
        private boolean dockRoot = true;
        /**
         * The docking position indicated by this button.
         */
        private DockPos dockPos = DockPos.CENTER;

        /**
         * Creates a new dock indicator button.
         */
        public DockPosButton(boolean dockRoot, DockPos dockPos) {
            super();
            this.dockRoot = dockRoot;
            this.dockPos = dockPos;
        }

        /**
         * Whether this dock indicator button is used for docking a node relative to the root of the
         * dock pane.
         *
         * @param dockRoot Whether this indicator button is used for docking a node relative to the
         * root of the dock pane.
         */
        public final void setDockRoot(boolean dockRoot) {
            this.dockRoot = dockRoot;
        }

        /**
         * The docking position indicated by this button.
         *
         * @param dockPos The docking position indicated by this button.
         */
        public final void setDockPos(DockPos dockPos) {
            this.dockPos = dockPos;
        }

        /**
         * The docking position indicated by this button.
         *
         * @return The docking position indicated by this button.
         */
        public final DockPos getDockPos() {
            return dockPos;
        }

        /**
         * Whether this dock indicator button is used for docking a node relative to the root of the
         * dock pane.
         *
         * @return Whether this indicator button is used for docking a node relative to the root of
         * the dock pane.
         */
        public final boolean isDockRoot() {
            return dockRoot;
        }
    }

    /**
     * A collection used to manage the indicator buttons and automate hit detection during DOCK_OVER
     * events.
     */
    private final ObservableList<DockPosButton> dockPosButtons;

    private final ObservableList<DockPane> floatingDockPanes;

    /**
     * Creates a new DockPane adding event handlers for dock events and creating the indicator
     * overlays.
     */
    public DockPane() {
        this(null);
    }

    protected DockPane(DockPane parentDockPane) {
        super();

        this.parentDockPane = parentDockPane;

        this.addEventHandler(DockEvent.ANY, this);
        this.addEventFilter(DockEvent.ANY, new EventHandler<DockEvent>() {

            @Override
            public void handle(DockEvent event) {

                if (event.getEventType() == DockEvent.DOCK_ENTER) {
                    DockPane.this.receivedEnter = true;
                } else if (event.getEventType() == DockEvent.DOCK_OVER) {
                    DockPane.this.dockNodeDrag = null;
                }
            }
        });

        dockIndicatorPopup = new Popup();
        dockIndicatorPopup.setAutoFix(false);

        dockIndicatorOverlay = new Popup();
        dockIndicatorOverlay.setAutoFix(false);

        StackPane dockRootPane = new StackPane();
        dockRootPane.prefWidthProperty().bind(this.widthProperty());
        dockRootPane.prefHeightProperty().bind(this.heightProperty());

        dockAreaIndicator = new Rectangle();
        dockAreaIndicator.setManaged(false);
        dockAreaIndicator.setMouseTransparent(true);

        dockAreaStrokeTimeline = new Timeline();
        dockAreaStrokeTimeline.setCycleCount(Timeline.INDEFINITE);
        // 12 is the cumulative offset of the stroke dash array in the default.css style sheet
        // RFE filed for CSS styled timelines/animations:
        // https://bugs.openjdk.java.net/browse/JDK-8133837
        KeyValue kv = new KeyValue(dockAreaIndicator.strokeDashOffsetProperty(), 12);
        KeyFrame kf = new KeyFrame(Duration.millis(500), kv);
        dockAreaStrokeTimeline.getKeyFrames().add(kf);
        dockAreaStrokeTimeline.play();

        DockPosButton dockCenter = new DockPosButton(false, DockPos.CENTER);
        dockCenter.getStyleClass().add("dock-center");

        DockPosButton dockTop = new DockPosButton(false, DockPos.TOP);
        dockTop.getStyleClass().add("dock-top");
        DockPosButton dockRight = new DockPosButton(false, DockPos.RIGHT);
        dockRight.getStyleClass().add("dock-right");
        DockPosButton dockBottom = new DockPosButton(false, DockPos.BOTTOM);
        dockBottom.getStyleClass().add("dock-bottom");
        DockPosButton dockLeft = new DockPosButton(false, DockPos.LEFT);
        dockLeft.getStyleClass().add("dock-left");

        DockPosButton dockTopRoot = new DockPosButton(true, DockPos.TOP);
        StackPane.setAlignment(dockTopRoot, Pos.TOP_CENTER);
        dockTopRoot.getStyleClass().add("dock-top-root");

        DockPosButton dockRightRoot = new DockPosButton(true, DockPos.RIGHT);
        StackPane.setAlignment(dockRightRoot, Pos.CENTER_RIGHT);
        dockRightRoot.getStyleClass().add("dock-right-root");

        DockPosButton dockBottomRoot = new DockPosButton(true, DockPos.BOTTOM);
        StackPane.setAlignment(dockBottomRoot, Pos.BOTTOM_CENTER);
        dockBottomRoot.getStyleClass().add("dock-bottom-root");

        DockPosButton dockLeftRoot = new DockPosButton(true, DockPos.LEFT);
        StackPane.setAlignment(dockLeftRoot, Pos.CENTER_LEFT);
        dockLeftRoot.getStyleClass().add("dock-left-root");

        // TODO: dockCenter goes first when tabs are added in a future version
        dockPosButtons
                = FXCollections.observableArrayList(dockCenter, dockTop, dockRight, dockBottom, dockLeft,
                        dockTopRoot, dockRightRoot, dockBottomRoot, dockLeftRoot);

        dockPosIndicator = new GridPane();
        dockPosIndicator.add(dockTop, 1, 0);
        dockPosIndicator.add(dockRight, 2, 1);
        dockPosIndicator.add(dockBottom, 1, 2);
        dockPosIndicator.add(dockLeft, 0, 1);
        dockPosIndicator.add(dockCenter, 1, 1);

        dockRootPane.getChildren().addAll(dockAreaIndicator, dockTopRoot, dockRightRoot, dockBottomRoot,
                dockLeftRoot);

        dockIndicatorOverlay.getContent().add(dockRootPane);
        dockIndicatorPopup.getContent().addAll(dockPosIndicator);

        this.getStyleClass().add("dock-pane");
        dockRootPane.getStyleClass().add("dock-root-pane");
        dockPosIndicator.getStyleClass().add("dock-pos-indicator");
        dockAreaIndicator.getStyleClass().add("dock-area-indicator");

        if (null == parentDockPane) {
            // we must abe a root pane
            floatingDockPanes = FXCollections.observableArrayList();
        } else {
            floatingDockPanes = parentDockPane.floatingDockPanes;
            floatingDockPanes.add(this);
        }

        borderPane = new BorderPane();
        borderPane.getStyleClass().add("dock-node-border");
        dockTitleBar = new DockPaneTitleBar(this);
        borderPane.setTop(dockTitleBar);
    }

    /**
     * The Timeline used to animate the docking area indicator in the dock indicator overlay for
     * this dock pane.
     *
     * @return The Timeline used to animate the docking area indicator in the dock indicator overlay
     * for this dock pane.
     */
    public final Timeline getDockAreaStrokeTimeline() {
        return dockAreaStrokeTimeline;
    }

    public DockPaneTitleBar getDockTitleBar() {
        return dockTitleBar;
    }

    /**
     * The stage style that will be used when the dock node is floating. This must be set prior to
     * setting the dock node to floating.
     *
     * @param stageStyle The stage style that will be used when the node is floating.
     */
    public void setStageStyle(StageStyle stageStyle) {
        this.stageStyle = stageStyle;
    }

    public StageStyle getStageStyle() {
        return this.stageStyle;
    }

    public void close() {
        floatingDockPanes.remove(this);
        this.closedProperty.set(true);
        this.stage.close();
    }

    /**
     * The stage associated with this dock pane. Can be null if the dock pane was never set to
     * floating.
     *
     * @return The stage associated with this pane.
     */
    public final Stage getStage() {
        return stage;
    }

    public final void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * The border pane used to parent this dock pane when floating. Can be null if the dock pane was
     * never set to floating.
     *
     * @return The border pane associated with this pane.
     */
    public final BorderPane getBorderPane() {
        return borderPane;
    }

    /**
     * Boolean property maintaining whether this node is currently floating.
     *
     * @defaultValue false
     */
    public final BooleanProperty floatingProperty() {
        return floatingProperty;
    }

    private BooleanProperty floatingProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            DockPane.this.pseudoClassStateChanged(FLOATING_PSEUDO_CLASS, get());
            if (borderPane != null) {
                borderPane.pseudoClassStateChanged(FLOATING_PSEUDO_CLASS, get());
            }
        }

        @Override
        public String getName() {
            return "floating";
        }
    };

    public final boolean isFloating() {
        return floatingProperty.get();
    }

    /**
     * Helper function to retrieve the URL of the default style sheet used by DockFX.
     *
     * @return The URL of the default style sheet used by DockFX.
     */
    public final static String getDefaultUserAgentStyleheet() {
        return DockPane.class.getResource("default.css").toExternalForm();
    }

    /**
     * Helper function to add the default style sheet of DockFX to the user agent style sheets.
     */
    public final static void initializeDefaultUserAgentStylesheet() {
        StyleManager.getInstance()
                .addUserAgentStylesheet(DockPane.class.getResource("default.css").toExternalForm());
    }

    /**
     * Boolean property maintaining whether this node is currently resizable.
     *
     * @defaultValue true
     */
    public final BooleanProperty resizableProperty() {
        return stageResizableProperty;
    }

    private BooleanProperty stageResizableProperty = new SimpleBooleanProperty(true) {
        @Override
        public String getName() {
            return "resizable";
        }
    };

    public final boolean isStageResizable() {
        return stageResizableProperty.get();
    }

    public final void setStageResizable(boolean resizable) {
        stageResizableProperty.set(resizable);
    }

    public final boolean isOnlyChild(DockNode node) {
        return null != root
                && (root instanceof ContentSplitPane)
                && 1 == ((ContentSplitPane) root).getChildrenList().size()
                && ((ContentSplitPane) root).getChildrenList().contains(node);
    }

    public final DockNode getOnlyChild() {
        return (null != root
                && (root instanceof ContentSplitPane)
                && 1 == ((ContentSplitPane) root).getChildrenList().size())
                && ((ContentSplitPane) root).getChildrenList().get(0) instanceof DockNode
                ? (DockNode) ((ContentSplitPane) root).getChildrenList().get(0) : null;
    }

    /**
     * Whether the node is currently floating.
     *
     * @param translation null The offset of the node after being set floating. Used for aligning it
     * with its layout bounds inside the dock pane when it becomes detached. Can be null indicating
     * no translation.
     */
    public void setFloating(Region content, Point2D translation, boolean absolutePosition) {
        // setup window stage
        stage = new Stage();
        Stage parentStage = null;
        if (parentDockPane != null && parentDockPane.getStage() != null) {
            parentStage = parentDockPane.getStage();
            stage.initOwner(parentStage);
        }

        stage.initStyle(stageStyle);

        // offset the new stage to cover exactly the area the dock was local to the scene
        // this is useful for when the user presses the + sign and we have no information
        // on where the mouse was clicked
        Point2D stagePosition;
        boolean translateToCenter = false;
        if (!absolutePosition) {
            // position the new stage relative to the old scene offset
            Point2D floatScreen = this.localToScreen(0, 0);

            //      if (node.isDecorated()) {
            //        Window owner = stage.getOwner();
            //        stagePosition = floatScene.add(new Point2D(owner.getX(), owner.getY()));
            //      } else 
            if (floatScreen != null) {
                // using coordinates the component was previously in (if available)
                stagePosition = floatScreen;
            } else {
                translateToCenter = true;

                if (null != parentStage) {
                    double centerX = parentStage.getX() + (parentStage.getWidth() / 2);
                    double centerY = parentStage.getY() + (parentStage.getHeight() / 2);
                    stagePosition = new Point2D(centerX, centerY);
                } else {
                    // using the center of the screen if no relative position is available
                    Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
                    double centerX = (primScreenBounds.getWidth() - Math.max(getWidth(), getMinWidth())) / 2;
                    double centerY = (primScreenBounds.getHeight() - Math.max(getHeight(), getMinHeight())) / 2;
                    stagePosition = new Point2D(centerX, centerY);
                }
            }

            if (translation != null) {
                stagePosition = stagePosition.add(translation);
            }
        } else {
            stagePosition = translation;
        }

        // the border pane allows the dock node to
        // have a drop shadow effect on the border
        // but also maintain the layout of contents
        // such as a tab that has no content
        borderPane.setCenter(this);

        Scene scene = new Scene(borderPane);

        // apply the floating property so we can get its padding size
        // while it is floating to offset it by the drop shadow
        // this way it pops out above exactly where it was when docked
        this.floatingProperty.set(true);
        this.applyCss();

        // apply the border pane css so that we can get the insets and
        // position the stage properly
        borderPane.applyCss();
        Insets insetsDelta = borderPane.getInsets();

        double insetsWidth = insetsDelta.getLeft() + insetsDelta.getRight();

        stage.setScene(scene);

        stage.setMinWidth(borderPane.minWidth(this.getMinWidth()) + insetsWidth);
        stage.setMinHeight(borderPane.minHeight(this.getMinHeight()));
        borderPane.setPrefSize(content.getPrefWidth() + insetsWidth, content.getPrefHeight());

        if (translateToCenter) {
            // we are floating over the center of some parent, therefore align our center with theirs
            stage.setX(stagePosition.getX() - insetsDelta.getLeft() - (borderPane.getPrefWidth() / 2.0));
            stage.setY(stagePosition.getY() - insetsDelta.getTop() - (borderPane.getPrefHeight() / 2.0));
        } else {
            stage.setX(stagePosition.getX() - insetsDelta.getLeft());
            stage.setY(stagePosition.getY() - insetsDelta.getTop());
        }

        if (stageStyle == StageStyle.TRANSPARENT) {
            scene.setFill(null);
        }

        stage.setResizable(this.isStageResizable());
        if (this.isStageResizable()) {
            stage.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
            stage.addEventFilter(MouseEvent.MOUSE_MOVED, this::handleMouseEvent);
            stage.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseEvent);
        }

        // we want to set the client area size
        // without this it subtracts the native border sizes from the scene
        // size
        stage.sizeToScene();

        layout();

        // if we a child of the primary stage and its not been shown yet then we must wait before
        // showing ourselves otherwise we will not be a child stage
        if (absolutePosition && parentStage != null && !parentStage.isShowing()) {
            queueOnShow((e) -> stage.show());
        } else {
            stage.show();
        }
    }

    private final Stack<EventHandler<WindowEvent>> onShowQueue = new Stack<>();
    private EventHandler<WindowEvent> defaultOnShow;

    private void queueOnShow(EventHandler<WindowEvent> value) {
        if (null != parentDockPane) {
            parentDockPane.queueOnShow(value);
        } else {
            if (onShowQueue.isEmpty()) {
                defaultOnShow = stage.getOnShown();
                stage.setOnShown((e) -> {
                    onShowQueue.forEach((a) -> a.handle(e));
                    if (null != defaultOnShow) {
                        stage.setOnShown(defaultOnShow);
                        defaultOnShow.handle(e);
                    }
                });
            }
            onShowQueue.push(value);
        }
    }

    /**
     * A cache of all dock node event handlers that we have created for tracking the current docking
     * area.
     */
    private ObservableMap<Node, DockNodeEventHandler> dockNodeEventFilters
            = FXCollections.observableHashMap();

    /**
     * A wrapper to the type parameterized generic EventHandler that allows us to remove it from its
     * listener when the dock node becomes detached. It is specifically used to monitor which dock
     * node in this dock pane's layout we are currently dragging over.
     *
     * @since DockFX 0.1
     */
    private class DockNodeEventHandler implements EventHandler<DockEvent> {

        /**
         * The node associated with this event handler that reports to the encapsulating dock pane.
         */
        private Node node = null;

        /**
         * Creates a default dock node event handler that will help this dock pane track the current
         * docking area.
         *
         * @param node The node that is to listen for docking events and report to the encapsulating
         * docking pane.
         */
        public DockNodeEventHandler(Node node) {
            this.node = node;
        }

        @Override
        public void handle(DockEvent event) {
            DockPane.this.dockNodeDrag = node;
        }
    }

    /**
     * Dock the node into this dock pane at the given docking position relative to the sibling in
     * the layout. This is used to relatively position the dock nodes to other nodes given their
     * preferred size.
     *
     * @param node The node that is to be docked into this dock pane.
     * @param dockPos The docking position of the node relative to the sibling.
     * @param sibling The sibling of this node in the layout.
     */
    void floatNode(DockNode node) {
        DockNodeEventHandler dockNodeEventHandler = new DockNodeEventHandler(node);
        dockNodeEventFilters.put(node, dockNodeEventHandler);
        node.addEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);

        ContentPane pane = (ContentPane) root;
        if (pane == null) {
            pane = new ContentSplitPane(node);
            root = (Control) pane;
            this.getChildren().add(root);
        }

        // link out title bar text and graphic to the nodes as there is only a single
        dockTitleBar.mirrorNodeTitleBar(null, node);
        node.showTitleBar(false);
    }

    void dock(DockNode node, DockPos dockPos, Node sibling) {
        DockNodeEventHandler dockNodeEventHandler = new DockNodeEventHandler(node);
        dockNodeEventFilters.put(node, dockNodeEventHandler);
        node.addEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);

        // if we already only have a single child then we need to switch on its titlebar
        DockNode onlyChild = getOnlyChild();
        if (null != onlyChild) {
            dockTitleBar.mirrorNodeTitleBar(onlyChild, null);
            onlyChild.showTitleBar(true);
        }

        ContentPane pane = (ContentPane) root;
        if (pane == null) {
            pane = new ContentSplitPane(node);
            root = (Control) pane;
            this.getChildren().add(root);
            return;
        }

        if (sibling != null && sibling != root) {
            Stack<Parent> stack = new Stack<>();
            stack.push((Parent) root);
            pane = pane.getSiblingParent(stack, sibling);
        }

        if (pane == null) {
            sibling = root;
            dockPos = DockPos.RIGHT;
            pane = (ContentPane) root;
        }

        if (dockPos == DockPos.CENTER) {
            if (pane instanceof ContentSplitPane) {
                // Create a ContentTabPane with two nodes
                DockNode siblingNode = (DockNode) sibling;
                DockNode newNode = (DockNode) node;

                if (null != siblingNode) {
                    siblingNode.showTitleBar(true);
                }
                newNode.showTitleBar(true);

                ContentTabPane tabPane = new ContentTabPane();
                tabPane.setContentParent(pane);

                tabPane.addDockNodeTab(new DockNodeTab(siblingNode));
                tabPane.addDockNodeTab(new DockNodeTab(newNode));

                double[] pos = ((ContentSplitPane) pane).getDividerPositions();
                pane.set(sibling, tabPane);
                ((ContentSplitPane) pane).setDividerPositions(pos);
            }
        } else {
            // Otherwise, SplitPane is assumed.
            Orientation requestedOrientation = (dockPos == DockPos.LEFT || dockPos == DockPos.RIGHT)
                    ? Orientation.HORIZONTAL : Orientation.VERTICAL;

            if (pane instanceof ContentSplitPane) {
                ContentSplitPane split = (ContentSplitPane) pane;

                // if the orientation is different then reparent the split pane
                if (split.getOrientation() != requestedOrientation) {
                    if (split.getItems().size() > 1) {
                        ContentSplitPane splitPane = new ContentSplitPane();

                        if (split == root && sibling == root) {
                            this.getChildren().set(this.getChildren().indexOf(root), splitPane);
                            splitPane.getItems().add(split);
                            root = splitPane;
                        } else {
                            split.set(sibling, splitPane);
                            splitPane.setContentParent(split);
                            splitPane.getItems().add(sibling);
                        }

                        split = splitPane;
                    }
                    split.setOrientation(requestedOrientation);
                    pane = split;
                }

            } else if (pane instanceof ContentTabPane) {
                ContentSplitPane split = (ContentSplitPane) pane.getContentParent();

                // if the orientation is different then reparent the split pane
                if (split.getOrientation() != requestedOrientation) {
                    ContentSplitPane splitPane = new ContentSplitPane();
                    if (split == root && sibling == root) {
                        this.getChildren().set(this.getChildren().indexOf(root), splitPane);
                        splitPane.getItems().add(split);
                        root = splitPane;
                    } else {
                        pane.setContentParent(splitPane);
                        sibling = (Node) pane;
                        split.set(sibling, splitPane);
                        splitPane.setContentParent(split);
                        splitPane.getItems().add(sibling);
                    }
                    split = splitPane;
                } else {
                    sibling = (Node) pane;
                }

                split.setOrientation(requestedOrientation);
                pane = split;
            }
        }

        // Add a node to the proper pane
        pane.addNode(root, sibling, node, dockPos);

        // show the title bar if we are not floating or they are not our only child
        node.showTitleBar(!isFloating() || !isOnlyChild(node));

        if (isFloating()) {
            DockNode newOnlyChild = getOnlyChild();
            if (null != newOnlyChild) {
                dockTitleBar.mirrorNodeTitleBar(null, newOnlyChild);
            }
        }
    }

    /**
     * Dock the node into this dock pane at the given docking position relative to the root in the
     * layout. This is used to relatively position the dock nodes to other nodes given their
     * preferred size.
     *
     * @param node The node that is to be docked into this dock pane.
     * @param dockPos The docking position of the node relative to the sibling.
     */
    void dock(DockNode node, DockPos dockPos) {
        dock(node, dockPos, root);
    }

    /**
     * Detach the node from this dock pane removing it from the layout.
     *
     * @param node The node that is to be removed from this dock pane.
     */
    void undock(DockNode node) {
        DockNodeEventHandler dockNodeEventHandler = dockNodeEventFilters.get(node);
        if (null != dockNodeEventHandler) {
            node.removeEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);
            dockNodeEventFilters.remove(node);
        }

        // depth first search to find the parent of the node
        Stack<Parent> findStack = new Stack<>();
        findStack.push((Parent) root);

        while (!findStack.isEmpty()) {
            Parent parent = findStack.pop();

            if (parent instanceof ContentPane) {
                ContentPane pane = (ContentPane) parent;
                pane.removeNode(findStack, node);

                // if there is 0 children left, make sure we remove the split pane
                if (pane.getChildrenList().isEmpty()) {
                    if (root == pane) {
                        this.getChildren().remove((Node) pane);
                        root = null;
                    }
                } else if (pane.getChildrenList().size() == 1
                        && pane instanceof ContentTabPane
                        && pane.getChildrenList().get(0) instanceof DockNode) {
                    // if there is only 1-tab left, we replace it with the SplitPane

                    List<Node> children = pane.getChildrenList();
                    Node sibling = children.get(0);
                    ContentPane contentParent = pane.getContentParent();

                    contentParent.set((Node) pane, sibling);
                    ((DockNode) sibling).tabbedProperty().setValue(false);
                }
            }
        }

        // is there only a single child left then hide its bar
        if (isFloating()) {
            DockNode onlyChild = getOnlyChild();
            if (null != onlyChild) {
                onlyChild.showTitleBar(false);
                dockTitleBar.mirrorNodeTitleBar(node, onlyChild);
            }
        }
    }

    @Override
    public void handle(DockEvent event) {
        if (event.getEventType() == DockEvent.DOCK_ENTER) {
            if (!dockIndicatorOverlay.isShowing()) {
                Point2D originToScreen;
                if (null != root) {
                    originToScreen = root.localToScreen(0, 0);
                } else {
                    originToScreen = this.localToScreen(0, 0);
                }

                dockIndicatorOverlay
                        .show(DockPane.this, originToScreen.getX(), originToScreen.getY());
                event.getStage().toFront();
            }
        } else if (event.getEventType() == DockEvent.DOCK_OVER) {
            this.receivedEnter = false;

            dockPosDrag = null;
            dockAreaDrag = dockNodeDrag;

            for (DockPosButton dockIndicatorButton : dockPosButtons) {
                if (dockIndicatorButton
                        .contains(dockIndicatorButton.screenToLocal(event.getScreenX(), event.getScreenY()))) {
                    dockPosDrag = dockIndicatorButton.getDockPos();
                    if (dockIndicatorButton.isDockRoot()) {
                        dockAreaDrag = root;
                    }
                    dockIndicatorButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
                    break;
                } else {
                    dockIndicatorButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), false);
                }
            }

            if (dockPosDrag != null && dockAreaDrag != null) {
                Point2D originToScene = dockAreaDrag.localToScreen(0, 0);

                dockAreaIndicator.setVisible(true);
                dockAreaIndicator.relocate(originToScene.getX() - dockIndicatorOverlay.getAnchorX(),
                        originToScene.getY() - dockIndicatorOverlay.getAnchorY());
                if (dockPosDrag == DockPos.RIGHT) {
                    dockAreaIndicator.setTranslateX(dockAreaDrag.getLayoutBounds().getWidth() / 2);
                } else {
                    dockAreaIndicator.setTranslateX(0);
                }

                if (dockPosDrag == DockPos.BOTTOM) {
                    dockAreaIndicator.setTranslateY(dockAreaDrag.getLayoutBounds().getHeight() / 2);
                } else {
                    dockAreaIndicator.setTranslateY(0);
                }

                if (dockPosDrag == DockPos.LEFT || dockPosDrag == DockPos.RIGHT) {
                    dockAreaIndicator.setWidth(dockAreaDrag.getLayoutBounds().getWidth() / 2);
                } else {
                    dockAreaIndicator.setWidth(dockAreaDrag.getLayoutBounds().getWidth());
                }
                if (dockPosDrag == DockPos.TOP || dockPosDrag == DockPos.BOTTOM) {
                    dockAreaIndicator.setHeight(dockAreaDrag.getLayoutBounds().getHeight() / 2);
                } else {
                    dockAreaIndicator.setHeight(dockAreaDrag.getLayoutBounds().getHeight());
                }
            } else {
                dockAreaIndicator.setVisible(false);
            }

            if (dockNodeDrag != null) {
                Point2D originToScreen = dockNodeDrag.localToScreen(0, 0);

                double posX = originToScreen.getX() + dockNodeDrag.getLayoutBounds().getWidth() / 2
                        - dockPosIndicator.getWidth() / 2;
                double posY = originToScreen.getY() + dockNodeDrag.getLayoutBounds().getHeight() / 2
                        - dockPosIndicator.getHeight() / 2;

                if (!dockIndicatorPopup.isShowing()) {
                    dockIndicatorPopup.show(DockPane.this, posX, posY);
                    event.getStage().toFront();
                } else {
                    dockIndicatorPopup.setX(posX);
                    dockIndicatorPopup.setY(posY);
                    event.getStage().toFront();
                }

                // set visible after moving the popup
                dockPosIndicator.setVisible(true);
            } else {
                dockPosIndicator.setVisible(false);
            }
        }

        if (event.getEventType() == DockEvent.DOCK_RELEASED && event.getContents() != null) {
            if (dockPosDrag != null && dockIndicatorOverlay.isShowing()) {
                DockNode dockNode = ((DockPane) event.getContents()).getOnlyChild();
                if (null != dockNode) {
                    dockNode.dock(this, dockPosDrag, dockAreaDrag);
                }
            }
        }

        if ((event.getEventType() == DockEvent.DOCK_EXIT && !this.receivedEnter)
                || event.getEventType() == DockEvent.DOCK_RELEASED) {
            if (dockIndicatorOverlay.isShowing()) {
                dockIndicatorOverlay.hide();
            }
            if (dockIndicatorPopup.isShowing()) {
                dockIndicatorPopup.hide();
            }
        }
    }

    public void storeLayout(String filePath) throws FileNotFoundException {
        storeLayout(filePath, generateLayout());
    }

    public Map<String, ContentHolder> generateLayout() {
        HashMap<String, ContentHolder> contents = new HashMap<>();

        // Floating Nodes collection
        ContentHolder floatingContent = new ContentHolder(ContentHolder.Type.Collection);
        contents.put("_FloatingNodes", floatingContent);

        for (DockPane floatingPane : floatingDockPanes) {
            ContentHolder floatingNode = checkPane(floatingPane.root);
            floatingNode.addProperty("Title", floatingPane.getTitle());

            floatingNode.addProperty("Position", new Double[]{
                floatingPane.getStage().getX(),
                floatingPane.getStage().getY()
            });

            floatingNode.addProperty("MinSize", new Double[]{
                floatingPane.getStage().getMinWidth(),
                floatingPane.getStage().getMinHeight()
            });

            floatingNode.addProperty("Size", new Double[]{
                floatingPane.getStage().getWidth(),
                floatingPane.getStage().getHeight()
            });

            floatingContent.addChild(floatingNode);
        }

        if (null != this.root) {
            contents.put("_DockedNodes", checkPane(this.root));
        }

        return contents;
    }

    public static void storeLayout(String fileName, Map<String, ContentHolder> contents) throws FileNotFoundException {
        try (XMLEncoder e = new XMLEncoder(
                new BufferedOutputStream(
                        new FileOutputStream(fileName)))) {
            e.writeObject(contents);
        }
    }

    private static ContentHolder checkPane(Node pane) {
        ContentHolder holder = null;
        if (pane instanceof ContentSplitPane) {
            ContentSplitPane splitPane = (ContentSplitPane) pane;

            holder = new ContentHolder(ContentHolder.Type.SplitPane);

            holder.addProperty("Orientation", splitPane.getOrientation());
            holder.addProperty("DividerPositions", splitPane.getDividerPositions());
        } else if (pane instanceof ContentTabPane) {
            ContentTabPane tabPane = (ContentTabPane) pane;

            holder = new ContentHolder(ContentHolder.Type.TabPane);

            holder.addProperty("SelectedIndex", tabPane.getSelectionModel().getSelectedIndex());
        } else if (pane instanceof DockNode) {
            DockNode nd = (DockNode) pane;

            holder = new ContentHolder(ContentHolder.Type.DockNode);

            holder.addProperty("Id", nd.getIdentity());
            holder.addProperty("Title", nd.getTitle());
            holder.addProperty("Size", new Double[]{
                nd.getWidth(),
                nd.getHeight()
            });

            if (null != nd.getViewController()) {
                Map<String, Object> props = nd.getViewController().getState();
                holder.addProperty("Properties", props);
            }
        }

        if ((null != holder) && (pane instanceof ContentPane)) {
            for (Node node : ((ContentPane) pane).getChildrenList()) {
                if ((node instanceof ContentPane) || (node instanceof DockNode)) {
                    holder.addChild(checkPane(node));
                }
            }
        }

        return holder;
    }

    public void loadAndApplyLayout(String filePath) throws FileNotFoundException {
        loadAndApplyLayout(filePath, null);
    }

    public void loadAndApplyLayout(InputStream is) {
        loadAndApplyLayout(is, null);
    }

    public void loadAndApplyLayout(String filePath, DelayOpenHandler delayOpenHandler) throws FileNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            loadAndApplyLayout(new BufferedInputStream(fis), delayOpenHandler);
        }
        catch (FileNotFoundException ex) {
            throw ex;
        }
        catch (IOException ex) {
        }
    }

    public void loadAndApplyLayout(InputStream is, DelayOpenHandler delayOpenHandler) {
        applyLayout(loadLayout(is), delayOpenHandler);
    }

    public void applyLayout(Map<String, ContentHolder> contents, DelayOpenHandler delayOpenHandler) {
        if (null != contents) {
            applyPane(contents, (ContentPane) root, delayOpenHandler);
        }
    }

    public static HashMap<String, ContentHolder> loadLayout(String filePath) throws FileNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return loadLayout(new BufferedInputStream(fis));
        }
        catch (FileNotFoundException ex) {
            throw ex;
        }
        catch (IOException ex) {
            return null;
        }
    }

    public static HashMap<String, ContentHolder> loadLayout(InputStream is) {
        try (XMLDecoder e = new XMLDecoder(is)) {
            return (HashMap<String, ContentHolder>) e.readObject();
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            // empty file
        }

        return null;
    }

    private void collectDockNodes(HashMap<String, DockNode> dockNodes, ContentPane pane) {
        if (null != pane) {
            for (Node node : pane.getChildrenList()) {
                if (node instanceof DockNode) {
                    dockNodes.put(((DockNode) node).getIdentity(), (DockNode) node);
                }

                if (node instanceof ContentPane) {
                    collectDockNodes(dockNodes, (ContentPane) node);
                }
            }
        }
    }

    private void applyPane(Map<String, ContentHolder> contents, ContentPane root, DelayOpenHandler delayOpenHandler) {
        // Collect the current pane information
        HashMap<String, DockNode> dockNodes = new HashMap<>();

        collectDockNodes(dockNodes, root);

        for (DockPane floatingDockPane : floatingDockPanes) {
            collectDockNodes(dockNodes, (ContentPane) floatingDockPane.root);
        }

        // Set floating docks according to the preference data
        for (Object item : contents.get("_FloatingNodes").getChildren()) {
            ContentHolder holder = (ContentHolder) item;

            DockPane floatingPane = new DockPane(this);
            Control newRoot = (Control) buildPane(this, floatingPane, null,
                    holder, dockNodes, delayOpenHandler);

            floatingPane.root = newRoot;
            floatingPane.getChildren().add(newRoot);

            String title = holder.getProperties().getProperty("Title");
            floatingPane.setTitle(title);

            DockNode onlyChild = floatingPane.getOnlyChild();
            if (null != onlyChild) {
                floatingPane.floatNode(onlyChild);
            }

            Double[] position = (Double[]) holder.getProperties().get("Position");
            floatingPane.setFloating(newRoot, new Point2D(position[0], position[1]), true);

            // need to reset min width/height
            Double[] msize = (Double[]) holder.getProperties().get("MinSize");
            Double[] size = (Double[]) holder.getProperties().get("Size");
            floatingPane.stage.setMinWidth(msize[0]);
            floatingPane.stage.setMinHeight(msize[1]);

            floatingPane.queueOnShow((e) -> {
                floatingPane.stage.setWidth(size[0]);
                floatingPane.stage.setHeight(size[1]);
            });
        }

        // Restore dock location based on the preferences
        // Make it sorted
        ContentHolder rootHolder = contents.get("_DockedNodes");

        if (null != rootHolder) {
            Node newRoot = buildPane(this, this, null, rootHolder, dockNodes, delayOpenHandler);

            if (null != newRoot) {
                this.root = (Control) newRoot;
                this.getChildren().add(this.root);
            }
        }
    }

    private static Node buildPane(DockPane oldPane, DockPane newPane, ContentPane containerPane,
            ContentHolder holder, HashMap<String, DockNode> dockNodes,
            DelayOpenHandler delayOpenHandler) {
        Node rv = null;

        switch (holder.getType()) {
            case SplitPane: {
                ContentSplitPane splitPane = new ContentSplitPane();
                splitPane.setContentParent(containerPane);
                splitPane.setOrientation((Orientation) holder.getProperties().get("Orientation"));
                splitPane.setDividerPositions((double[]) holder.getProperties().get("DividerPositions"));

                for (Object item : holder.getChildren()) {
                    // Call this function recursively
                    splitPane.getItems().add(buildPane(oldPane, newPane, splitPane,
                            (ContentHolder) item, dockNodes, delayOpenHandler));
                }

                rv = splitPane;
            }
            break;
            case TabPane: {
                ContentTabPane tabPane = new ContentTabPane();
                tabPane.setContentParent(containerPane);

                for (Object item : holder.getChildren()) {
                    Node n = buildPane(oldPane, newPane, tabPane,
                            (ContentHolder) item, dockNodes, delayOpenHandler);

                    if (n instanceof DockNode) {
                        tabPane.addDockNodeTab(new DockNodeTab((DockNode) n));
                    }
                }

                tabPane.getSelectionModel().select((int) holder.getProperties().get("SelectedIndex"));
                rv = tabPane;
            }
            break;
            case DockNode: {
                String id = holder.getProperties().getProperty("Id");
                String title = holder.getProperties().getProperty("Title");
                Double[] size = (Double[]) holder.getProperties().get("Size");
                Map<String, Object> props = (Map) holder.getProperties().get("Properties");
                DockNode n = dockNodes.get(id);

                if ((null == n) && (null != delayOpenHandler)) {
                    // If delayOpenHandler is provided, we call it
                    n = delayOpenHandler.open(id, title, size[0], size[1], props);
                }

                // Use dock node
                if (null != n) {
                    if (n.tabbedProperty().get()) {
                        n.tabbedProperty().set(false);
                    }

                    if (oldPane != newPane) {
                        oldPane.undock(n);
                    }

                    n.setDockPane(newPane);
                    DockNodeEventHandler dockNodeEventHandler = newPane.new DockNodeEventHandler(n);
                    newPane.dockNodeEventFilters.put(n, dockNodeEventHandler);
                    n.addEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);
                } else {
                    System.err.println(id + " is not present.");
                }

                dockNodes.remove(id);
                rv = n;
            }
            break;
        }

        return rv;
    }

    /**
     * The last position of the mouse that was within the minimum layout bounds.
     */
    private Point2D sizeLast;
    /**
     * Whether we are currently resizing in a given direction.
     */
    private boolean sizeWest = false, sizeEast = false, sizeNorth = false, sizeSouth = false;

    /**
     * Gets whether the mouse is currently in this dock node's resize zone.
     *
     * @return Whether the mouse is currently in this dock node's resize zone.
     */
    public boolean isMouseResizeZone() {
        return sizeWest || sizeEast || sizeNorth || sizeSouth;
    }

    public void handleMouseEvent(MouseEvent event) {
        Cursor cursor = Cursor.DEFAULT;

        // TODO: use escape to cancel resize/drag operation like visual studio
        if (!this.isFloating() || !this.isStageResizable()) {
            return;
        }

        if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            sizeLast = new Point2D(event.getScreenX(), event.getScreenY());
        } else if (event.getEventType() == MouseEvent.MOUSE_MOVED) {
            Insets insets = borderPane.getPadding();

            sizeWest = event.getX() < insets.getLeft();
            sizeEast = event.getX() > borderPane.getWidth() - insets.getRight();
            sizeNorth = event.getY() < insets.getTop();
            sizeSouth = event.getY() > borderPane.getHeight() - insets.getBottom();

            if (sizeWest) {
                if (sizeNorth) {
                    cursor = Cursor.NW_RESIZE;
                } else if (sizeSouth) {
                    cursor = Cursor.SW_RESIZE;
                } else {
                    cursor = Cursor.W_RESIZE;
                }
            } else if (sizeEast) {
                if (sizeNorth) {
                    cursor = Cursor.NE_RESIZE;
                } else if (sizeSouth) {
                    cursor = Cursor.SE_RESIZE;
                } else {
                    cursor = Cursor.E_RESIZE;
                }
            } else if (sizeNorth) {
                cursor = Cursor.N_RESIZE;
            } else if (sizeSouth) {
                cursor = Cursor.S_RESIZE;
            }

            this.getScene().setCursor(cursor);
        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED && this.isMouseResizeZone()) {
            Point2D sizeCurrent = new Point2D(event.getScreenX(), event.getScreenY());
            Point2D sizeDelta = sizeCurrent.subtract(sizeLast);

            double newX = stage.getX(), newY = stage.getY(), newWidth = stage.getWidth(),
                    newHeight = stage.getHeight();

            if (sizeNorth) {
                newHeight -= sizeDelta.getY();
                newY += sizeDelta.getY();
            } else if (sizeSouth) {
                newHeight += sizeDelta.getY();
            }

            if (sizeWest) {
                newWidth -= sizeDelta.getX();
                newX += sizeDelta.getX();
            } else if (sizeEast) {
                newWidth += sizeDelta.getX();
            }

            // TODO: find a way to do this synchronously and eliminate the flickering of moving the stage
            // around, also file a bug report for this feature if a work around can not be found this
            // primarily occurs when dragging north/west but it also appears in native windows and Visual
            // Studio, so not that big of a concern.
            // Bug report filed:
            // https://bugs.openjdk.java.net/browse/JDK-8133332
            double currentX = sizeLast.getX(), currentY = sizeLast.getY();
            if (newWidth >= stage.getMinWidth()) {
                stage.setX(newX);
                stage.setWidth(newWidth);
                currentX = sizeCurrent.getX();
            }

            if (newHeight >= stage.getMinHeight()) {
                stage.setY(newY);
                stage.setHeight(newHeight);
                currentY = sizeCurrent.getY();
            }
            sizeLast = new Point2D(currentX, currentY);
            // we do not want the title bar getting these events
            // while we are actively resizing
            if (sizeNorth || sizeSouth || sizeWest || sizeEast) {
                event.consume();
            }
        }
    }
}
