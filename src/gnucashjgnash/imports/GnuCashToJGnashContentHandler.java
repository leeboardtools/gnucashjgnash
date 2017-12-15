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
package gnucashjgnash.imports;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.SecurityNode;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import gnucashjgnash.GnuCashConvertUtil;
import java.util.*;
import java.util.logging.Logger;

public class GnuCashToJGnashContentHandler implements ContentHandler {
    private static final Logger LOG = Logger.getLogger(GnuCashToJGnashContentHandler.class.getName());

    final Engine engine;
    final GnuCashImport.StatusCallback statusCallback;
    final List<StateHandler> stateHandlers = new ArrayList<>();
    StateHandler activeStateHandler;

    final Map<String, Integer> countData = new HashMap<>();
    
    final IdEntry bookId = new IdEntry();
    final Map<String, SlotEntry> bookSlots = new HashMap<>();

    final Map<String, CommodityEntry> commodityEntries = new HashMap<>();
    final Map<String, SecurityNode> jGnashSecurities = new HashMap<>();
    final Map<String, CurrencyNode> jGnashCurrencies = new HashMap<>();

    final Map<String, PriceEntry> priceEntries = new HashMap<>();

    final Map<String, AccountImportEntry> accountImportEntries = new HashMap<>();
    final Set<String> accountIdsInUse = new HashSet<>();

    final Set<String> accountIdsToIgnore = new HashSet<>();

    final Map<String, Account> jGnashAccounts = new HashMap<>();

    final Map<String, TransactionImportEntry> transactionEntries = new HashMap<>();
    

    final Set<String> recordedWarningMsgIds = new HashSet<>();
    final List<String> recordedWarnings = new ArrayList<>();
    String errorMsg;


    GnuCashToJGnashContentHandler(Engine engine, GnuCashImport.StatusCallback statusCallback) {
        this.engine = engine;
        this.statusCallback = statusCallback;
    }

    protected void pushStateHandler(StateHandler stateHandler) {
        this.stateHandlers.add(stateHandler);
        this.activeStateHandler = stateHandler;
    }

    protected void popStateHandler() {
        this.stateHandlers.remove(stateHandlers.size() -1);
        this.activeStateHandler = stateHandlers.get(stateHandlers.size() - 1);
        if (this.activeStateHandler != null) {
            this.activeStateHandler.stateHandlerReactivated();
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
        this.errorMsg = null;
        this.recordedWarningMsgIds.clear();
        this.recordedWarnings.clear();

        this.stateHandlers.clear();
        pushStateHandler(new OuterStateHandler(this));
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.stateHandlers.size() != 1) {
            LOG.warning("The number of state handlers at endDocument() is not 1! this.stateHandlers.size()=" + this.stateHandlers.size());
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        activeStateHandler.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        activeStateHandler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        activeStateHandler.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {

    }

    @Override
    public void skippedEntity(String name) throws SAXException {

    }


    interface StateHandler {
        void handleStateAttributes(Attributes atts);

        void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException;
        void endElement(String uri, String localName, String qName) throws SAXException;

        void characters(char[] ch, int start, int length) throws SAXException;

        void stateHandlerReactivated();

        void recordWarning(String msgId, String key, Object ... arguments);
    }



    interface StateHandlerCreator {
        StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                                        String elementName);
    }



    abstract static class AbstractStateHandler implements StateHandler {
        final GnuCashToJGnashContentHandler contentHandler;
        final StateHandler parentStateHandler;
        final String elementName;
        String characters = "";
        boolean ignoreChildElements = false;

        AbstractStateHandler(final GnuCashToJGnashContentHandler contentHandler,
                             final StateHandler parentStateHandler, final String elementName,
                             final Map<String, StateHandlerCreator> qNameToStateHandlers) {
            this.contentHandler = contentHandler;
            this.parentStateHandler = parentStateHandler;
            this.elementName = elementName;
        }

        @Override
        public void handleStateAttributes(Attributes atts) {

        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            StateHandler stateHandler = getStateHandlerForElement(qName);

            if (stateHandler == null) {
                stateHandler = new NOP_StateHandler(this.contentHandler, this, qName);
            }

            this.contentHandler.pushStateHandler(stateHandler);
            stateHandler.handleStateAttributes(atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (this.elementName.equals(qName)) {
                this.endState();
                this.contentHandler.popStateHandler();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            this.characters += new String(ch, start, length);
        }

        @Override
        public void stateHandlerReactivated() {
        }

		protected StateHandler getStateHandlerForElement(String qName) {
			return new NOP_StateHandler(this.contentHandler, this, qName);
		}
        
        protected void endState() {
        }

        @Override
        public void recordWarning(String msgId, String key, Object ... arguments) {
            this.contentHandler.recordWarning(msgId, key, arguments);
        }
    }



    static class NOP_StateHandler extends AbstractStateHandler {
        NOP_StateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                         String elementName) {
            super(contentHandler, parentStateHandler, elementName,null);
        }

        @Override
        protected void endState() {
            super.endState();
            System.out.println("NOP_StateHandler.endState()..." + this.elementName);
        }

    }



    static class OuterStateHandler extends AbstractStateHandler {

        OuterStateHandler(GnuCashToJGnashContentHandler contentHandler) {
            super(contentHandler, null, null, null);
        }

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "gnc-v2":
				return new GNC_V2_StateHandler(this.contentHandler, this, qName); 

			}

			return super.getStateHandlerForElement(qName);
		}

    }


    static class GNC_V2_StateHandler extends AbstractStateHandler {
        GNC_V2_StateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName, null);

        }

        /* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "gnc:count-data":
				return new CountDataStateHandler(this.contentHandler, this, qName);

			case "gnc:book":
				return new BookStateHandler(this.contentHandler, this, qName);
			}
			return super.getStateHandlerForElement(qName);
		}

		@Override
        protected void endState() {
            super.endState();
            System.out.println("GNC_V2_StateHandler.endState()...");
        }
    }


    static class CountDataStateHandler extends AbstractStateHandler {
        String cdType;

        CountDataStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                                    String elementName) {
            super(contentHandler, parentStateHandler, elementName,null);
        }

        @Override
        public void handleStateAttributes(Attributes atts) {
            super.handleStateAttributes(atts);

            this.cdType = atts.getValue("cd:type");
        }

        @Override
        protected void endState() {
            super.endState();

            if (this.cdType == null) {
                recordWarning("MissingCDType", "Message.Parse.XMLMissingAttribute", this.elementName, "cd:type");
            }
            try {
                int count = Integer.parseInt(this.characters);
                if (this.contentHandler.countData.get(this.cdType) != null) {
                    recordWarning("MultipleCountDataEntries_" + this.cdType, "Message.Parse.XMLMultipleCountDataEntries", this.cdType);
                }
                this.contentHandler.countData.put(this.cdType, count);
            }
            catch (NumberFormatException e) {
                recordWarning("InvalidCountDataValue", "Message.Parse.XMLValueNotNumber", this.elementName);
            }
        }
    }


    static abstract class AbstractVersionStateHandler extends AbstractStateHandler {
        String version;

        AbstractVersionStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler, String elementName,
                                    Map<String, StateHandlerCreator> qNameToStateHandlers) {
            super(contentHandler, parentStateHandler, elementName, qNameToStateHandlers);
        }

        @Override
        public void handleStateAttributes(Attributes atts) {
            super.handleStateAttributes(atts);
            this.version = atts.getValue("version");
            if (this.version == null) {
                recordWarning("MissingVersion", "Message.Parse.XMLMissingAttribute", this.elementName, "version");
            }

            if (!validateVersion(this.version)) {
                this.version = null;
            }

            this.ignoreChildElements = (this.version == null);
        }

        protected boolean validateVersion(String version) {
            return true;
        }
    }


    static class BookStateHandler extends AbstractVersionStateHandler {
        BookStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName, null);
        }

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "book:id" :
				return new IdEntry.IdStateHandler(this.contentHandler.bookId, this.contentHandler, this, qName);
				
			case "book:slots" :
				return new SlotEntry.SlotsStateHandler(this.contentHandler.bookSlots, this.contentHandler, this, qName);
				
			case "gnc:transaction" :
				return new TransactionImportEntry.TransactionStateHandler(this.contentHandler, this, qName);
				
			case "gnc:account":
                return new AccountImportEntry.AccountStateHandler(this.contentHandler, this, qName);
                
			case "gnc:commodity":
				return new CommodityEntry.CommodityStateHandler(this.contentHandler, this, qName);
				
			case "gnc:pricedb" :
				return new PriceEntry.PriceDBStateHandler(this.contentHandler, this, qName);

			case "gnc:count-data" :
				return new CountDataStateHandler(this.contentHandler, this, qName);
				
			}

			return super.getStateHandlerForElement(qName);
		}
    }


    public interface SimpleDataSetter {
        public void setAttributes(Attributes atts, SimpleDataStateHandler stateHandler);
        public void setData(String characters, SimpleDataStateHandler stateHandler);
    }

    public static abstract class AbstractSimpleDataSetter implements SimpleDataSetter {
        @Override
        public void setAttributes(Attributes atts, SimpleDataStateHandler stateHandler) {

        }
    }

    public static class SimpleDataStateHandler extends AbstractStateHandler {
        final SimpleDataSetter dataSetter;
        SimpleDataStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler, String elementName, SimpleDataSetter dataSetter) {
            super(contentHandler, parentStateHandler, elementName, null);
            this.dataSetter = dataSetter;
        }

        @Override
        public void handleStateAttributes(Attributes atts) {
            super.handleStateAttributes(atts);
            this.dataSetter.setAttributes(atts, this);
        }

        @Override
        protected void endState() {
            super.endState();
            this.dataSetter.setData(this.characters, this);
        }
    }


    public String getErrorMsg() {
        return errorMsg;
    }


    void recordWarning(String id, String key, Object ... arguments) {
        if (!this.recordedWarningMsgIds.contains(id)) {
            String msg = GnuCashConvertUtil.getString(key, arguments);
            this.recordedWarnings.add(msg);
            LOG.warning(msg);
        }
    }

    void recordError(String key, Object ... arguments) {
        this.errorMsg = GnuCashConvertUtil.getString(key, arguments);
    }


    public boolean generateJGnashDatabase() {
        this.errorMsg = null;

        if (!setupCommodities()) {
            return false;
        }

        if (!setupPrices()) {
            return false;
        }

        if (!setupAccounts()) {
            return false;
        }
        
        if (!processTransactions()) {
        	return false;
        }
        
        // TODO scheduled transactions
        return true;
    }


    protected boolean setupCommodities() {
        Integer expectedCount = this.countData.get("commodity");
        if (expectedCount != null) {
            if (expectedCount != this.commodityEntries.size()) {
                recordWarning(null, "Message.Warning.CommodityCountMismatch", expectedCount, this.commodityEntries.size());
            }
        }

        for (Map.Entry<String, CommodityEntry> entry : this.commodityEntries.entrySet()) {
            CommodityEntry commodityEntry = entry.getValue();
            if (!commodityEntry.createJGnashCommodity(this, this.engine)) {
                return false;
            }
        }
        return true;
    }


    protected boolean setupPrices() {
        for (Map.Entry<String, PriceEntry> entry : this.priceEntries.entrySet()) {
            PriceEntry priceEntry = entry.getValue();
            if (!priceEntry.generateJGnashSecurityHistoryNode(this, this.engine)) {
                return false;
            }
        }
        return true;
    }


    protected boolean setupAccounts() {
        Integer expectedCount = this.countData.get("account");
        if (expectedCount != null) {
            if (expectedCount != this.accountImportEntries.size()) {
                recordWarning(null, "Message.Warning.AccountCountMismatch", expectedCount, this.accountImportEntries.size());
            }
        }

        // Find the root account.
        // Build the account tree from there.
        // Create and add the accounts based on the account tree.
        Map<String, AccountImportEntry> workingAccountEntries = new HashMap<>();
        AccountImportEntry rootAccountEntry = null;
        for (Map.Entry<String, AccountImportEntry> entry : this.accountImportEntries.entrySet()) {
            if (entry.getValue().type.equals("ROOT")) {
                if (rootAccountEntry != null) {
                    recordError("Message.Error.MultipleRootAccounts");
                    return false;
                }
                rootAccountEntry = entry.getValue();
            }
            else {
                workingAccountEntries.put(entry.getKey(), entry.getValue());
            }
        }

        if (rootAccountEntry == null) {
            recordError("Message.Error.RootAccountNotFound");
            return false;
        }

        rootAccountEntry.gatherChildAccountEntries(workingAccountEntries);

        if (!workingAccountEntries.isEmpty()) {
            recordError("Message.Error.OrphanAccounts");
            return false;
        }

        // Make sure all the accounts that have been referred to do in fact exist...
        for (String id : this.accountIdsInUse) {
            if (!this.accountImportEntries.containsKey(id)) {
                recordError("Message.Error.MissingAccount");
                return false;
            }
        }

        // OK, ready to create the jGnash accounts!
        if (!rootAccountEntry.createJGnashAccounts(this, this.engine, this.jGnashAccounts, this.accountIdsToIgnore)) {
            return false;
        }

        return true;
    }
    
    
    protected boolean processTransactions() {
        Integer expectedCount = this.countData.get("transaction");
        if (expectedCount != null) {
            if (expectedCount != this.transactionEntries.size()) {
                recordWarning(null, "Message.Warning.TransactionCountMismatch", expectedCount, this.transactionEntries.size());
            }
        }
        
        for (Map.Entry<String, TransactionImportEntry> entry : this.transactionEntries.entrySet()) {
        	TransactionImportEntry transactionEntry = entry.getValue();
        	if (!transactionEntry.generateJGnashTransaction(this, this.engine)) {
        		return false;
        	}
        }

    	return true;
    }

}
