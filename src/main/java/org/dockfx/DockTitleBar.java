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

import java.util.HashMap;
import java.util.Stack;

import com.sun.javafx.stage.StageHelper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Base class for a dock node title bar that provides the mouse dragging functionality, captioning,
 * docking, and state manipulation.
 *
 * @since DockFX 0.1
 */
public abstract class DockTitleBar extends HBox implements EventHandler<MouseEvent> {
    /**
     * The label node used for captioning and the graphic.
     */
    protected final Label label;
    /**
     * State manipulation buttons including close, maximize, detach, and restore.
     */
    protected final Button closeButton, stateButton;

    /**
     * Creates a default DockTitleBar with captions and dragging behavior.
     *
     */
    public DockTitleBar(String style) {
        label = new Label("Dock Title Bar");

        stateButton = new Button();
        stateButton.setOnAction((ActionEvent event) -> {
            handleButtonAction();
        });

        closeButton = new Button();
        closeButton.setOnAction((ActionEvent event) -> {
            handleCloseAction();
        });

        // create a pane that will stretch to make the buttons right aligned
        Pane fillPane = new Pane();
        HBox.setHgrow(fillPane, Priority.ALWAYS);

        getChildren().addAll(label, fillPane, stateButton, closeButton);

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, this);
        this.addEventHandler(MouseEvent.DRAG_DETECTED, this);
        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, this);
        this.addEventHandler(MouseEvent.MOUSE_RELEASED, this);

        label.getStyleClass().add("dock-title-label");
        closeButton.getStyleClass().add("dock-close-button");
        stateButton.getStyleClass().add("dock-state-button");
        this.getStyleClass().add(style);
    }

    /**
     * Whether this title bar is currently being dragged.
     *
     * @return Whether this title bar is currently being dragged.
     */
    public final boolean isDragging() {
        return dragging;
    }

    /**
     * The label used for captioning and to provide a graphic.
     *
     * @return The label used for captioning and to provide a graphic.
     */
    public final Label getLabel() {
        return label;
    }

    /**
     * The button used for closing this title bar and its associated dock node.
     *
     * @return The button used for closing this title bar and its associated dock node.
     */
    public final Button getCloseButton() {
        return closeButton;
    }

    /**
     * The button used for detaching, maximizing, or restoring this title bar and its associated
     * dock node.
     *
     * @return The button used for detaching, maximizing, or restoring this title bar and its
     * associated dock node.
     */
    public final Button getStateButton() {
        return stateButton;
    }

    protected abstract Stage getStage();

    protected abstract void handleButtonAction();

    protected abstract void handleCloseAction();

    protected final void unbindLabelGraphic() {
        label.graphicProperty().unbind();
        label.setGraphic(null);
    }

    /**
     * The mouse location of the original click which we can use to determine the offset during
     * drag. Title bar dragging is asynchronous so it will not be negatively impacted by less
     * frequent or lagging mouse events as in the case of most current JavaFX implementations on
     * Linux.
     */
    protected Point2D dragStart;
    /**
     * Whether this title bar is currently being dragged.
     */
    protected boolean dragging = false;
    /**
     * The current node being dragged over for each window so we can keep track of enter/exit
     * events.
     */
    protected HashMap<Window, Node> dragNodes = new HashMap<Window, Node>();

    /**
     * The task that is to be executed when the dock event target is picked. This provides context
     * for what specific events and what order the events should be fired.
     *
     * @since DockFX 0.1
     */
    protected abstract class EventTask {
        /**
         * The number of times this task has been executed.
         */
        protected int executions = 0;

        /**
         * Creates a default DockTitleBar with captions and dragging behavior.
         *
         * @param node The node that was chosen as the event target.
         * @param dragNode The node that was last event target.
         */
        public abstract void run(Node node, Node dragNode);

        /**
         * The number of times this task has been executed.
         *
         * @return The number of times this task has been executed.
         */
        public int getExecutions() {
            return executions;
        }

        /**
         * Reset the execution count to zero.
         */
        public void reset() {
            executions = 0;
        }
    }

    /**
     * Traverse the scene graph for all open stages and pick an event target for a dock event based
     * on the location. Once the event target is chosen run the event task with the target and the
     * previous target of the last dock event if one is cached. If an event target is not found fire
     * the explicit dock event on the stage root if one is provided.
     *
     * @param location The location of the dock event in screen coordinates.
     * @param eventTask The event task to be run when the event target is found.
     * @param explicit The explicit event to be fired on the stage root when no event target is
     * found.
     */
    protected void pickEventTarget(Point2D location, EventTask eventTask, Event explicit) {
        // RFE for public scene graph traversal API filed but closed:
        // https://bugs.openjdk.java.net/browse/JDK-8133331

        ObservableList<Stage> stages
                = FXCollections.unmodifiableObservableList(StageHelper.getStages());
        // fire the dock over event for the active stages
        for (Stage targetStage : stages) {
            // obviously this title bar does not need to receive its own events
            // though users of this library may want to know when their
            // dock node is being dragged by subclassing it or attaching
            // an event listener in which case a new event can be defined or
            // this continue behavior can be removed
            if (targetStage == getStage()) {
                continue;
            }

            eventTask.reset();

            Node dragNode = dragNodes.get(targetStage);

            Parent root = targetStage.getScene().getRoot();
            Stack<Parent> stack = new Stack<>();
            if (root.contains(root.screenToLocal(location.getX(), location.getY()))
                    && !root.isMouseTransparent()) {
                stack.push(root);
            }
            // depth first traversal to find the deepest node or parent with no children
            // that intersects the point of interest
            while (!stack.isEmpty()) {
                Parent parent = stack.pop();
                // if this parent contains the mouse click in screen coordinates in its local bounds
                // then traverse its children
                boolean notFired = true;
                for (Node node : parent.getChildrenUnmodifiable()) {
                    if (node.contains(node.screenToLocal(location.getX(), location.getY()))
                            && !node.isMouseTransparent()) {
                        if (node instanceof Parent) {
                            stack.push((Parent) node);
                        } else {
                            eventTask.run(node, dragNode);
                        }
                        notFired = false;
                        break;
                    }
                }
                // if none of the children fired the event or there were no children
                // fire it with the parent as the target to receive the event
                if (notFired) {
                    eventTask.run(parent, dragNode);
                }
            }

            if (explicit != null && dragNode != null && eventTask.getExecutions() < 1) {
                Event.fireEvent(dragNode, explicit.copyFor(this, dragNode));
                dragNodes.put(targetStage, null);
            }
        }
    }
}
