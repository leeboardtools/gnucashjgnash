/*
 * Copyright 2017 Albert Santos.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package gnucashjgnash.action;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jgnash.engine.DataStoreType;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

import java.io.File;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import gnucashjgnash.GnuCashConvertUtil;
import gnucashjgnash.imports.GnuCashImport;

public class ImportGnuCashAction {

    private static final String LAST_DIR = "importDir";
    private String gnuCashFileName;
    private String jGnashFileName;
    private Stage progressStage;

    private ImportGnuCashAction() {
        // Utility class
    }

    public static void showAndWait(Stage stage) {
    		ImportGnuCashAction action = new ImportGnuCashAction();
    		action.showAndWaitImpl(stage);
    }
    
    private void showAndWaitImpl(Stage stage) {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final FileChooser fileChooser = configureFileChooser();
        fileChooser.setTitle(resources.getString("Title.SelFile"));

        DataStoreType dataStoreType = DataStoreType.BINARY_XSTREAM;

        final File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
        		return;
        }
        
        Preferences pref = Preferences.userNodeForPackage(ImportGnuCashAction.class);
        pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

        gnuCashFileName = file.toString();
        jGnashFileName = null;
        if (jGnashFileName == null) {
            jGnashFileName = FileUtils.stripFileExtension(gnuCashFileName) + dataStoreType.getDataStore().getFileExt();
        }
        
        ImportTask task = new ImportTask(gnuCashFileName, jGnashFileName, dataStoreType, stage);
        
        String plainGnuCashFileName = file.getName();
        
        progressStage = new Stage();
        progressStage.setTitle(GnuCashConvertUtil.getString("Title.Progress", plainGnuCashFileName));
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        //grid.setPadding(new Insets(25, 25, 25, 25));
        
        Scene scene = new Scene(grid, 500, 175);
        progressStage.setScene(scene);
        
        Text progressMessage = new Text();
        progressMessage.setWrappingWidth(400);
        grid.add(progressMessage, 0, 0, 3, 1);
        progressMessage.textProperty().bind(task.messageProperty());
        
        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        grid.add(progressBar, 0, 1, 3, 1);
        progressBar.progressProperty().bind(task.progressProperty());
        
        progressStage.initOwner(stage);
        progressStage.show();
        
        new Thread(task).start();
    }

    private static FileChooser configureFileChooser() {
        final Preferences pref = Preferences.userNodeForPackage(ImportGnuCashAction.class);
        final FileChooser fileChooser = new FileChooser();

        File initialDir = new File(pref.get(LAST_DIR, System.getProperty("user.home")));
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GnuCash Files (*.gnucash)", "*.gnucash", "*.xml")
        );

        return fileChooser;
    }

    private class ImportTask extends Task<Void> {

        private final String gnuCashFileName;
        private final String jGnashFileName;
        private final DataStoreType dataStoreType;
        private final Stage stage;
        private String errorMsg = null;

        ImportTask(final String gnuCashFileName, final String jGnashFileName, final DataStoreType dataStoreType, Stage stage) {
            this.gnuCashFileName = gnuCashFileName;
            this.jGnashFileName = jGnashFileName;
            this.dataStoreType = dataStoreType;
            this.stage = stage;
            setOnSucceeded(event -> onSuccess());
            setOnCancelled(event -> onCancelled());
        }

        @Override
        protected Void call() throws Exception {
            updateMessage(ResourceUtils.getString("Message.PleaseWait"));
            updateProgress(-1, Long.MAX_VALUE);

            final GnuCashImport importer = new GnuCashImport();
            if (!importer.convertGnuCashToJGnash(this.gnuCashFileName, this.jGnashFileName, this.dataStoreType,
                    new GnuCashImport.StatusCallback() {
                        @Override
                        public void updateStatus(long progress, long total, String statusMsg) {
                            if ((progress >= 0) || (total >= 0)) {
                                updateProgress(progress, total);
                            }
                            if (statusMsg != null) {
                                updateMessage(statusMsg);
                            }
                        }
                    })) {
                this.errorMsg = importer.getErrorMsg();
                System.out.println("GnuCash Import Failed: " + this.errorMsg);
                cancel();
                return null;
            }
            return null;
        }

        private void onCancelled() {
            final Alert alert = new Alert(Alert.AlertType.ERROR, this.errorMsg);

            alert.setTitle(GnuCashConvertUtil.getString("Title.Error.ImportFailed"));
            alert.initOwner(stage);

            alert.showAndWait();
            progressStage.close();
        }

        private void onSuccess() {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, this.errorMsg);

            alert.setTitle(GnuCashConvertUtil.getString("Title.ImportComplete"));
            alert.setContentText(GnuCashConvertUtil.getString("Message.ImportComplete", gnuCashFileName, jGnashFileName));
            alert.initOwner(stage);

            alert.showAndWait();
            progressStage.close();
        }
    }

}
