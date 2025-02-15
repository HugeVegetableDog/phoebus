/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.NodeChangedListener;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.io.IOException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Tab} subclass showing a view for the purpose of taking a snapshot, or restoring one.
 * These two use cases are split in terms of fxml files and controller classes in order to facilitate development
 * and maintenance. Drawback is that this class will need to switch between the two in order to switch
 * view when a new snapshot has been saved.
 */
public class SnapshotTab extends Tab implements NodeChangedListener {

    public SaveAndRestoreService saveAndRestoreService;

    private final SimpleStringProperty tabTitleProperty = new SimpleStringProperty();

    private SnapshotController snapshotController;

    private final SimpleObjectProperty<Image> tabGraphicImageProperty = new SimpleObjectProperty<>();


    public SnapshotTab(org.phoebus.applications.saveandrestore.model.Node node, SaveAndRestoreService saveAndRestoreService) {

        this.saveAndRestoreService = saveAndRestoreService;

        if (node.getNodeType().equals(NodeType.SNAPSHOT)) {
            setId(node.getUniqueId());
        }

        HBox container = new HBox();
        ImageView imageView = new ImageView();
        imageView.imageProperty().bind(tabGraphicImageProperty);
        Label label = new Label("");
        label.textProperty().bind(tabTitleProperty);
        HBox.setMargin(label, new Insets(1, 0, 0, 5));
        container.getChildren().addAll(imageView, label);

        setGraphic(container);

        tabTitleProperty.set(node.getNodeType().equals(NodeType.SNAPSHOT) ? node.getName() : Messages.unnamedSnapshot);

        boolean isGolden = node.getTags() != null && node.getTags().stream().anyMatch(t -> t.getName().equals(Tag.GOLDEN));

        tabGraphicImageProperty.set(isGolden ? ImageRepository.GOLDEN_SNAPSHOT : ImageRepository.SNAPSHOT);

        setOnCloseRequest(event -> {
            if (snapshotController != null && !snapshotController.handleSnapshotTabClosed()) {
                event.consume();
            } else {
                SaveAndRestoreService.getInstance().removeNodeChangeListener(this);
            }
        });

        SaveAndRestoreService.getInstance().addNodeChangeListener(this);
    }

    public void updateTabTitle(String name) {
        Platform.runLater(() -> tabTitleProperty.set(name));
    }

    public void setGoldenImage() {
        tabGraphicImageProperty.set(ImageRepository.GOLDEN_SNAPSHOT);
    }

    public void setCompositeSnapshotImage() {
        tabGraphicImageProperty.set(ImageRepository.COMPOSITE_SNAPSHOT);
    }

    public void newSnapshot(org.phoebus.applications.saveandrestore.model.Node configurationNode) {
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(SnapshotTab.class.getResource("SnapshotView.fxml"));

        loader.setControllerFactory(clazz -> {
            try {
                if (clazz.isAssignableFrom(SnapshotController.class)) {
                    return clazz.getConstructor(SnapshotTab.class)
                            .newInstance(this);
                }
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError("Error",
                        "Failed to open new snapshot tab", e);
            }
            return null;
        });

        try {
            setContent(loader.load());
            snapshotController = loader.getController();
        } catch (IOException e) {
            Logger.getLogger(SnapshotTab.class.getName())
                    .log(Level.SEVERE, "Failed to load fxml", e);
            return;
        }
        setId(null);
        snapshotController.newSnapshot(configurationNode);
    }

    public void loadSnapshot(Node snapshotNode) {
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(resourceBundle);
            loader.setLocation(SnapshotTab.class.getResource("RestoreSnapshotView.fxml"));

            loader.setControllerFactory(clazz -> {
                try {
                    if (clazz.isAssignableFrom(RestoreSnapshotController.class)) {
                        return clazz.getConstructor(SnapshotTab.class)
                                .newInstance(this);
                    }
                } catch (Exception e) {
                    ExceptionDetailsErrorDialog.openError("Error",
                            "Failed to open new snapshot tab", e);
                }
                return null;
            });

            try {
                setContent(loader.load());
                snapshotController = loader.getController();
            } catch (IOException e) {
                Logger.getLogger(SnapshotTab.class.getName())
                        .log(Level.SEVERE, "Failed to load fxml", e);
                return;
            }
        ((RestoreSnapshotController)snapshotController).loadSnapshot(snapshotNode);
    }

    public void addSnapshot(org.phoebus.applications.saveandrestore.model.Node node) {
        snapshotController.addSnapshot(node);
    }

    @Override
    public void nodeChanged(Node node) {
        if (node.getUniqueId().equals(getId())) {
            Platform.runLater(() -> {
                tabTitleProperty.set(node.getName());
                snapshotController.setSnapshotNameProperty(node.getName());
            });
        }
    }

    public Node getSnapshotNode() {
        return snapshotController.getSnapshot(0).getSnapshotNode();
    }

    public Node getConfigNode() {
        return snapshotController.getConfigurationNode();
    }
}
