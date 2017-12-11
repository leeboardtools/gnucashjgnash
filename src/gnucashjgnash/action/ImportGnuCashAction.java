package gnucashjgnash.action;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jgnash.convert.imports.GenericImport;
import jgnash.convert.imports.ImportTransaction;
import jgnash.engine.Account;
import jgnash.engine.DataStoreType;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import gnucashjgnash.GnuCashConvertUtil;
import gnucashjgnash.imports.GnuCashImport;

public class ImportGnuCashAction {

    private static final String LAST_DIR = "importDir";

    private ImportGnuCashAction() {
        // Utility class
    }

    public static void showAndWait(Stage stage) {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final FileChooser fileChooser = configureFileChooser();
        fileChooser.setTitle(resources.getString("Title.SelFile"));

        DataStoreType dataStoreType = DataStoreType.H2MV_DATABASE;

        final File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(ImportGnuCashAction.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

            String gnuCashFileName = file.toString();
            String jGnashFileName = null;
            if (jGnashFileName == null) {
                jGnashFileName = FileUtils.stripFileExtension(gnuCashFileName) + dataStoreType.getDataStore().getFileExt();
            }
            new Thread(new ImportTask(gnuCashFileName, jGnashFileName, dataStoreType, stage)).start();
        }
    }

    private static FileChooser configureFileChooser() {
        final Preferences pref = Preferences.userNodeForPackage(ImportGnuCashAction.class);
        final FileChooser fileChooser = new FileChooser();

        fileChooser.setInitialDirectory(new File(pref.get(LAST_DIR, System.getProperty("user.home"))));

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GnuCash Files (*.gnucash)", "*.gnucash", "*.xml")
        );

        return fileChooser;
    }

    private static class ImportTask extends Task<Void> {

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
            //Platform.runLater(alert::showAndWait);
        }

        private void onSuccess() {
/*            final GnuCashImport qifImport = getValue();
            final GnuCashParser parser = qifImport.getParser();

            final ImportWizard importWizard = new ImportWizard();

            final WizardDialogController<ImportWizard.Settings> wizardDialogController
                    = importWizard.wizardControllerProperty().get();

            importWizard.dateFormatSelectionEnabled().set(true);

            final GnuCashAccount qAccount = parser.getBank();

            wizardDialogController.setSetting(ImportWizard.Settings.BANK, qAccount);

            importWizard.showAndWait();

            if (wizardDialogController.validProperty().get()) {
                final Account account = (Account) wizardDialogController.getSetting(ImportWizard.Settings.ACCOUNT);

                @SuppressWarnings("unchecked")
                final List<ImportTransaction> transactions = (List<ImportTransaction>) wizardDialogController.getSetting(ImportWizard.Settings.TRANSACTIONS);

                // import threads in the background
                ImportTransactionsTask importTransactionsTask = new ImportTransactionsTask(account, transactions);

                new Thread(importTransactionsTask).start();

                StaticUIMethods.displayTaskProgress(importTransactionsTask);
            }
            */
        }
    }

    private static class ImportTransactionsTask extends Task<Void> {

        private final Account account;
        private final List<ImportTransaction> transactions;

        ImportTransactionsTask(final Account account, final List<ImportTransaction> transactions) {
            this.account = account;
            this.transactions = transactions;
        }

        @Override
        public Void call() {
            updateMessage(ResourceUtils.getString("Message.PleaseWait"));
            updateProgress(-1, Long.MAX_VALUE);

            /* Import the transactions */
            GenericImport.importTransactions(transactions, account);

            return null;
        }
    }
}
