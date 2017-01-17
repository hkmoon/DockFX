/**
 * @file DockTitleBar.java
 * @brief Class implementing a generic base for a dock node title bar.
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

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Base class for a dock node title bar that provides the mouse dragging functionality, captioning,
 * docking, and state manipulation.
 *
 * @since DockFX 0.1
 */
public class DockNodeTitleBar extends DockTitleBar {
    /**
     * The window this node is a title bar for.
     */
    private final DockNode dockNode;

    /**
     * Creates a default DockTitleBar with captions and dragging behavior.
     *
     * @param dockNode The docking window that requires a title bar.
     */
    public DockNodeTitleBar(DockNode dockNode) {
        super("dock-title-bar");
        label.textProperty().bind(dockNode.titleProperty());
        label.graphicProperty().bind(dockNode.graphicProperty());

        this.dockNode = dockNode;

        dockNode.closableProperty().addListener((p, o, n) -> {
            if (n) {
                if (!getChildren().contains(closeButton)) {
                    getChildren().add(closeButton);
                }
            } else {
                getChildren().removeIf(c -> c.equals(closeButton));
            }
        });
    }

    protected void bindLabelGraphic() {
        unbindLabelGraphic();
        label.setGraphic(dockNode.getGraphic());
        label.graphicProperty().bind(dockNode.graphicProperty());
    }

    protected void bindLabelGraphic(DockPane pane) {
        if (pane.getDockTitleBar().label.getGraphic() == dockNode.getGraphic()) {
            pane.getDockTitleBar().unbindLabelGraphic();
        }
        unbindLabelGraphic();
        label.setGraphic(dockNode.getGraphic());
        label.graphicProperty().bind(dockNode.graphicProperty());
    }

    @Override
    protected void handleButtonAction() {
        if (dockNode.getDockPane().isFloating() && dockNode.getDockPane().isOnlyChild(dockNode)) {
            dockNode.getDockPane().setMaximized(!dockNode.getDockPane().isMaximized());
        } else {
            dockNode.setFloating(true);
        }
    }

    @Override
    protected Stage getStage() {
        return dockNode.getStage();
    }

    @Override
    protected void handleCloseAction() {
        dockNode.close();
    }

    @Override
    public void handle(MouseEvent event) {
        if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            if (dockNode.getDockPane().isFloating() && event.getClickCount() == 2
                    && event.getButton() == MouseButton.PRIMARY) {
                // drag detected is used in place of mouse pressed so there is some threshold for the
                // dragging which is determined by the default drag detection threshold
                dragStart = new Point2D(event.getX(), event.getY());
            }
        } else if (event.getEventType() == MouseEvent.DRAG_DETECTED) {
            // if we are not using a custom title bar and the user
            // is not forcing the default one for floating and
            // the dock node does have native window decorations
            // then we need to offset the stage position by
            // the height of this title bar
            if (!dockNode.isCustomTitleBar() && dockNode.isDecorated()) {
                dockNode.setFloating(true, new Point2D(0, DockNodeTitleBar.this.getHeight()), null);
            } else {
                dockNode.setFloating(true);
            }

            // TODO: Find a better solution.
            // Temporary work around for nodes losing the drag event when removed from
            // the scene graph.
            // A possible alternative is to use "ghost" panes in the DockPane layout
            // while making DockNode simply an overlay stage that is always shown.
            // However since flickering when popping out was already eliminated that would
            // be overkill and is not a suitable solution for native decorations.
            // Bug report open: https://bugs.openjdk.java.net/browse/JDK-8133335
            DockPane dockPane = dockNode.getDockPane();
            if (dockPane != null) {
                dockPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, this);
                dockPane.addEventFilter(MouseEvent.MOUSE_RELEASED, this);
            }

            double ratioX = event.getX() / dockNode.getWidth();
            double ratioY = event.getY() / dockNode.getHeight();

            // scale the drag start location by our restored dimensions
            dragStart = new Point2D(ratioX * dockNode.getWidth(), ratioY * dockNode.getHeight());
            dragging = true;
            event.consume();
        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            if (dockNode.getDockPane().isFloating() && event.getClickCount() == 2
                    && event.getButton() == MouseButton.PRIMARY) {
                event.setDragDetect(false);
                event.consume();
                return;
            }

            if (!dragging) {
                return;
            }

            Stage stage = dockNode.getStage();
            Insets insetsDelta = dockNode.getDockPane().getBorderPane().getInsets();

            // it is possible that drag start has not been set if some other node had focus when
            // we started the drag
            if (null == dragStart) {
                dragStart = new Point2D(event.getX(), event.getY());
            }

            // dragging this way makes the interface more responsive in the event
            // the system is lagging as is the case with most current JavaFX
            // implementations on Linux
            stage.setX(event.getScreenX() - dragStart.getX() - insetsDelta.getLeft());
            stage.setY(event.getScreenY() - dragStart.getY() - insetsDelta.getTop());

            // TODO: change the pick result by adding a copyForPick()
            DockEvent dockEnterEvent
                    = new DockEvent(this.getStage(), this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_ENTER, event.getX(),
                            event.getY(), event.getScreenX(), event.getScreenY(), null);
            DockEvent dockOverEvent
                    = new DockEvent(this.getStage(), this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_OVER, event.getX(),
                            event.getY(), event.getScreenX(), event.getScreenY(), null);
            DockEvent dockExitEvent
                    = new DockEvent(this.getStage(), this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_EXIT, event.getX(),
                            event.getY(), event.getScreenX(), event.getScreenY(), null);

            DockTitleBar.EventTask eventTask = new DockTitleBar.EventTask() {
                @Override
                public void run(Node node, Node dragNode) {
                    executions++;

                    if (dragNode != node) {
                        Event.fireEvent(node, dockEnterEvent.copyFor(DockNodeTitleBar.this, node));

                        if (dragNode != null) {
                            // fire the dock exit first so listeners
                            // can actually keep track of the node we
                            // are currently over and know when we
                            // aren't over any which DOCK_OVER
                            // does not provide
                            Event.fireEvent(dragNode, dockExitEvent.copyFor(DockNodeTitleBar.this, dragNode));
                        }

                        dragNodes.put(node.getScene().getWindow(), node);
                    }
                    Event.fireEvent(node, dockOverEvent.copyFor(DockNodeTitleBar.this, node));
                }
            };

            this.pickEventTarget(new Point2D(event.getScreenX(), event.getScreenY()), eventTask,
                    dockExitEvent);
        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
            dragging = false;

            DockEvent dockReleasedEvent
                    = new DockEvent(this.getStage(), this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_RELEASED, event.getX(),
                            event.getY(), event.getScreenX(), event.getScreenY(), null, dockNode.getDockPane());

            DockTitleBar.EventTask eventTask = new DockTitleBar.EventTask() {
                @Override
                public void run(Node node, Node dragNode) {
                    executions++;
                    if (dragNode != node) {
                        Event.fireEvent(node, dockReleasedEvent.copyFor(DockNodeTitleBar.this, node));
                    }
                    Event.fireEvent(node, dockReleasedEvent.copyFor(DockNodeTitleBar.this, node));
                }
            };

            this.pickEventTarget(new Point2D(event.getScreenX(), event.getScreenY()), eventTask, null);

            dragNodes.clear();

            // Remove temporary event handler for bug mentioned above.
            DockPane dockPane = dockNode.getDockPane();
            if (dockPane != null) {
                dockPane.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this);
                dockPane.removeEventFilter(MouseEvent.MOUSE_RELEASED, this);
            }
        }
    }
}
