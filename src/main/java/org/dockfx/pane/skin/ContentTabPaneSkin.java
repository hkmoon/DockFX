package org.dockfx.pane.skin;

import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;

import org.dockfx.pane.DockNodeTab;

/**
 * Custom skin for ContentTabPane that extends the public JavaFX TabPaneSkin.
 *
 * The original class was a full copy of the internal
 * com.sun.javafx.scene.control.skin.TabPaneSkin from Java 8. The only
 * DockFX-specific customization was a modified TabMenuItem that used
 * DockNodeTab.titleProperty() for the overflow popup menu text, since
 * DockNodeTab tabs use custom graphics rather than text.
 *
 * This rewrite delegates all standard behavior to the public TabPaneSkin
 * and binds each DockNodeTab's text property to its title so the overflow
 * popup menu displays correctly.
 *
 * @author HongKee Moon
 */
public class ContentTabPaneSkin extends TabPaneSkin
{
  public ContentTabPaneSkin(TabPane tabPane)
  {
    super(tabPane);

    for (Tab tab : tabPane.getTabs())
    {
      bindTabText(tab);
    }

    tabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
      while (c.next())
      {
        if (c.wasAdded())
        {
          for (Tab tab : c.getAddedSubList())
          {
            bindTabText(tab);
          }
        }
      }
    });
  }

  private void bindTabText(Tab tab)
  {
    if (tab instanceof DockNodeTab)
    {
      tab.textProperty().bind(((DockNodeTab) tab).titleProperty());
    }
  }
}
