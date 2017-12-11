package gnucashjgnash.imports;

import jgnash.engine.*;

import java.util.HashMap;
import java.util.Map;

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
            super(contentHandler, parentStateHandler, elementName, _getCommodityStateHandlerQNameToStateHandlers());
        }

        @Override
        protected boolean validateVersion(String version) {
            if (!version.equals("2.0.0")) {
                this.contentHandler.recordError("Message.Parse.XMLCommodityVersionUnsupported", version);
                return false;
            }
            return true;
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

            if (this.contentHandler.commodityEntries.containsKey(this.commodityEntry.id)) {
                recordWarning("DuplicateCommodityId", "Message.Parse.XMLDuplicateCommodityId", this.commodityEntry.id);
            }
            this.contentHandler.commodityEntries.put(this.commodityEntry.id, this.commodityEntry);
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


    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _CommodityStateHandlerQNameToStateHandlers = null;
    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _getCommodityStateHandlerQNameToStateHandlers() {
        if (_CommodityStateHandlerQNameToStateHandlers == null) {
            Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> stateHandlers = _CommodityStateHandlerQNameToStateHandlers = new HashMap<>();

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:space", new SimpleDataSetterImpl() {
                @Override
                protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                    commodityEntry.space = value;
                }
            });

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:id", new SimpleDataSetterImpl() {
                @Override
                protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                    commodityEntry.id = value;
                }
            });

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:name", new SimpleDataSetterImpl() {
                @Override
                protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                    commodityEntry.name = value;
                }
            });

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:xcode", new SimpleDataSetterImpl() {
                @Override
                protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                    commodityEntry.xCode = value;
                }
            });

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:fraction", new SimpleDataSetterImpl() {
                @Override
                protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                    commodityEntry.fraction = value;
                }
            });

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:get_quotes", new SimpleDataSetterImpl() {
                @Override
                protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                    commodityEntry.isGetQuotes = true;
                }
            });

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:quote_source", new SimpleDataSetterImpl() {
                @Override
                protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                    commodityEntry.quoteSource = value;
                }
            });

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:quote_tz", new SimpleDataSetterImpl() {
                @Override
                protected void setCommodityEntryField(CommodityEntry commodityEntry, String value) {
                    commodityEntry.quoteTimeZone = value;
                }
            });

            stateHandlers.put("cmdty:slots", new GnuCashToJGnashContentHandler.StateHandlerCreator() {
                @Override
                public GnuCashToJGnashContentHandler.StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler,
                                                                                     GnuCashToJGnashContentHandler.StateHandler parentStateHandler,
                                                                                     String elementName) {
                    CommodityStateHandler commodityStateHandler = (CommodityStateHandler)parentStateHandler;
                    return new SlotEntry.SlotsStateHandler(contentHandler, parentStateHandler, elementName, commodityStateHandler.commodityEntry.slots);
                }
            });
        }
        return _CommodityStateHandlerQNameToStateHandlers;
    }



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
            super(contentHandler, parentStateHandler, elementName, _getCommodityRefStateHandlerQNameToStateHandlers());
            this.commodityRef = commodityRef;
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

    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _CommodityRefStateHandlerQNameToStateHandlers = null;
    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _getCommodityRefStateHandlerQNameToStateHandlers() {
        if (_CommodityRefStateHandlerQNameToStateHandlers == null) {
            Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> stateHandlers = _CommodityRefStateHandlerQNameToStateHandlers = new HashMap<>();

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:space", new RefSimpleDataSetterImpl() {
                @Override
                protected void setCommodityRefField(CommodityRef commodityRef, String value) {
                    commodityRef.space = value;
                }
            });

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "cmdty:id", new RefSimpleDataSetterImpl() {
                @Override
                protected void setCommodityRefField(CommodityRef commodityRef, String value) {
                    commodityRef.id = value;
                }
            });

        }
        return _CommodityRefStateHandlerQNameToStateHandlers;
    }


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
