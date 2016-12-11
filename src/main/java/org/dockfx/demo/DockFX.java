/**
 * @file DockFX.java
 * @brief Driver demonstrating basic dock layout with prototypes. Maintained in a separate package
 *        to ensure the encapsulation of org.dockfx private package members.
 *
 * @section License
 *
 *          This file is a part of the DockFX Library. Copyright (C) 2015 Robert B. Colton
 *
 *          This program is free software: you can redistribute it and/or modify it under the terms
 *          of the GNU Lesser General Public License as published by the Free Software Foundation,
 *          either version 3 of the License, or (at your option) any later version.
 *
 *          This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *          WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *          PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 *          You should have received a copy of the GNU Lesser General Public License along with this
 *          program. If not, see <http://www.gnu.org/licenses/>.
 **/

package org.dockfx.demo;

import java.util.Random;

import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPos;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class DockFX extends Application {

  private DockPane dockPane = new DockPane();

  public static void main(String[] args) {
    launch(args);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("DockFX");

    BorderPane borderPane = new BorderPane(dockPane);
    borderPane.setTop(createMenuBar());

    primaryStage.setScene(new Scene(borderPane, 800, 500));
    primaryStage.sizeToScene();

    primaryStage.show();

    // test the look and feel with both Caspian and Modena
    Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
    // initialize the default styles for the dock pane and undocked nodes using the DockFX
    // library's internal Default.css stylesheet
    // unlike other custom control libraries this allows the user to override them globally
    // using the style manager just as they can with internal JavaFX controls
    // this must be called after the primary stage is shown
    // https://bugs.openjdk.java.net/browse/JDK-8132900
    DockPane.initializeDefaultUserAgentStylesheet();

    // TODO: after this feel free to apply your own global stylesheet using the StyleManager class
  }

  private TreeView<String> generateRandomTree() {
    // create a demonstration tree view to use as the contents for a dock node
    TreeItem<String> root = new TreeItem<String>("Root");
    TreeView<String> treeView = new TreeView<String>(root);
    treeView.setShowRoot(false);

    // populate the prototype tree with some random nodes
    Random rand = new Random();
    for (int i = 4 + rand.nextInt(8); i > 0; i--) {
      TreeItem<String> treeItem = new TreeItem<String>("Item " + i);
      root.getChildren().add(treeItem);
      for (int j = 2 + rand.nextInt(4); j > 0; j--) {
        TreeItem<String> childItem = new TreeItem<String>("Child " + j);
        treeItem.getChildren().add(childItem);
      }
    }

    return treeView;
  }

  private MenuBar createMenuBar() {
    final MenuBar menuBar = new MenuBar();
    Menu menu = new Menu("Window");
    MenuItem mi = new MenuItem("New Window");
    mi.setOnAction(event -> {
      // load an image to caption the dock nodes
      Image dockImage = new Image(DockFX.class.getResource("docknode.png").toExternalForm());
      DockNode treeDock = new DockNode(generateRandomTree(), "Tree Dock", new ImageView(dockImage));
      treeDock.setPrefSize(100, 100);
      treeDock.dock(dockPane, DockPos.LEFT);

    });
    menu.getItems().add(mi);
    menuBar.getMenus().add(menu);
    return menuBar;
  }
}
