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
public class DockPaneTitleBar extends DockTitleBar {
    /**
     * The window this node is a title bar for.
     */
    private final DockPane dockPane;

    /**
     * Creates a default DockTitleBar with captions and dragging behavior.
     *
     * @param dockPane The docking window that requires a title bar.
     */
    public DockPaneTitleBar(DockPane dockPane) {
        super("dock-pane-title-bar");
        label.textProperty().bind(dockPane.titleProperty());
        label.graphicProperty().bind(dockPane.graphicProperty());

        this.dockPane = dockPane;
    }

    public void mirrorNodeTitleBar(DockNode oldNode, DockNode newNode) {
        if ((null != oldNode) && (null != label.getGraphic()))
        {
            // return title bar graphic if we have one to the previous dock node
            unbindLabelGraphic();
            oldNode.getDockTitleBar().bindLabelGraphic();
        }
        
        if (null != newNode) {
            newNode.getDockTitleBar().unbindLabelGraphic();
            
            label.textProperty().unbind();
            label.graphicProperty().unbind();
            label.setText(newNode.getTitle());
            label.setGraphic(newNode.getGraphic());
            label.textProperty().bind(newNode.titleProperty());
            label.graphicProperty().bind(newNode.graphicProperty());
        } else {
            label.textProperty().unbind();
            label.graphicProperty().unbind();
            label.setText(dockPane.getTitle());
            label.setGraphic(dockPane.getGraphic());
            label.textProperty().bind(dockPane.titleProperty());
            label.graphicProperty().bind(dockPane.graphicProperty());
        }
    }

    @Override
    protected Stage getStage() {
        return dockPane.getStage();
    }

    @Override
    protected void handleCloseAction() {
        dockPane.close();
    }

    @Override
    protected void handleButtonAction() {
        dockPane.setMaximized(!dockPane.isMaximized());
    }

    @Override
    public void handle(MouseEvent event) {
        if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            if (dockPane.isFloating() && event.getClickCount() == 2
                    && event.getButton() == MouseButton.PRIMARY) {
                dockPane.setMaximized(!dockPane.isMaximized());
            } else {
                // drag detected is used in place of mouse pressed so there is some threshold for the
                // dragging which is determined by the default drag detection threshold
                dragStart = new Point2D(event.getX(), event.getY());
            }
        } else if (event.getEventType() == MouseEvent.DRAG_DETECTED) {
            if (dockPane.isFloating() && dockPane.isMaximized()) {
                double ratioX = event.getX() / dockPane.getWidth();
                double ratioY = event.getY() / dockPane.getHeight();

                // Please note that setMaximized is ruined by width and height changes occurring on the
                // stage and there is currently a bug report filed for this though I did not give them an
                // accurate test case which I should and wish I would have. This was causing issues in the
                // original release requiring maximized behavior to be implemented manually by saving the
                // restored bounds. The problem was that the resize functionality in DockNode.java was
                // executing at the same time canceling the maximized change.
                // https://bugs.openjdk.java.net/browse/JDK-8133334
                // restore/minimize the window after we have obtained its dimensions
                dockPane.setMaximized(false);

                // scale the drag start location by our restored dimensions
                dragStart = new Point2D(ratioX * dockPane.getWidth(), ratioY * dockPane.getHeight());
            }
            dragging = true;
            event.consume();
        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            if (dockPane.isFloating() && event.getClickCount() == 2
                    && event.getButton() == MouseButton.PRIMARY) {
                event.setDragDetect(false);
                event.consume();
                return;
            }

            if (!dragging) {
                return;
            }

            Stage stage = dockPane.getStage();
            Insets insetsDelta = dockPane.getBorderPane().getInsets();

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
                        Event.fireEvent(node, dockEnterEvent.copyFor(DockPaneTitleBar.this, node));

                        if (dragNode != null) {
                            // fire the dock exit first so listeners
                            // can actually keep track of the node we
                            // are currently over and know when we
                            // aren't over any which DOCK_OVER
                            // does not provide
                            Event.fireEvent(dragNode, dockExitEvent.copyFor(DockPaneTitleBar.this, dragNode));
                        }

                        dragNodes.put(node.getScene().getWindow(), node);
                    }
                    Event.fireEvent(node, dockOverEvent.copyFor(DockPaneTitleBar.this, node));
                }
            };

            this.pickEventTarget(new Point2D(event.getScreenX(), event.getScreenY()), eventTask,
                    dockExitEvent);
        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
            dragging = false;

            DockEvent dockReleasedEvent
                    = new DockEvent(this.getStage(), this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_RELEASED, event.getX(),
                            event.getY(), event.getScreenX(), event.getScreenY(), null, dockPane);

            DockTitleBar.EventTask eventTask = new DockTitleBar.EventTask() {
                @Override
                public void run(Node node, Node dragNode) {
                    executions++;
                    if (dragNode != node) {
                        Event.fireEvent(node, dockReleasedEvent.copyFor(DockPaneTitleBar.this, node));
                    }
                    Event.fireEvent(node, dockReleasedEvent.copyFor(DockPaneTitleBar.this, node));
                }
            };

            this.pickEventTarget(new Point2D(event.getScreenX(), event.getScreenY()), eventTask, null);

            dragNodes.clear();

            // Remove temporary event handler for bug mentioned above.
            if (dockPane != null) {
                dockPane.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this);
                dockPane.removeEventFilter(MouseEvent.MOUSE_RELEASED, this);
            }
        }
    }
}
