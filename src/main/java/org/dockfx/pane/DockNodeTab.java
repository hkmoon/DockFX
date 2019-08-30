package org.dockfx.pane;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Tab;

import org.dockfx.DockNode;

/**
 * DockNodeTab class holds Tab for ContentTabPane
 *
 * @author HongKee Moon
 */
public class DockNodeTab extends Tab
{

  final private DockNode dockNode;

  final private SimpleStringProperty title;

  public DockNodeTab(DockNode node)
  {
    this.dockNode = node;
    setClosable(false);

    title = new SimpleStringProperty("");
    title.bind(dockNode.titleProperty());

    setGraphic(dockNode.getDockTitleBar());
    setContent(dockNode);
    dockNode.tabbedProperty().set(true);
    dockNode.setNodeTab(this);
  }

  public String getTitle()
  {
    return title.getValue();
  }

  public SimpleStringProperty titleProperty()
  {
    return title;
  }

  public void select()
  {
    getTabPane().getSelectionModel().select(this);
  }
}
