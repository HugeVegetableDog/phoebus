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
 */

package org.phoebus.logbook.olog.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.olog.es.api.model.LogGroupProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

/**
 * Controller for the merged log entry view. The idea is to create a single view of all the entries
 * in a log entry group such that the entire content of the group (aka thread) can be read without the
 * need to select the individual log entries from a list.
 * <p>
 * The attachments and properties of the individual log entries are intentionally left out as the
 * purpose is to read a sequence of text, and not to show all details.
 * <p>
 * Embedded attachments (i.e. images) are maintained in the merged view, as are - of course - other
 * Markdown elements like lists or tables.
 */
public class MergedLogEntryDisplayController extends HtmlAwareController {

    @FXML
    WebView logDescription;

    private final LogClient logClient;

    private final ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();

    private final Logger logger = Logger.getLogger(MergedLogEntryDisplayController.class.getName());

    private WebEngine webEngine;

    public MergedLogEntryDisplayController(LogClient logClient) {
        super(logClient.getServiceUrl());
        this.logClient = logClient;
    }

    @FXML
    public void initialize() {
        // Content is defined by the source (default) or description field. If both are null
        // or empty, do no load any content to the WebView.
        webEngine = logDescription.getEngine();
        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail_log_webview.css").toExternalForm());
    }

    /**
     * Set the selected entry containing the log group property to be used for finding
     * other entries in the group. Note that this will query the service
     * such that all log records in the group are retrieved, irrespective of how
     * the specified log record was obtained.
     *
     * @param logEntry The log entry selected by user in the table/list view.
     */
    public void setLogEntry(LogEntry logEntry) {
        getLogEntries(logEntry);
    }

    private void mergeAndRender(LogEntry selectedLogEntry) {
        StringBuilder stringBuilder = new StringBuilder();
        logEntries.forEach(l -> {
            if (l.getId().equals(selectedLogEntry.getId())) {
                stringBuilder.append("<div class='selected-log-entry'>");
            }
            stringBuilder.append(createSeparator(l));

            stringBuilder.append(toHtml(l.getSource()));
            if (l.getId().equals(selectedLogEntry.getId())) {
                stringBuilder.append("</div>");
            }
        });
        webEngine.loadContent(stringBuilder.toString());
    }

    /**
     * Creates the log entry separator item inserted as a header for each log entry
     * when showing the log group view.
     *
     * @param logEntry
     * @return
     */
    private String createSeparator(LogEntry logEntry) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<div class='separator'>");
        stringBuilder.append(SECONDS_FORMAT.format(logEntry.getCreatedDate())).append(", ");
        stringBuilder.append(logEntry.getOwner()).append(", ");
        stringBuilder.append(logEntry.getTitle());
        stringBuilder.append("<div style='float: right;'>").append(logEntry.getId()).append("</div>");
        stringBuilder.append("</div>");
        return stringBuilder.toString();
    }

    private void getLogEntries(LogEntry logEntry) {

        String id =
                logEntry.getProperties().stream()
                        .filter(p -> p.getName().equals(LogGroupProperty.NAME)).findFirst().get().getAttributes().get(LogGroupProperty.ATTRIBUTE_ID);
        logger.log(Level.INFO, "Fetching log entries for group " + id);
        try {
            Map<String, String> mMap = new HashMap<>();
            mMap.put("properties", LogGroupProperty.NAME + ".id." + id);
            logEntries.setAll(logClient.findLogs(mMap));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to locate log entry items using log entry group id " + id, e);
        }

        mergeAndRender(logEntry);
    }
}
