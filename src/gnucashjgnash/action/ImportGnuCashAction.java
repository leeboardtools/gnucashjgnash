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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import jgnash.engine.DataStoreType;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

import java.io.File;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import gnucashjgnash.GnuCashConvertUtil;
import gnucashjgnash.NoticeTree;
import gnucashjgnash.NoticeTree.SourceEntry;
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
        private NoticeTree warningNoticeTree = null;

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
            boolean result = importer.convertGnuCashToJGnash(this.gnuCashFileName, this.jGnashFileName, this.dataStoreType,
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
            });
            this.warningNoticeTree = importer.getWarningNoticeTree();
            if (!result) {
                this.errorMsg = importer.getErrorMsg();
                System.out.println("GnuCash Import Failed: " + this.errorMsg);
                cancel();
                return null;
            }
            return null;
        }

        private void onCancelled() {
        	displayFinalStatus();
        }

        private void onSuccess() {
        	displayFinalStatus();
        }
        
        
        private void displayFinalStatus() {
        	progressStage.close();
        	
        	if ((this.errorMsg != null) && !this.errorMsg.isEmpty()) {
                final Alert alert = new Alert(Alert.AlertType.ERROR, this.errorMsg);

                alert.setTitle(GnuCashConvertUtil.getString("Title.Error.ImportFailed"));
                alert.initOwner(stage);

                alert.showAndWait();
        	}
        	else if ((this.warningNoticeTree == null) || !this.warningNoticeTree.isNotices()) {
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, this.errorMsg);

                alert.setTitle(GnuCashConvertUtil.getString("Title.ImportComplete"));
                alert.setContentText(GnuCashConvertUtil.getString("Message.ImportComplete", gnuCashFileName, jGnashFileName));
                alert.initOwner(stage);

                alert.showAndWait();
        	}
        	else {
        		Stage stage = new Stage();
        		stage.setTitle(GnuCashConvertUtil.getString("Title.Warnings"));
        		stage.setMinWidth(500);
        		stage.setMinHeight(300);
        		
        		VBox pane = new VBox();
        		pane.setAlignment(Pos.CENTER);
        		pane.setPadding(new Insets(10));
        		
        		Text caption = new Text(GnuCashConvertUtil.getString("Message.WarningsCaption"));
        		pane.getChildren().add(caption);

        		WarningTreeItem root = new WarningTreeItem(this.warningNoticeTree.getRootSourceEntry());
        		TreeView<NoticeTree.SourceEntry> treeView = new TreeView<NoticeTree.SourceEntry>(root);
        		treeView.setShowRoot(false);
        		pane.getChildren().add(treeView);
        		
        		Button closeButton = new Button(GnuCashConvertUtil.getString("Button.Close"));
        		closeButton.setDefaultButton(true);
        		pane.getChildren().add(closeButton);
        		closeButton.setOnAction(e -> stage.close());
        		
        		Scene scene = new Scene(pane);
        		stage.setScene(scene);
        		
        		stage.showAndWait();
        	}
        }
        
    }

    static class WarningTreeItem extends TreeItem<NoticeTree.SourceEntry> {
    	final NoticeTree.SourceEntry sourceEntry;
    	boolean isChildrenLoaded = false;
    	
    	WarningTreeItem(NoticeTree.SourceEntry sourceEntry) {
    		this.sourceEntry = sourceEntry;
    		setValue(sourceEntry);
    	}

		/* (non-Javadoc)
		 * @see javafx.scene.control.TreeItem#getChildren()
		 */
		@Override
		public ObservableList<TreeItem<NoticeTree.SourceEntry>> getChildren() {
			buildChildren();
			return super.getChildren();
		}
    	
		/* (non-Javadoc)
		 * @see javafx.scene.control.TreeItem#isLeaf()
		 */
		@Override
		public boolean isLeaf() {
			return !this.sourceEntry.hasChildren();
		}

		private void buildChildren() {
			if (!this.isChildrenLoaded) {
				NoticeTree.SourceEntry [] childSourceEntries = this.sourceEntry.getChildren();
				if (childSourceEntries.length > 0) {
					ObservableList<WarningTreeItem> children = FXCollections.observableArrayList();
					for (NoticeTree.SourceEntry sourceEntry : childSourceEntries) {
						children.add(new WarningTreeItem(sourceEntry));
					}
					
					super.getChildren().setAll(children);
				}
				
				this.isChildrenLoaded = true;
			}
		}
    }
}
