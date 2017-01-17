package org.dockfx.pane;

import java.util.Comparator;
import org.dockfx.DockNode;
import org.dockfx.DockPos;

import java.util.List;
import java.util.Stack;

import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TabPane;

/**
 * ContentTabPane holds multiple tabs
 *
 * @author HongKee Moon
 */
public class ContentTabPane extends TabPane implements ContentPane {

    ContentPane parent;

    public ContentTabPane() {
        this.setStyle("-fx-skin: \"org.dockfx.pane.skin.ContentTabPaneSkin\";");
    }

    @Override
    public Type getType() {
        return Type.TabPane;
    }

    @Override
    public void setContentParent(ContentPane pane) {
        parent = pane;
    }

    @Override
    public ContentPane getContentParent() {
        return parent;
    }

    @Override
    public ContentPane getSiblingParent(Stack<Parent> stack, Node sibling) {
        ContentPane pane = null;

        while (!stack.isEmpty()) {
            Parent lparent = stack.pop();

            List<Node> children = lparent.getChildrenUnmodifiable();

            if (lparent instanceof ContentPane) {
                children = ((ContentPane) lparent).getChildrenList();
            }

            for (int i = 0; i < children.size(); i++) {
                if (children.get(i) == sibling) {
                    pane = (ContentPane) lparent;
                } else if (children.get(i) instanceof Parent) {
                    stack.push((Parent) children.get(i));
                }
            }
        }
        return pane;
    }

    @Override
    public boolean removeNode(Stack<Parent> stack, Node node) {
        List<Node> children = getChildrenList();

        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == node) {
                getTabs().remove(i);
                return true;
            }
        }

        return false;
    }

    @Override
    public void set(int idx, Node node) {
        DockNode newNode = (DockNode) node;
        getTabs().set(idx, new DockNodeTab(newNode));
        getSelectionModel().select(idx);
    }

    @Override
    public void set(Node sibling, Node node) {
        set(getChildrenList().indexOf(sibling), node);
    }

    @Override
    public List<Node> getChildrenList() {
        return getTabs().stream().map(i -> i.getContent()).collect(Collectors.toList());
    }

    @Override
    public void addNode(Node root, Node sibling, Node node, DockPos dockPos) {
        DockNode newNode = (DockNode) node;
        DockNodeTab t = new DockNodeTab(newNode);
        addDockNodeTab(t);
    }

    public void addDockNodeTab(DockNodeTab dockNodeTab) {
        getTabs().add(dockNodeTab);
        getSelectionModel().select(dockNodeTab);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return getTabs().stream().map(i -> i.getContent().maxWidth(height)).min(Comparator.naturalOrder()).get();
    }
}
