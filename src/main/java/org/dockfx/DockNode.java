/**
 * @file DockNode.java
 * @brief Class implementing basic dock node with floating and styling.
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

import javafx.stage.WindowEvent;
import org.dockfx.viewControllers.DockFXViewController;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.dockfx.pane.DockNodeTab;

/**
 * Base class for a dock node that provides the layout of the content along with a title bar and a
 * styled border. The dock node can be detached and floated or closed and removed from the layout.
 * Dragging behavior is implemented through the title bar.
 *
 * @since DockFX 0.1
 */
public class DockNode extends VBox {
    /**
     * The contents of the dock node, i.e. a TreeView or ListView.
     */
    private Node contents;
    /**
     * The title bar that implements our dragging and state manipulation.
     */
    private DockNodeTitleBar dockTitleBar;
    /**
     * The dock pane this dock node belongs to.
     */
    private DockPane dockPane;

    /**
     * View controller of node inside this DockNode
     */
    private DockFXViewController viewController;

    public DockNode(Node contents, String title, Node graphic, String identity, DockFXViewController controller) {
        initializeDockNode(contents, title, graphic, identity, controller);
    }

    /**
     * Creates a default DockNode with a default title bar and layout.
     *
     * @param contents The contents of the dock node which may be a tree or another scene graph
     * node.
     * @param title The caption title of this dock node which maintains bidirectional state with the
     * title bar and stage.
     * @param graphic The caption graphic of this dock node which maintains bidirectional state with
     * the title bar and stage.
     * @param identity The identity of this dock node, if null then it will bind to the title
     * property.
     */
    public DockNode(Node contents, String title, Node graphic, String identity) {
        this(contents, title, graphic, null, null);
    }

    /**
     * Creates a default DockNode with a default title bar and layout.
     *
     * @param contents The contents of the dock node which may be a tree or another scene graph
     * node.
     * @param title The caption title of this dock node which maintains bidirectional state with the
     * title bar and stage.
     * @param graphic The caption graphic of this dock node which maintains bidirectional state with
     * the title bar and stage.
     */
    public DockNode(Node contents, String title, Node graphic) {
        this(contents, title, graphic, null);
    }

    /**
     * Creates a default DockNode with a default title bar and layout.
     *
     * @param contents The contents of the dock node which may be a tree or another scene graph
     * node.
     * @param title The caption title of this dock node which maintains bidirectional state with the
     * title bar and stage.
     */
    public DockNode(Node contents, String title) {
        this(contents, title, null);
    }

    /**
     * Creates a default DockNode with a default title bar and layout.
     *
     * @param contents The contents of the dock node which may be a tree or another scene graph
     * node.
     */
    public DockNode(Node contents) {
        this(contents, null, null);
    }

    /**
     *
     * Creates a default DockNode with contents loaded from FXMLFile at provided path.
     *
     * @param FXMLPath path to fxml file.
     * @param title The caption title of this dock node which maintains bidirectional state with the
     * title bar and stage.
     * @param graphic The caption title of this dock node which maintains bidirectional state with
     * the title bar and stage.
     * @param identity The identity of this dock node, if null then it will bind to the title
     * property.
     */
    public DockNode(String FXMLPath, String title, Node graphic, String identity) {
        FXMLLoader loader = loadNode(FXMLPath);
        initializeDockNode(loader.getRoot(), title, graphic, identity, loader.getController());
    }

    /**
     *
     * Creates a default DockNode with contents loaded from FXMLFile at provided path.
     *
     * @param FXMLPath path to fxml file.
     * @param title The caption title of this dock node which maintains bidirectional state with the
     * title bar and stage.
     * @param graphic The caption title of this dock node which maintains bidirectional state with
     * the title bar and stage.
     */
    public DockNode(String FXMLPath, String title, Node graphic) {
        this(FXMLPath, title, graphic, null);
    }

    /**
     * Creates a default DockNode with contents loaded from FXMLFile at provided path.
     *
     * @param FXMLPath path to fxml file.
     * @param title The caption title of this dock node which maintains bidirectional state with the
     * title bar and stage.
     */
    public DockNode(String FXMLPath, String title) {
        this(FXMLPath, title, null);
    }

    /**
     * Creates a default DockNode with contents loaded from FXMLFile at provided path with default
     * title bar.
     *
     * @param FXMLPath path to fxml file.
     */
    public DockNode(String FXMLPath) {
        this(FXMLPath, null, null);
    }

    /**
     * Loads Node from fxml file located at FXMLPath and returns it.
     *
     * @param FXMLPath Path to fxml file.
     * @return Node loaded from fxml file or StackPane with Label with error message.
     */
    private static FXMLLoader loadNode(String FXMLPath) {
        FXMLLoader loader = new FXMLLoader();
        try {
            loader.load(DockNode.class.getResourceAsStream(FXMLPath));
        }
        catch (Exception e) {
            e.printStackTrace();
            loader.setRoot(new StackPane(new Label("Could not load FXML file")));
        }
        return loader;
    }

    /**
     * Sets DockNodes contents, title and title bar graphic
     *
     * @param contents The contents of the dock node which may be a tree or another scene graph
     * node.
     * @param title The caption title of this dock node which maintains bidirectional state with the
     * title bar and stage.
     * @param identity The identity of this dock node, if null then it will bind to the title
     * property.
     * @param graphic The caption title of this dock node which maintains bidirectional state with
     * the title bar and stage.
     */
    private void initializeDockNode(Node contents, String title, Node graphic, String identity, DockFXViewController controller) {
        this.titleProperty.setValue(title);
        this.graphicProperty.setValue(graphic);
        this.contents = contents;
        this.viewController = controller;

        if (null == identity) {
            this.identityProperty.bind(titleProperty);
        } else {
            this.identityProperty.set(identity);
        }

        dockTitleBar = new DockNodeTitleBar(this);
        if (viewController != null) {
            viewController.setDockTitleBar(dockTitleBar);
        }

        getChildren().addAll(dockTitleBar, contents);
        VBox.setVgrow(contents, Priority.ALWAYS);

        this.getStyleClass().add("dock-node");
    }

    /**
     * Changes the contents of the dock node.
     *
     * @param contents The new contents of this dock node.
     */
    public void setContents(Node contents) {
        this.getChildren().set(this.getChildren().indexOf(this.contents), contents);
        this.contents = contents;
    }

    /**
     * Changes the title bar in the layout of this dock node. This can be used to remove the dock
     * title bar from the dock node by passing null.
     *
     * @param dockTitleBar null The new title bar of this dock node, can be set null indicating no
     * title bar is used.
     */
    public void setDockTitleBar(DockNodeTitleBar dockTitleBar) {
        if (dockTitleBar != null) {
            if (this.dockTitleBar != null) {
                this.getChildren().set(this.getChildren().indexOf(this.dockTitleBar), dockTitleBar);
            } else {
                this.getChildren().add(0, dockTitleBar);
            }
        } else {
            this.getChildren().remove(this.dockTitleBar);
        }

        this.dockTitleBar = dockTitleBar;
    }

    public void showTitleBar(boolean show) {
        if (null != dockTitleBar) {
            if (show) {
                dockTitleBar.bindLabelGraphic(dockPane);
            }
            dockTitleBar.setVisible(show);
            dockTitleBar.setManaged(show);
        }
    }

    public Stage getStage() {
        return (null != dockPane) ? dockPane.getStage() : null;
    }

    /**
     * Whether the node is currently floating.
     *
     * @param floating Whether the node is currently floating.
     * @param translation null The offset of the node after being set floating. Used for aligning it
     * with its layout bounds inside the dock pane when it becomes detached. Can be null indicating
     * no translation.
     */
    public void setFloating(boolean floating, Point2D translation, DockPane parentDockPane) {
        if (floating) {
            if (dockPane == null || !dockPane.isFloating() || !dockPane.isOnlyChild(this)) {
                // we will be the only child so we don't need to show our title bar
                showTitleBar(false);

                if (null != dockPane) {
                    dockPane.undock(this);
                }

                // dispose of the old dock pane if it is now empty
                if (null != dockPane && dockPane.isFloating() && dockPane.getChildren().isEmpty()) {
                    dockPane.close();
                }

                // need a new DockPane to contain us
                setDockPane(new DockPane(parentDockPane));

                dockPane.floatNode(this);
                dockPane.setFloating(this, translation, false);
            }
        } else if (!floating && null != dockPane) {
            DockPane oldDockPane = undock();

            // dispose of the old dock pane if it is now empty
            if (oldDockPane.isFloating() && oldDockPane.getChildren().isEmpty()) {
                oldDockPane.close();
            }
        }
    }

    /**
     * Whether the node is currently floating.
     *
     * @param floating Whether the node is currently floating.
     */
    public void setFloating(boolean floating) {
        setFloating(floating, null, dockPane);
    }

    /**
     * The dock pane that was last associated with this dock node. Either the dock pane that it is
     * currently docked to or the one it was detached from. Can be null if the node was never
     * docked.
     *
     * @return The dock pane that was last associated with this dock node.
     */
    public final DockPane getDockPane() {
        return dockPane;
    }

    /**
     * ViewController associated with this dock nodes contents, might be null
     *
     * @return ViewController associated with this dock nodes contents
     */
    public final DockFXViewController getViewController() {
        return viewController;
    }

    /**
     * The dock title bar associated with this dock node.
     *
     * @return The dock title bar associated with this node.
     */
    public final DockNodeTitleBar getDockTitleBar() {
        return this.dockTitleBar;
    }

    /**
     * The contents managed by this dock node.
     *
     * @return The contents managed by this dock node.
     */
    public final Node getContents() {
        return contents;
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
     * String property maintaining bidirectional state of the caption title for this node with the
     * dock title bar or stage.
     *
     * @defaultValue "Dock"
     */
    public final StringProperty titleProperty() {
        return titleProperty;
    }

    private StringProperty titleProperty = new SimpleStringProperty("Dock") {
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
     * String property maintaining bidirectional state of the caption title for this node with the
     * dock title bar or stage.
     *
     * @defaultValue "Dock"
     */
    public final StringProperty identityProperty() {
        return identityProperty;
    }

    private StringProperty identityProperty = new SimpleStringProperty("Dock") {
        @Override
        public String getName() {
            return "identity";
        }
    };

    public final String getIdentity() {
        return identityProperty.get();
    }

    public final void setIdentity(String id) {
        if (null == id) {
            this.identityProperty.setValue(titleProperty.get());
            this.identityProperty.bind(titleProperty);
        } else {
            this.identityProperty.unbind();
            this.identityProperty.setValue(id);
        }
    }

    /**
     * Boolean property maintaining whether this node is currently using a custom title bar. This
     * can be used to force the default title bar to show when the dock node is set to floating
     * instead of using native window borders.
     *
     * @defaultValue true
     */
    public final BooleanProperty customTitleBarProperty() {
        return customTitleBarProperty;
    }

    private BooleanProperty customTitleBarProperty = new SimpleBooleanProperty(true) {
        @Override
        public String getName() {
            return "customTitleBar";
        }
    };

    public final boolean isCustomTitleBar() {
        return customTitleBarProperty.get();
    }

    public final void setUseCustomTitleBar(boolean useCustomTitleBar) {
        if (dockPane.isFloating()) {
            dockTitleBar.setVisible(useCustomTitleBar);
            dockTitleBar.setManaged(useCustomTitleBar);
        }
        this.customTitleBarProperty.set(useCustomTitleBar);
    }

    /**
     * Boolean property maintaining whether this node is currently floatable.
     *
     * @defaultValue true
     */
    public final BooleanProperty floatableProperty() {
        return floatableProperty;
    }

    private BooleanProperty floatableProperty = new SimpleBooleanProperty(true) {
        @Override
        public String getName() {
            return "floatable";
        }
    };

    public final boolean isFloatable() {
        return floatableProperty.get();
    }

    public final void setFloatable(boolean floatable) {
        if (!floatable && dockPane.isFloating()) {
            this.setFloating(false);
        }
        this.floatableProperty.set(floatable);
    }

    /**
     * Boolean property maintaining whether this node is currently closable.
     *
     * @defaultValue true
     */
    public final BooleanProperty closableProperty() {
        return closableProperty;
    }

    private BooleanProperty closableProperty = new SimpleBooleanProperty(true) {
        @Override
        public String getName() {
            return "closable";
        }
    };

    public final boolean isClosable() {
        return closableProperty.get();
    }

    public final void setClosable(boolean closable) {
        this.closableProperty.set(closable);
    }

//  /**
//   * Boolean property maintaining whether this node is currently docked. This is used by the dock
//   * pane to inform the dock node whether it is currently docked.
//   * 
//   * @defaultValue false
//   */
//  public final BooleanProperty dockedProperty() {
//    return dockedProperty;
//  }
//
//  private BooleanProperty dockedProperty = new SimpleBooleanProperty(false) {
//    @Override
//    protected void invalidated() {
//      if (get()) {
//        if (dockTitleBar != null) {
//          dockTitleBar.setVisible(true);
//          dockTitleBar.setManaged(true);
//        }
//      }
//
//      DockNode.this.pseudoClassStateChanged(DOCKED_PSEUDO_CLASS, get());
//    }
//
//    @Override
//    public String getName() {
//      return "docked";
//    }
//  };
//
//  public final boolean isDocked() {
//    return dockedProperty.get();
//  }
//
    public final boolean isDecorated() {
        return dockPane.getStageStyle() != StageStyle.TRANSPARENT
                && dockPane.getStageStyle() != StageStyle.UNDECORATED;
    }

    /**
     * Boolean property maintaining whether this node is currently tabbed.
     *
     * @defaultValue false
     */
    public final BooleanProperty tabbedProperty() {
        return tabbedProperty;
    }

    private BooleanProperty tabbedProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {

            if (getChildren() != null) {
                if (get()) {
                    getChildren().remove(dockTitleBar);
                } else {
                    getChildren().add(0, dockTitleBar);
                }
            }
        }

        @Override
        public String getName() {
            return "tabbed";
        }
    };

    public final boolean isTabbed() {
        return tabbedProperty.get();
    }

    /**
     * Boolean property maintaining whether this node is currently closed.
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

    private DockPos lastDockPos;

    public DockPos getLastDockPos() {
        return lastDockPos;
    }

    private Node lastDockSibling;

    public Node getLastDockSibling() {
        return lastDockSibling;
    }

    /**
     * Dock this node into a dock pane.
     *
     * @param dockPane The dock pane to dock this node into.
     * @param dockPos The docking position relative to the sibling of the dock pane.
     * @param sibling The sibling node to dock this node relative to.
     */
    public void dock(DockPane dockPane, DockPos dockPos, Node sibling) {
        dockImpl(dockPane);
        dockPane.dock(this, dockPos, sibling);
        this.lastDockPos = dockPos;
        this.lastDockSibling = sibling;
    }

    /**
     * Dock this node into a dock pane.
     *
     * @param dockPane The dock pane to dock this node into.
     * @param dockPos The docking position relative to the sibling of the dock pane.
     */
    public void dock(DockPane dockPane, DockPos dockPos) {
        dockImpl(dockPane);
        dockPane.dock(this, dockPos);
        this.lastDockPos = dockPos;
    }

    protected void setDockPane(DockPane newDockPane) {
        if (null != dockPane) {
            dockPane.closedProperty().removeListener(dockPaneCloseListener);
        }

        this.dockPane = newDockPane;

        if (null != newDockPane) {
            newDockPane.closedProperty().addListener(dockPaneCloseListener);
        }
    }

    private final void dockImpl(DockPane newDockPane) {
        if ((dockPane != null) && dockPane.isFloating()) {
            setFloating(false);
        }
        setDockPane(newDockPane);
        //this.dockedProperty.set(true);
        this.closedProperty.set(false);
    }

    /**
     * Detach this node from its previous dock pane if it was previously docked.
     */
    public DockPane undock() {
        DockPane oldDockPane = null;

        if (dockPane != null) {
            oldDockPane = dockPane;
            dockPane.undock(this);
            setDockPane(null);
        }
        //this.dockedProperty.set(false);
        this.tabbedProperty.set(false);

        return oldDockPane;
    }

    /**
     * Close this dock node by setting it to not floating and making sure it is detached from any
     * dock pane.
     */
    public void close() {
        this.closedProperty.set(true);

        setFloating(false);
    }

    private DockNodeTab dockNodeTab;

    public void setNodeTab(DockNodeTab nodeTab) {
        this.dockNodeTab = nodeTab;
    }

    public void focus() {
        if (tabbedProperty().get()) {
            dockNodeTab.select();
        }
    }

    private final DockPaneCloseListener dockPaneCloseListener = new DockPaneCloseListener();

    private final class DockPaneCloseListener implements ChangeListener<Boolean> {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue) {
                close();
            }
        }
    }
}
