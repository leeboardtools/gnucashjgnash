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

import jgnash.engine.*;

import java.util.HashMap;
import java.util.Map;

import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;


/**
 * Represents a parsed GnuCash Commodity from 
 * <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>.
 * @author albert
 *
 */
public class CommodityEntry {
    String space;
    String id;
    String name;
    String xCode;
    String fraction;

    boolean isGetQuotes = false;
    String quoteSource;
    String quoteTimeZone;

    boolean isCurrency = false;

    public static final String CURRENCY_SPACE = "ISO4217";

    Map<String, SlotEntry> slots = new HashMap<>();


    static class CommodityStateHandler extends GnuCashToJGnashContentHandler.AbstractVersionStateHandler {
        final CommodityEntry commodityEntry = new CommodityEntry();

        CommodityStateHandler(GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler, 
                              String elementName) {
            super(contentHandler, parentStateHandler, elementName);
        }

        @Override
        protected boolean validateVersion(String version) {
            if (!version.equals("2.0.0")) {
                this.contentHandler.recordError("Message.Parse.XMLCommodityVersionUnsupported", version);
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
         */
        @Override
        protected StateHandler getStateHandlerForElement(String qName) {
            switch (qName) {
            case "cmdty:space" :
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                            commodityEntry.space = value;
                        }
                    });

            case "cmdty:id" :
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                            commodityEntry.id = value;
                        }
                    });

            case "cmdty:name" :
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                            commodityEntry.name = value;
                        }
                    });
    
            case "cmdty:xcode" :
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                            commodityEntry.xCode = value;
                        }
                    });

            case "cmdty:fraction" : 
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                            commodityEntry.fraction = value;
                        }
                    });

            case "cmdty:get_quotes" : 
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                            commodityEntry.isGetQuotes = true;
                        }
                    });

            case "cmdty:quote_source" :
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                            commodityEntry.quoteSource = value;
                        }
                    });

            case "cmdty:quote_tz" :
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                            commodityEntry.quoteTimeZone = value;
                        }
                    });

            case "cmdty:slots" : 
                return new SlotEntry.SlotsStateHandler(this.commodityEntry.slots, this.contentHandler, this, qName);

            }
            return super.getStateHandlerForElement(qName);
        }

        @Override
        protected void endState() {
            super.endState();

            if ("template".equals(this.commodityEntry.space) && "template".equals(this.commodityEntry.id)) {
                return;
            }

            if (this.commodityEntry.space == null) {
                recordWarning("CommodityMissingSpace", "Message.Parse.XMLCommodityMissingSpace", this.commodityEntry.id, "cmdty:space");
                return;
            }

            if (this.commodityEntry.id == null) {
                recordWarning("CommodityMissingId", "Message.Parse.XMLCommodityMissingId", this.commodityEntry.space, "cmdty:id");
                return;
            }

            this.commodityEntry.isCurrency = this.commodityEntry.space.equals(CURRENCY_SPACE);

            if (!this.commodityEntry.isCurrency) {
                if (this.commodityEntry.fraction == null) {
                    recordWarning("CommodityMissingFraction", "Message.Parse.XMLCommodityMissingFraction", this.commodityEntry.id, "cmdty:fraction");
                    return;
                }
            }

            if (this.contentHandler.commodityEntries.put(this.commodityEntry.id, this.commodityEntry) != null) {
                recordWarning("DuplicateCommodityId", "Message.Parse.XMLDuplicateCommodityId", this.commodityEntry.id);
            }
        }
    }


    static abstract class SimpleDataSetterImpl extends GnuCashToJGnashContentHandler.AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            CommodityStateHandler parentStateHandler = (CommodityStateHandler)stateHandler.parentStateHandler;
            setCommodityEntryField(parentStateHandler.commodityEntry, characters);
        }

        protected abstract void setCommodityEntryField(CommodityEntry commodityEntry, String value);
    }


    
    /**
     * Represents a parsed commodity reference (act:commodity, price:currency, price:commodity, trn:currency, etc.)
     * @author albert
     *
     */
    public static class CommodityRef {
        String space;
        String id;

        boolean validateRef(GnuCashToJGnashContentHandler.StateHandler stateHandler, String qName) {
            if (this.space == null) {
                stateHandler.recordWarning("CommodityRefMissingSpace_" + qName, "Message.Parse.XMLCommodityRefMissingElement", qName, "cmdty:space");
                return false;
            }
            if (this.id == null) {
                stateHandler.recordWarning("CommodityRefMissingId_" + qName, "Message.Parse.XMLCommodityRefMissingElement", qName, "cmdty:id");
                return false;
            }
            return true;
        }
    }

    public static class CurrencyRef extends CommodityRef {

    }


    public static class CommodityRefStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final CommodityRef commodityRef;
        CommodityRefStateHandler(final CommodityRef commodityRef, GnuCashToJGnashContentHandler contentHandler,
                                 GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName);
            this.commodityRef = commodityRef;
        }
        
        /* (non-Javadoc)
         * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
         */
        @Override
        protected StateHandler getStateHandlerForElement(String qName) {
            switch (qName) {
            case "cmdty:space" : 
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new RefSimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityRefField(CommodityRef commodityRef, String value) {
                            commodityRef.space = value;
                        }
                    });

            case "cmdty:id" : 
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new RefSimpleDataSetterImpl() {
                        @Override
                        protected void setCommodityRefField(CommodityRef commodityRef, String value) {
                            commodityRef.id = value;
                        }
                    });
                
            }
            return super.getStateHandlerForElement(qName);
        }
    }

    public static class CurrencyRefStateHandler extends CommodityRefStateHandler {

        CurrencyRefStateHandler(CurrencyRef currencyRef, GnuCashToJGnashContentHandler contentHandler,
                                GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName) {
            super(currencyRef, contentHandler, parentStateHandler, elementName);
        }
    }

    static abstract class RefSimpleDataSetterImpl extends GnuCashToJGnashContentHandler.AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            CommodityRefStateHandler parentStateHandler = (CommodityRefStateHandler)stateHandler.parentStateHandler;
            setCommodityRefField(parentStateHandler.commodityRef, characters);
        }

        protected abstract void setCommodityRefField(CommodityRef commodityRef, String value);
    }

    
    /**
     * Main method for creating/adding the jGnash equivalent commodity (currency or security).
     * @param contentHandler
     * @param engine
     * @return	<code>false</code> if failed.
     */
    public boolean createJGnashCommodity(GnuCashToJGnashContentHandler contentHandler, Engine engine) {
        if (this.isCurrency) {
            return createJGnashCurrency(contentHandler, engine);
        }
        else {
            return createJGnashSecurity(contentHandler, engine);
        }
    }

    protected boolean createJGnashCurrency(GnuCashToJGnashContentHandler contentHandler, Engine engine) {
        CurrencyNode node = new CurrencyNode();
        if (!setupCommodityNode(node, contentHandler, engine)) {
            return false;
        }

        if (!this.id.equals(engine.getDefaultCurrency().getSymbol())) {
            engine.addCurrency(node);
        }

        contentHandler.jGnashCurrencies.put(this.id, node);
        return true;
    }

    protected boolean createJGnashSecurity(GnuCashToJGnashContentHandler contentHandler, Engine engine) {
        SecurityNode node = new SecurityNode();
        if (!setupCommodityNode(node, contentHandler, engine)) {
            return false;
        }

        if (this.quoteSource != null) {
            switch (this.quoteSource) {
                case "yahoo" :
                    node.setQuoteSource(QuoteSource.YAHOO);
                    break;
                case "yahoo_australia" :
                    node.setQuoteSource(QuoteSource.YAHOO_AUS);
                    break;
                case "yahoo_europe" :
                    node.setQuoteSource(QuoteSource.YAHOO_UK);
                    break;

                default :
                    contentHandler.recordWarning("QuoteSourceNotSupported_" + this.quoteSource, "Message.Warning.QuoteSourceNotSupported", this.quoteSource);
            }
        }

        if (this.xCode != null) {
            node.setISIN(this.xCode);
        }
        else {
            node.setISIN("");
        }

        node.setReportedCurrencyNode(engine.getCurrency("USD"));

        if (!engine.addSecurity(node)) {
            return false;
        }
        contentHandler.jGnashSecurities.put(this.id, node);
        return true;
    }

    protected boolean setupCommodityNode(CommodityNode node, GnuCashToJGnashContentHandler contentHandler, Engine engine) {
        if (this.space != null) {

        }

        if (this.id != null) {
            node.setSymbol(this.id);
        }

        if (this.name != null) {
            node.setDescription(this.name);
        }

        return true;
    }
}
