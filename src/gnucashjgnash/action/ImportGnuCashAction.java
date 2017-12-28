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
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jgnash.engine.DataStoreType;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import gnucashjgnash.GnuCashConvertUtil;
import gnucashjgnash.NoticeTree;
import gnucashjgnash.imports.GnuCashImport;
import gnucashjgnash.imports.ParsedEntry;

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
        
        
        private void showMessage(String message, String titleId) {
    		Stage stage = new Stage();
            stage.setTitle(GnuCashConvertUtil.getString(titleId));
            stage.setMinWidth(500);
            //stage.setMinHeight(200);
            
    		Insets margins = new Insets(10);

            VBox pane = new VBox();
            pane.setPadding(margins);
            
            Text messageText = new Text(message);
            pane.getChildren().add(messageText);
            VBox.setMargin(messageText, margins);
            
            Text fileNameText = new Text(GnuCashConvertUtil.getString("Message.GnuCashFileName", gnuCashFileName));
            pane.getChildren().add(fileNameText);
            VBox.setMargin(fileNameText, margins);
            
            fileNameText = new Text(GnuCashConvertUtil.getString("Message.JGnashFileName", jGnashFileName));
            pane.getChildren().add(fileNameText);
            VBox.setMargin(fileNameText, margins);

    		Button closeButton = new Button(GnuCashConvertUtil.getString("Button.Close"));
    		closeButton.setDefaultButton(true);
    		
    		VBox buttonPane = new VBox();
    		buttonPane.setAlignment(Pos.CENTER);
    		buttonPane.getChildren().add(closeButton);
    		VBox.setMargin(buttonPane, margins);
    		pane.getChildren().add(buttonPane);
    		closeButton.setOnAction(e -> stage.close());
    		
    		Scene scene = new Scene(pane);
    		stage.setScene(scene);
    		
    		stage.showAndWait();
        }
        
        private void displayFinalStatus() {
        	progressStage.close();
        	
        	if ((this.errorMsg != null) && !this.errorMsg.isEmpty()) {
        		showMessage(GnuCashConvertUtil.getString("Message.ImportFailed", this.errorMsg), "Title.ImportFailed");
        	}
        	else if ((this.warningNoticeTree == null) || !this.warningNoticeTree.isNotices()) {
        		showMessage(GnuCashConvertUtil.getString("Message.ImportComplete"), "Title.ImportComplete");
        	}
        	else {
        		String warningsFileName = saveWarnings();
        		String captionMsg;
        		if ((warningsFileName != null) && !warningsFileName.isEmpty()) {
        			captionMsg = GnuCashConvertUtil.getString("Message.WarningsFileSavedCaption", warningsFileName);
        		}
        		else {
        			captionMsg = GnuCashConvertUtil.getString("Message.WarningsCaption");
        		}
        		
        		Stage stage = new Stage();
        		stage.setTitle(GnuCashConvertUtil.getString("Title.Warnings"));
        		stage.setMinWidth(500);
        		stage.setMinHeight(300);
        		
        		Insets margins = new Insets(10);
        		VBox pane = new VBox();
        		
        		Text caption = new Text(captionMsg);
        		pane.getChildren().add(caption);
        		VBox.setMargin(caption, margins);

        		WarningTreeItem root = new WarningTreeItem(this.warningNoticeTree.getRootSourceEntry());
        		TreeView<NoticeTree.SourceEntry> treeView = new TreeView<NoticeTree.SourceEntry>(root);
        		treeView.setShowRoot(false);
        		pane.getChildren().add(treeView);
        		VBox.setMargin(treeView, margins);

        		Button closeButton = new Button(GnuCashConvertUtil.getString("Button.Close"));
        		closeButton.setDefaultButton(true);
        		
        		VBox buttonPane = new VBox();
        		buttonPane.setAlignment(Pos.CENTER);
        		buttonPane.getChildren().add(closeButton);
        		VBox.setMargin(buttonPane, margins);
        		pane.getChildren().add(buttonPane);
        		closeButton.setOnAction(e -> stage.close());
        		
        		Scene scene = new Scene(pane);
        		stage.setScene(scene);
        		
        		stage.showAndWait();
        	}
        }
        
        
        private String saveWarnings() {
        	File gnuCashFile = new File(this.gnuCashFileName);
        	String path = gnuCashFile.getParent();
        	File file = new File(path, "Warnings.TXT");
        	
        	try {
				FileWriter writer = new FileWriter(file);
				
	        	String newline = System.lineSeparator();
				writer.write("Warnings from converting:" + newline);
				writer.write("\t" + this.gnuCashFileName + newline);
				writer.write("to" + newline);
				writer.write("\t" + this.jGnashFileName + newline);

				String indent = "";
				for (NoticeTree.SourceEntry sourceEntry : this.warningNoticeTree.getRootSourceEntry().getChildren()) {
					writeWarning(writer, indent, sourceEntry);
				}
				
				writer.close();
			} catch (IOException e) {
				return null;
			}
        	
        	return file.getPath();
        }
        
        private void writeWarning(FileWriter writer, String indent, NoticeTree.SourceEntry sourceEntry) throws IOException {
        	String newline = System.lineSeparator();
        	if (!(sourceEntry.getSource() instanceof NoticeTree.TextSource)) {
        		writer.write(newline);
        	}
        	
        	String title = sourceEntry.getSource().getSourceTitle();
        	if (title != null) {
        		writer.write(indent);
        		writer.write(title);
        		writer.write(newline);
        	}

        	String description = sourceEntry.getSource().getSourceDescription();
        	if (description != null) {
        		writer.write(indent);
        		writer.write(description);
        		writer.write(newline);
        	}
        	
        	if (sourceEntry.hasChildren()) {
	        	indent += '\t';
	        	for (NoticeTree.SourceEntry child : sourceEntry.getChildren()) {
	        		writeWarning(writer, indent, child);
	        	}
        	}
        	else if (sourceEntry.getSource() instanceof NoticeTree.TextSource){
        		NoticeTree.TextSource textSource = (NoticeTree.TextSource)sourceEntry.getSource();
        		if (textSource.getParentSource() instanceof ParsedEntry) {
	        		ParsedEntry parsedEntry = (ParsedEntry)textSource.getParentSource();
	        		if (parsedEntry != null) {
	        			int lineNumber = parsedEntry.getLineNumber();
	        			int columnNumber = parsedEntry.getColumnNumber();
	        			if ((lineNumber >= 0) && (columnNumber >= 0)) {
	        				writer.write(indent);
	        				writer.write("Line:\t" + lineNumber + "\tColumn:\t" + columnNumber);
	                		writer.write(newline);
	                		writer.write(newline);
	        			}
	        		}
        		}
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
