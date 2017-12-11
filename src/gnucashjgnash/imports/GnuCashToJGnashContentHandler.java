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

    final Map<String, CommodityEntry> commodityEntries = new HashMap<>();
    final Map<String, SecurityNode> jGnashSecurities = new HashMap<>();
    final Map<String, CurrencyNode> jGnashCurrencies = new HashMap<>();

    final Map<String, PriceEntry> priceEntries = new HashMap<>();

    final Map<String, AccountImportEntry> accountImportEntries = new HashMap<>();
    final Set<String> accountIdsInUse = new HashSet<>();

    final Set<String> accountIdsToIgnore = new HashSet<>();

    final Map<String, Account> jNashAccounts = new HashMap<>();


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
        final Map<String, StateHandlerCreator> qNameToStateHandlers;
        String characters = "";
        boolean ignoreChildElements = false;

        AbstractStateHandler(final GnuCashToJGnashContentHandler contentHandler,
                             final StateHandler parentStateHandler, final String elementName,
                             final Map<String, StateHandlerCreator> qNameToStateHandlers) {
            this.contentHandler = contentHandler;
            this.parentStateHandler = parentStateHandler;
            this.elementName = elementName;
            this.qNameToStateHandlers = qNameToStateHandlers;
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
            if (this.ignoreChildElements) {
                return null;
            }

            StateHandlerCreator creator = (this.qNameToStateHandlers != null) ? this.qNameToStateHandlers.get(qName) : null;
            if (creator != null) {
                return creator.createStateHandler(this.contentHandler, this, qName);
            }
            return null;
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
            super(contentHandler, null, null, _getOuterStateHandlerQNameToStateHandlers());
        }

    }

    static Map<String, StateHandlerCreator> _OuterStateHandlerQNameToStateHandlers = null;
    static Map<String, StateHandlerCreator> _getOuterStateHandlerQNameToStateHandlers() {
        if (_OuterStateHandlerQNameToStateHandlers == null) {
            _OuterStateHandlerQNameToStateHandlers = new HashMap<>();
            _OuterStateHandlerQNameToStateHandlers.put("gnc-v2", new StateHandlerCreator() {
                @Override
                public StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                                                       String elementName) {
                    return new GNC_V2_StateHandler(contentHandler, parentStateHandler, elementName);
                }
            });
        }
        return _OuterStateHandlerQNameToStateHandlers;
    }


    static class GNC_V2_StateHandler extends AbstractStateHandler {
        GNC_V2_StateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName, _getGNC_V2_StateHandlerQNameToStateHandlers());

        }

        @Override
        protected void endState() {
            super.endState();
            System.out.println("GNC_V2_StateHandler.endState()...");
        }
    }

    static Map<String, StateHandlerCreator> _GNC_V2_StateHandlerQNameToStateHandlers = null;
    static Map<String, StateHandlerCreator> _getGNC_V2_StateHandlerQNameToStateHandlers() {
        if (_GNC_V2_StateHandlerQNameToStateHandlers == null) {
            _GNC_V2_StateHandlerQNameToStateHandlers = new HashMap<>();
            _GNC_V2_StateHandlerQNameToStateHandlers.put("gnc:count-data", new StateHandlerCreator() {
                @Override
                public StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                                                       String elementName) {
                    return new CountDataStateHandler(contentHandler, parentStateHandler, elementName);
                }
            });
            _GNC_V2_StateHandlerQNameToStateHandlers.put("gnc:book", new StateHandlerCreator() {
                @Override
                public StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                                                       String elementName) {
                    return new BookStateHandler(contentHandler, parentStateHandler, elementName);
                }
            });
        }
        return _GNC_V2_StateHandlerQNameToStateHandlers;
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
            super(contentHandler, parentStateHandler, elementName, _getBookStateHandlerQNameToStateHandlers());
        }
    }

    static Map<String, StateHandlerCreator> _BookStateHandlerQNameToStateHandlers = null;
    static Map<String, StateHandlerCreator> _getBookStateHandlerQNameToStateHandlers() {
        if (_BookStateHandlerQNameToStateHandlers == null) {
            _BookStateHandlerQNameToStateHandlers = new HashMap<>();
            _BookStateHandlerQNameToStateHandlers.put("gnc:count-data", new StateHandlerCreator() {
                @Override
                public StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                                                       String elementName) {
                    return new CountDataStateHandler(contentHandler, parentStateHandler, elementName);
                }
            });
            _BookStateHandlerQNameToStateHandlers.put("gnc:account", new StateHandlerCreator() {
                @Override
                public StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                                                       String elementName) {
                    return new AccountImportEntry.AccountStateHandler(contentHandler, parentStateHandler, elementName);
                }
            });
            _BookStateHandlerQNameToStateHandlers.put("gnc:commodity", new StateHandlerCreator() {
                @Override
                public StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                                                       String elementName) {
                    return new CommodityEntry.CommodityStateHandler(contentHandler, parentStateHandler, elementName);
                }
            });
            _BookStateHandlerQNameToStateHandlers.put("gnc:pricedb", new StateHandlerCreator() {
                @Override
                public StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                                                       String elementName) {
                    return new PriceEntry.PriceDBStateHandler(contentHandler, parentStateHandler, elementName);
                }
            });
        }
        return _BookStateHandlerQNameToStateHandlers;
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
        SimpleDataStateHandler(SimpleDataSetter dataSetter, GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler, String elementName) {
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

    public static void addSimpleDataStateHandler(Map<String, StateHandlerCreator> stateHandlers, String key, SimpleDataSetter dataSetter) {
        stateHandlers.put(key, new StateHandlerCreator() {

            @Override
            public StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler, String elementName) {
                return new SimpleDataStateHandler(dataSetter, contentHandler, parentStateHandler, elementName);
            }
        });
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
        return true;
    }


    protected boolean setupCommodities() {
        Integer expectedCommoditiesCount = this.countData.get("commodity");
        if (expectedCommoditiesCount != null) {
            if (expectedCommoditiesCount != this.commodityEntries.size()) {
                recordWarning(null, "Message.Warning.CommodityCountMismatch", expectedCommoditiesCount, this.commodityEntries.size());
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
        Integer expectedAccountCount = this.countData.get("account");
        if (expectedAccountCount != null) {
            if (expectedAccountCount != this.accountImportEntries.size()) {
                recordWarning(null, "Message.Warning.AccountCountMismatch", expectedAccountCount, this.accountImportEntries.size());
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
        if (!rootAccountEntry.createJGnashAccounts(this, this.engine, this.jNashAccounts, this.accountIdsToIgnore)) {
            return false;
        }

        return true;
    }

}
