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
import gnucashjgnash.NoticeTree;
import gnucashjgnash.NoticeTree.Source;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;


/**
 * This is the main worker class. It implements the SAX content handler for parsing the GnuCash XML file,
 * as well as managing the conversion of the parsed contents to jGnash format.
 * <p>
 * The XML parsing is handled via {@link GnuCashToJGnashContentHandler.StateHandler} objects. These operating on the individual XML elements.
 * A stack of {@link GnuCashToJGnashContentHandler.StateHandler} objects is maintained as the XML tree is descended.
 * <p>
 * 
 * @author albert
 *
 */
public class GnuCashToJGnashContentHandler implements ContentHandler {
    private static final Logger LOG = Logger.getLogger(GnuCashToJGnashContentHandler.class.getName());

    final Engine engine;
    final GnuCashImport.StatusCallback statusCallback;
    int statusProgressCount;
    int statusProgressTotalCount;
    
    Locator documentLocator;
    
    final List<StateHandler> stateHandlers = new ArrayList<>();
    StateHandler activeStateHandler;

    final Map<String, Integer> countData = new HashMap<>();
    
    final IdEntry bookId = new IdEntry(null);
    final Map<String, SlotEntry> bookSlots = new HashMap<>();

    final Map<String, CommodityEntry> commodityEntries = new HashMap<>();
    final Map<String, SecurityNode> jGnashSecurities = new HashMap<>();
    final Map<String, CurrencyNode> jGnashCurrencies = new HashMap<>();

    final Map<String, PriceEntry> priceEntries = new HashMap<>();
    final Map<String, SortedMap<LocalDate, PriceEntry>> sortedPriceEntries = new HashMap<>();
    
    final Map<String, NoticeTree.Source> priceCommoditySources = new HashMap<>();
    final NoticeTree.Source priceCommoditySourcesRoot = new NoticeTree.Source() {
		
		@Override
		public String getSourceTitle() {
			return GnuCashConvertUtil.getString("Message.Notice.CommodityPricesRoot");
		}
		
		@Override
		public String getSourceDescription() {
			return null;
		}
		
		@Override
		public Source getParentSource() {
			return null;
		}
	};
	
	final NoticeTree.Source priceCommodityOrphanEntry = new NoticeTree.Source() {
		
		@Override
		public String getSourceTitle() {
			return GnuCashConvertUtil.getString("Message.Notice.OrphanCommodityPrices");
		}
		
		@Override
		public String getSourceDescription() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public Source getParentSource() {
			return priceCommoditySourcesRoot;
		}
	};

	
    final Map<String, NoticeTree.Source> priceCurrencySources = new HashMap<>();
    final NoticeTree.Source priceCurrencySourcesRoot = new NoticeTree.Source() {
		
		@Override
		public String getSourceTitle() {
			return GnuCashConvertUtil.getString("Message.Notice.CurrencyPricesRoot");
		}
		
		@Override
		public String getSourceDescription() {
			return null;
		}
		
		@Override
		public Source getParentSource() {
			return null;
		}
	};

    
	
	final Map<String, AccountImportEntry> accountImportEntries = new HashMap<>();

    final Set<String> accountIdsToIgnore = new HashSet<>();

    final Map<String, SecurityNode> jGnashSecuritiesByStockAccountId = new HashMap<>();
    
    final Map<String, Account> jGnashAccounts = new HashMap<>();

    //final Map<String, TransactionImportEntry> transactionEntries = new HashMap<>();
    final SortedMap<LocalDate, Map<String, TransactionImportEntry>> transactionEntriesByDate = new TreeMap<>();
    int totalTransactionEntryCount;

    final Map<String, NoticeTree.Source> transactionAccountSources = new HashMap<>();
    final NoticeTree.Source transactionAccountSourcesRoot = new NoticeTree.Source() {

		@Override
		public Source getParentSource() {
			return null;
		}

		@Override
		public String getSourceTitle() {
			return GnuCashConvertUtil.getString("Message.Notice.TransactionsRoot");
		}

		@Override
		public String getSourceDescription() {
			return null;
		}
    };
    final NoticeTree.Source orphanTransactionAccountSources = new NoticeTree.Source() {
		
		@Override
		public String getSourceTitle() {
			return GnuCashConvertUtil.getString("Message.Notice.OrphanTransactions");
		}
		
		@Override
		public String getSourceDescription() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public Source getParentSource() {
			// TODO Auto-generated method stub
			return null;
		}
	};
    
    
    final Map<String, AccountImportEntry> templateAccountImportEntries = new HashMap<>();
    final Map<String, TransactionImportEntry> templateTransactionImportEntries = new HashMap<>();
    final Map<String, ScheduledTransactionEntry> scheduledTransactionEntries = new HashMap<>();
    int totalScheduledTransactionEntryCount;

    final NoticeTree warningNoticeTree = new NoticeTree();
    
    final Set<String> recordedWarningMsgIds = new HashSet<>();
    final List<String> recordedWarnings = new ArrayList<>();
    String errorMsg;
    
    TransactionMode transactionMode = TransactionMode.NORMAL;
    
    
    static enum TransactionMode {
    	NORMAL(""),
    	TEMPLATE("Template");
    	
    	final String idSuffix;
    	TransactionMode(String idSuffix) {
    		this.idSuffix = idSuffix;
    	}
    }


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
    	this.documentLocator = locator;
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
    	ParsedEntry getParsedEntry();
    	
        String getElementName();
        void handleStateAttributes(Attributes atts);

        void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException;
        void endElement(String uri, String localName, String qName) throws SAXException;

        void characters(char[] ch, int start, int length) throws SAXException;

        void stateHandlerReactivated();

        void recordWarningOld(String msgId, String key, Object ... arguments);
        void recordWarning(String key, Object ...arguments);
        
        GnuCashToJGnashContentHandler getContentHandler();
    }




    abstract static class AbstractStateHandler implements StateHandler {
        final GnuCashToJGnashContentHandler contentHandler;
        final StateHandler parentStateHandler;
        final String elementName;
        String characters = "";
        boolean ignoreChildElements = false;

        AbstractStateHandler(final GnuCashToJGnashContentHandler contentHandler,
                             final StateHandler parentStateHandler, final String elementName) {
            this.contentHandler = contentHandler;
            this.parentStateHandler = parentStateHandler;
            this.elementName = elementName;
        }
        
        @Override
        public String getElementName() { 
            return elementName; 
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
        public void recordWarningOld(String msgId, String key, Object ... arguments) {
            this.contentHandler.recordWarningOld(msgId, key, arguments);
        }

        @Override
        public void recordWarning(String key, Object ... arguments) {
        	ParsedEntry parsedEntry = this.getParsedEntry();
        	if (parsedEntry == null) {
        		for (int index = this.contentHandler.stateHandlers.size() - 1; index >= 0; --index) {
        			StateHandler stateHandler = this.contentHandler.stateHandlers.get(index);
        			parsedEntry = stateHandler.getParsedEntry();
        			if (parsedEntry != null) {
        				break;
        			}
        		}
        	}
        	
            this.contentHandler.recordWarning(parsedEntry, key, arguments);
        }

        @Override
        public GnuCashToJGnashContentHandler getContentHandler() {
        	return this.contentHandler;
        }
    }



    /**
     * The default state handler, this prints the element name to the console.
     * @author albert
     *
     */
    static class NOP_StateHandler extends AbstractStateHandler {
        NOP_StateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                         String elementName) {
            super(contentHandler, parentStateHandler, elementName);
        }
        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return null;
        }

        @Override
        protected void endState() {
            super.endState();
            String namePath = "";
            String separator = "";
            for (StateHandler stateHandler : this.contentHandler.stateHandlers) {
                namePath += separator + stateHandler.getElementName();
                separator = ">";
            }
            System.out.println("NOP_StateHandler.endState()..." + namePath);
        }

    }

    
    /**
     * State handler for skipping an element and all its children. The element name is printed
     * to the console, all the child elements are ignored.
     * @author albert
     *
     */
    static class SkipStateHandler extends AbstractStateHandler {

        SkipStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                String elementName) {
            super(contentHandler, parentStateHandler, elementName);
        }
        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return null;
        }

        /* (non-Javadoc)
         * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        }

        /* (non-Javadoc)
         * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#endState()
         */
        @Override
        protected void endState() {
            String namePath = "";
            String separator = "";
            for (StateHandler stateHandler : this.contentHandler.stateHandlers) {
                namePath += separator + stateHandler.getElementName();
                separator = ">";
            }
            System.out.println("SkipStateHandler.endState()..." + namePath);
        }
        
        
    }


    /**
     * The outer-most state handler, what we start with.
     * @author albert
     *
     */
    static class OuterStateHandler extends AbstractStateHandler {

        OuterStateHandler(GnuCashToJGnashContentHandler contentHandler) {
            super(contentHandler, null, null);
        }
        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return null;
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
            super(contentHandler, parentStateHandler, elementName);

        }
        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return null;
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
            super(contentHandler, parentStateHandler, elementName);
        }
        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	// TODO Add a parsed entry for the different counts
        	return null;
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
                recordWarning("Message.Parse.XMLMissingAttribute", this.elementName, "cd:type");
            }
            try {
                int count = Integer.parseInt(this.characters);
                if (this.contentHandler.countData.get(this.cdType) != null) {
                    recordWarning("Message.Parse.XMLMultipleCountDataEntries", this.cdType);
                }
                this.contentHandler.countData.put(this.cdType, count);
            }
            catch (NumberFormatException e) {
                recordWarning("Message.Parse.XMLValueNotNumber", this.elementName);
            }
        }
    }


    /**
     * State handler for elements that have a version attribute to be validated.
     * @author albert
     *
     */
    static abstract class AbstractVersionStateHandler extends AbstractStateHandler {
        String version;

        AbstractVersionStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName);
        }

        @Override
        public void handleStateAttributes(Attributes atts) {
            super.handleStateAttributes(atts);
            this.version = atts.getValue("version");
            if (this.version == null) {
                recordWarning("Message.Parse.XMLMissingAttribute", this.elementName, "version");
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
            super(contentHandler, parentStateHandler, elementName);
        }
        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return null;
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
                return new SlotEntry.SlotsStateHandler(this.contentHandler.bookSlots, null, this.contentHandler, this, qName);
                
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
                
            case "gnc:schedxaction":
                return new ScheduledTransactionEntry.ScheduledTransactionStateHandler(this.contentHandler, this, qName);
                
            case "gnc:template-transactions":
                return new TemplateTransactionsStateHandler(this.contentHandler, this, qName);

            case "gnc:budget":
                return new SkipStateHandler(this.contentHandler, this, qName);
                
            }

            return super.getStateHandlerForElement(qName);
        }
    }
    
    /**
     * State handler for the template transactions.
     * @author albert
     *
     */
    static class TemplateTransactionsStateHandler extends AbstractStateHandler {

		TemplateTransactionsStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
				String elementName) {
			super(contentHandler, parentStateHandler, elementName);
		}

        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return null;
        }

        /* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "gnc:account":
                return new AccountImportEntry.AccountStateHandler(this.contentHandler, this, qName);

			case "gnc:transaction":
                return new TransactionImportEntry.TransactionStateHandler(this.contentHandler, this, qName);
			}
			
			return super.getStateHandlerForElement(qName);
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			this.contentHandler.transactionMode = TransactionMode.TEMPLATE;
			super.startElement(uri, localName, qName, atts);
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			this.contentHandler.transactionMode = TransactionMode.NORMAL;
		}
    	
    }


    /**
     * Interface called by {@link SimpleDataStateHandler} to have the data value set.
     */
    public interface SimpleDataSetter {
        public void setAttributes(Attributes atts, SimpleDataStateHandler stateHandler);
        public void setData(String characters, SimpleDataStateHandler stateHandler);
    }

    public static abstract class AbstractSimpleDataSetter implements SimpleDataSetter {
        @Override
        public void setAttributes(Attributes atts, SimpleDataStateHandler stateHandler) {

        }
    }

    /**
     * A {@link GnuCashToJGnashContentHandler.StateHandler} that passes information to a {@link SimpleDataSetter}. This is used for simple
     * elements that just need to record the value of the element somewhere.
     * @author albert
     *
     */
    public static class SimpleDataStateHandler extends AbstractStateHandler {
        final SimpleDataSetter dataSetter;
        SimpleDataStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler, String elementName, SimpleDataSetter dataSetter) {
            super(contentHandler, parentStateHandler, elementName);
            this.dataSetter = dataSetter;
        }

        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return null;
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

    void recordWarning(NoticeTree.Source source, String key, Object ...arguments) {
        String msg = GnuCashConvertUtil.getString(key, arguments);
        this.warningNoticeTree.addNotice(source, msg, null);
    }

    void recordWarningOld(String id, String key, Object ... arguments) {
        if (!this.recordedWarningMsgIds.contains(id)) {
            String msg = GnuCashConvertUtil.getString(key, arguments);
            this.recordedWarnings.add(msg);
            LOG.warning(msg);
        }
    }

    void recordError(String key, Object ... arguments) {
        this.errorMsg = GnuCashConvertUtil.getString(key, arguments);
    }
    
    boolean addPriceEntry(PriceEntry priceEntry) {
        String securityId = priceEntry.commodityRef.id;
        SortedMap<LocalDate, PriceEntry> priceEntriesForSecurity = this.sortedPriceEntries.get(securityId);
        if (priceEntriesForSecurity == null) {
            priceEntriesForSecurity = new TreeMap<LocalDate, PriceEntry>();
            this.sortedPriceEntries.put(securityId, priceEntriesForSecurity);
        }
        
        if (priceEntriesForSecurity.put(priceEntry.time.localDate, priceEntry) != null) {
            recordWarning(priceEntry.getParentSource(), "Message.Parse.XMLDuplicatePriceEntry", securityId, priceEntry.time.toDateString());
        }
        
        return true;
    }
    
    
    class PriceCommoditySource implements NoticeTree.Source {
    	final String commodityId;
    	CommodityEntry commodity;
    	
    	PriceCommoditySource(String commodityId) {
    		this.commodityId = commodityId;
    	}
    	
    	void updateCommodity() {
    		if (this.commodity == null) {
    			this.commodity = GnuCashToJGnashContentHandler.this.commodityEntries.get(this.commodityId);
    		}
    	}

		/* (non-Javadoc)
		 * @see gnucashjgnash.NoticeTree.Source#getParentSource()
		 */
		@Override
		public Source getParentSource() {
			return priceCommoditySourcesRoot;
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.NoticeTree.Source#getSourceTitle()
		 */
		@Override
		public String getSourceTitle() {
			updateCommodity();
			String title = null;
			if (this.commodity != null) {
				title = this.commodity.name;
				if ((title == null) || title.isEmpty()) {
					title = this.commodity.id;
				}
			}
			return title;
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.NoticeTree.Source#getSourceDescription()
		 */
		@Override
		public String getSourceDescription() {
			return null;
		}
    	
    }
    
    NoticeTree.Source getPriceParentSource(PriceEntry priceEntry) {
    	NoticeTree.Source source = null;

    	if (priceEntry.commodityRef.isParsed()) {
    		String securityId = priceEntry.commodityRef.id;
        	source = this.priceCommoditySources.get(securityId);
	        if (source == null) {
	        	source = new PriceCommoditySource(securityId);
	        	this.priceCommoditySources.put(securityId, source);
	        }
    	}
    	else if (priceEntry.currencyRef.isParsed()) {
    		String currencyId = priceEntry.currencyRef.id;
        	source = this.priceCurrencySources.get(currencyId);
	        if (source == null) {
	        	source = new PriceCommoditySource(currencyId);
	        	this.priceCurrencySources.put(currencyId, source);
	        }
    	}
    	else {
    		return priceCommodityOrphanEntry;
    	}
    	return source;
    }

    
    boolean addAccountEntry(AccountImportEntry entry) {
    	switch (this.transactionMode) {
    	case NORMAL :
            if (this.accountImportEntries.put(entry.id.id, entry) != null) {
                recordWarning(entry, "Message.Parse.XMLMultipleAccountEntries", entry.name, entry.id);
            }
    		break;
    		
    	case TEMPLATE :
            if (this.templateAccountImportEntries.put(entry.id.id, entry) != null) {
                recordWarning(entry, "Message.Parse.XMLMultipleTemplateAccountEntries", entry.name, entry.id);
            }
    		break;
    	}
    	return true;
    }
    
    
    boolean addTransactionEntry(TransactionImportEntry entry) {
    	switch (this.transactionMode) {
    	case NORMAL :
            Map<String, TransactionImportEntry> dateEntries = this.transactionEntriesByDate.get(entry.datePosted.localDate);
            if (dateEntries == null) {
                dateEntries = new HashMap<>();
                this.transactionEntriesByDate.put(entry.datePosted.localDate, dateEntries);
            }
            if (dateEntries.put(entry.id.id, entry) != null) {
                recordWarning(entry, "Message.Parse.XMLDuplicateTransaction", entry.id, entry.datePosted.toDateString());
            }
            else {
                ++this.totalTransactionEntryCount;
            }
            break;
            
    	case TEMPLATE :
    		if (this.templateTransactionImportEntries.put(entry.id.id, entry) != null) {
                recordWarning(entry, "Message.Parse.XMLDuplicateTemplateTransaction", entry.id, entry.datePosted.toDateString());
    		}
    		break;
    	}
        
        return true;
    }
    
    
    class TransactionAccountSource implements NoticeTree.Source {
    	final String accountId;
    	AccountImportEntry accountImportEntry;
    	
		TransactionAccountSource(String accountId) {
			this.accountId = accountId;
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.NoticeTree.Source#getParentSource()
		 */
		@Override
		public Source getParentSource() {
			return transactionAccountSourcesRoot;
		}
		
		void updateAccountImportEntry() {
			if (this.accountImportEntry != null) {
				this.accountImportEntry = GnuCashToJGnashContentHandler.this.accountImportEntries.get(this.accountId);
			}
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.NoticeTree.Source#getSourceTitle()
		 */
		@Override
		public String getSourceTitle() {
			updateAccountImportEntry();
			String title = null;
			if (this.accountImportEntry != null) {
				title = this.accountImportEntry.name;
				if ((title == null) || title.isEmpty()) {
					title = this.accountImportEntry.description;
				}
				if ((title == null) || title.isEmpty()) {
					title = this.accountImportEntry.id.id;
				}
			}
			if (title == null) {
				title = GnuCashConvertUtil.getString("Message.Notice.OrphanTransactions");
			}
			
			return title;
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.NoticeTree.Source#getSourceDescription()
		 */
		@Override
		public String getSourceDescription() {
			return null;
		}
    	
    }
    
    NoticeTree.Source getTransactionParentSource(TransactionImportEntry entry) {
    	NoticeTree.Source source = null;

    	String accountId = entry.getAccountId();
    	if (accountId != null) {
    		source = this.transactionAccountSources.get(accountId);
    		if (source == null ) {
    			source = new TransactionAccountSource(accountId);
    			this.transactionAccountSources.put(accountId, source);
    		}
    	}

    	if (source == null) {
    		return orphanTransactionAccountSources;
    	}
    	return source;
    }
    
    
    boolean addScheduledTransactionEntry(ScheduledTransactionEntry entry) {
    	if (this.scheduledTransactionEntries.put(entry.id.id, entry) != null) {
    		recordWarning(entry, "Message.Parse.XMLDuplicateScheduledTransaction", entry.id, entry.name);
    	}
    	++this.totalScheduledTransactionEntryCount;
    	return true;
    }
    
    
    /**
     * Calls the installed StatusCallback, if any, with an updated progress/status.
     * @param toAdd	The amount to increase the current progress by.
     * @param statusMsg	If not null, the status message to set.
     */
    void updateStatusCallback(long toAdd, String statusMsg) {
        if (this.statusCallback != null) {
            this.statusProgressCount += toAdd;
            this.statusCallback.updateStatus(this.statusProgressCount,  this.statusProgressTotalCount, statusMsg);
        }
    }


    /**
     * The main entry point for generating the jGnash database after the GnuCash file has been parsed.
     * @return	<code>false</code> if failed.
     */
    public boolean generateJGnashDatabase() {
        this.errorMsg = null;
        
        this.statusProgressCount = 0;
        this.statusProgressTotalCount = 0;
        this.statusProgressTotalCount += this.commodityEntries.size();
        this.statusProgressTotalCount += this.sortedPriceEntries.size();
        this.statusProgressTotalCount += this.accountImportEntries.size();
        this.statusProgressTotalCount += this.totalTransactionEntryCount;

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
        
        if (!processScheduledTransactions()) {
        	return false;
        }

        return true;
    }


    protected boolean setupCommodities() {
        updateStatusCallback(0, GnuCashConvertUtil.getString("Message.Status.ImportingCommodities", this.commodityEntries.size()));
        
        Integer expectedCount = this.countData.get("commodity");
        if (expectedCount != null) {
            if (expectedCount != this.commodityEntries.size()) {
                recordWarning((ParsedEntry)null, "Message.Warning.CommodityCountMismatch", expectedCount, this.commodityEntries.size());
            }
        }

        for (Map.Entry<String, CommodityEntry> entry : this.commodityEntries.entrySet()) {
            CommodityEntry commodityEntry = entry.getValue();
            if (!commodityEntry.createJGnashCommodity(this, this.engine)) {
                return false;
            }
            updateStatusCallback(1, null);
        }
        return true;
    }


    protected boolean setupPrices() {
        
        for (Map.Entry<String, SortedMap<LocalDate, PriceEntry>> entry : this.sortedPriceEntries.entrySet()) {
            updateStatusCallback(0, GnuCashConvertUtil.getString("Message.Status.ImportingCommodityPrices", entry.getKey()));

            SecurityNode securityNode = this.jGnashSecurities.get(entry.getKey());
            if (securityNode != null) {
                if (!setupPricesForAccount(securityNode, entry.getValue())) {
                    return false;
                }
            }
            
            updateStatusCallback(1, null);
        }

        return true;
    }
    

    protected boolean setupPricesForAccount(SecurityNode securityNode, SortedMap<LocalDate, PriceEntry> priceEntriesByDate) {
        if (priceEntriesByDate.isEmpty()) {
            return true;
        }
        
        int count = 0;
        
        LocalDate keys [] = priceEntriesByDate.keySet().toArray(new LocalDate [priceEntriesByDate.size()]);
        
        // Keep everything newer than 30 days.
        // After that, keep the oldest in each month.
        LocalDate monthAgo = LocalDate.now().minusMonths(1);
        int index = keys.length - 1;
        for (; index >= 0; --index) {
            LocalDate date = keys[index];
            if (date.isBefore(monthAgo)) {
                break;
            }

            PriceEntry priceEntry = priceEntriesByDate.get(date);
            if (!priceEntry.generateJGnashSecurityHistoryNode(this, this.engine, securityNode)) {
                return false;
            }
            
            ++count;
        }
        
        // Get the last day of the previous month...
        monthAgo = LocalDate.of(monthAgo.getYear(), monthAgo.getMonth(), 1).minusDays(1);
        
        for (; index >= 0; --index) {
            LocalDate date = keys[index];
            if (date.isAfter(monthAgo)) {
                continue;
            }
            monthAgo = LocalDate.of(monthAgo.getYear(), monthAgo.getMonth(), 1).minusDays(1);

            PriceEntry priceEntry = priceEntriesByDate.get(date);
            if (!priceEntry.generateJGnashSecurityHistoryNode(this, this.engine, securityNode)) {
                return false;
            }
            
            ++count;
        }
        
        LOG.info("Added " + count + " prices for security " + securityNode.getSymbol());
        return true;
    }


    protected boolean setupAccounts() {
        updateStatusCallback(0, GnuCashConvertUtil.getString("Message.Status.SettingUpAccounts", this.accountImportEntries.size()));
        
        Integer expectedCount = this.countData.get("account");
        if (expectedCount != null) {
            if (expectedCount != this.accountImportEntries.size()) {
                recordWarning((ParsedEntry)null, "Message.Warning.AccountCountMismatch", expectedCount, this.accountImportEntries.size());
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
                    recordError("Message.Error.MultipleRootAccounts" + this.transactionMode.idSuffix);
                    return false;
                }
                rootAccountEntry = entry.getValue();
            }
            else {
                workingAccountEntries.put(entry.getKey(), entry.getValue());
            }
        }

        if (rootAccountEntry == null) {
            recordError("Message.Error.RootAccountNotFound" + this.transactionMode.idSuffix);
            return false;
        }

        rootAccountEntry.gatherChildAccountEntries(workingAccountEntries);

        if (!workingAccountEntries.isEmpty()) {
            recordError("Message.Error.OrphanAccounts" + this.transactionMode.idSuffix);
            return false;
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
            if (expectedCount != this.totalTransactionEntryCount) {
                recordWarning((ParsedEntry)null, "Message.Warning.TransactionCountMismatch", expectedCount, this.totalTransactionEntryCount);
            }
        }
        
        int count = 0;
        for (Map.Entry<LocalDate, Map<String, TransactionImportEntry>> dateEntry : this.transactionEntriesByDate.entrySet()) {
            updateStatusCallback(0, GnuCashConvertUtil.getString("Message.Status.ProcessingTransactions", this.totalTransactionEntryCount, dateEntry.getKey().format(DateTimeFormatter.ISO_DATE)));
            
            Map<String, TransactionImportEntry> entriesForDate = dateEntry.getValue();
            for (Map.Entry<String, TransactionImportEntry> entry : entriesForDate.entrySet()) {
                TransactionImportEntry transactionEntry = entry.getValue();
                boolean result = transactionEntry.generateJGnashTransaction(this, this.engine);
                updateStatusCallback(1, null);
                if (!result) {
                    //return false;
                    continue;
                }
                ++count;
            }
        }
        
        LOG.info("Processed " + count + " transactions.");
    
        return true;
    }

    
    
    protected boolean processScheduledTransactions() {
        Integer expectedCount = this.countData.get("schedxaction");
        if (expectedCount != null) {
            if (expectedCount != this.totalScheduledTransactionEntryCount) {
                recordWarning((ParsedEntry)null, "Message.Warning.ScheduledTransactionCountMismatch", expectedCount, this.totalScheduledTransactionEntryCount);
            }
        }
        
        // Process the template transactions.
        if (!ScheduledTransactionEntry.processTemplateTransactions(this, this.engine)) {
        	return false;
        }
        
        updateStatusCallback(0, GnuCashConvertUtil.getString("Message.Status.ProcessingScheduledTransactions", this.totalScheduledTransactionEntryCount));

        int count = 0;
        for (Map.Entry<String, ScheduledTransactionEntry> entry : this.scheduledTransactionEntries.entrySet()) {
        	ScheduledTransactionEntry scheduledTransactionEntry = entry.getValue();
        	if (!scheduledTransactionEntry.generateJGnashScheduledTransaction(this, this.engine)) {
        		return false;
        	}
        }
        
        LOG.info("Processed " + count + " scheduled transactions.");
    	
    	return true;
    }
    
    
}
