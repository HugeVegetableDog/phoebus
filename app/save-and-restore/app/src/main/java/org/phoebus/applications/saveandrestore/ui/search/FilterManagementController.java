/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.phoebus.applications.saveandrestore.ui.search;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link FilterManagementController} class provides the controller for SearchWindow.fxml
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class FilterManagementController implements Initializable {

    private SaveAndRestoreController callerController;
    private List<Node> tableEntries = new ArrayList<>();

    @FXML
    private TextField queryTextField;

    @FXML
    private TableView<Filter> resultTableView;

    @FXML
    private TableColumn<Filter, String> nameColumn;

    @FXML
    private TableColumn<Filter, String> queryColumn;

    @FXML
    private TableColumn<Filter, String> lastUpdatedColumn;

    @FXML
    private TableColumn<Filter, String> userColumn;

    @FXML
    private TableColumn<Filter, Filter> saveColumn;

    @FXML
    private TableColumn<Filter, Filter> deleteColumn;

    private SaveAndRestoreService saveAndRestoreService;

    private static final Logger LOG = Logger.getLogger(FilterManagementController.class.getName());


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveAndRestoreService = SaveAndRestoreService.getInstance();

        resultTableView.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        queryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getQueryString()));
        lastUpdatedColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper(TimestampFormats.SECONDS_FORMAT.format(cell.getValue().getLastUpdated().toInstant())));
        userColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUserName()));

        saveColumn.setCellFactory(column -> new SaveTableCell());
        deleteColumn.setCellValueFactory(cellValue -> new SimpleObjectProperty<>(cellValue.getValue()));
        deleteColumn.setCellFactory(column -> new DeleteTableCell());

        loadFilters();
    }

    private void loadFilters() {
        try {
            List<Filter> filters = saveAndRestoreService.getAllFilters();
            resultTableView.getItems().setAll(filters);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to load filters", e);
        }
    }

    private class SaveTableCell extends TableCell<Filter, Filter> {
        @Override
        protected void updateItem(final Filter item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                Button button = new Button();
                button.setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/save_edit.png"));
                button.setTooltip(new Tooltip(Messages.saveFilter));
                button.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {

                    }
                });
                setGraphic(button);
            }
        }
    }

    private class DeleteTableCell extends TableCell<Filter, Filter> {
        @Override
        protected void updateItem(final Filter filter, final boolean empty) {
            super.updateItem(filter, empty);
            if (empty) {
                setGraphic(null);
            } else {
                Button button = new Button();
                button.setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
                button.setTooltip(new Tooltip(Messages.deleteFilter));
                button.setOnAction(event -> {
                    try {
                        saveAndRestoreService.deleteFilter(filter.getName());
                        loadFilters();
                        callerController.filterDeleted(filter);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Failed to delete filter", e);
                        ExceptionDetailsErrorDialog.openError(Messages.errorGeneric, Messages.faildDeleteFilter, e);
                    }
                });
                setGraphic(button);
            }
        }
    }

    public void setCallerController(SaveAndRestoreController callerController) {
        this.callerController = callerController;
    }

}

