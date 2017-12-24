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

import jgnash.engine.Engine;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;

import java.math.BigDecimal;
import java.time.LocalDate;

import gnucashjgnash.GnuCashConvertUtil;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;

/**
 * Represents a parsed GnuCash Price from <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>
 * @author albert
 *
 */
public class PriceEntry extends ParsedEntry {
	IdEntry id = new IdEntry(this);
    CommodityEntry.CommodityRef commodityRef = new CommodityEntry.CommodityRef();
    CommodityEntry.CurrencyRef currencyRef = new CommodityEntry.CurrencyRef();
    TimeEntry time = new TimeEntry();
    String source;
    String type;
    NumericEntry value = new NumericEntry(this);

    static ParsedEntry orphanParsedParentEntry = new ParsedEntry(null) {

		@Override
		public String getIndentifyingText(GnuCashToJGnashContentHandler contentHandler) {
			return GnuCashConvertUtil.getString("Message.ParsedEntry.OrphanedPriceEntryParent");
		}
    	
    };

    /**
	 * @param contentHandler
	 */
	protected PriceEntry(GnuCashToJGnashContentHandler contentHandler) {
		super(contentHandler);
	}


    /* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getParentParsedEntry(gnucashjgnash.imports.GnuCashToJGnashContentHandler)
	 */
	@Override
	public ParsedEntry getParentParsedEntry(GnuCashToJGnashContentHandler contentHandler) {
		if (this.commodityRef.isParsed()) {
			CommodityEntry commodityEntry = contentHandler.commodityEntries.get(this.commodityRef.id);
			if (commodityEntry != null) {
				return commodityEntry;
			}
		}
		else if (this.currencyRef.isParsed()) {
		}
		return orphanParsedParentEntry;
	}



	/* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getIndentifyingText(gnucashjgnash.imports.GnuCashToJGnashContentHandler)
	 */
	@Override
	public String getIndentifyingText(GnuCashToJGnashContentHandler contentHandler) {
		if (time.isParsed()) {
			return time.toString();
		}
		return null;
	}



	/* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getUniqueId()
	 */
	@Override
	public String getUniqueId() {
		return this.id.id;
	}



	public static class PriceDBStateHandler extends GnuCashToJGnashContentHandler.AbstractVersionStateHandler {

        PriceDBStateHandler(GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler,
                            String elementName) {
            super(contentHandler, parentStateHandler, elementName);
        }

        @Override
        protected boolean validateVersion(String version) {
            if (!version.equals("1")) {
                this.contentHandler.recordError("Message.Parse.XMLPriceDBVersionUnsupported", version);
                return false;
            }
            return true;
        }

        @Override
        protected GnuCashToJGnashContentHandler.StateHandler getStateHandlerForElement(String qName) {
            if (qName.equals("price")) {
                return new PriceStateHandler(this.contentHandler, this, qName);
            }
            return super.getStateHandlerForElement(qName);
        }
    }



    public static class PriceStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final PriceEntry priceEntry;

        PriceStateHandler(GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler,
                          String elementName) {
            super(contentHandler, parentStateHandler, elementName);
            this.priceEntry = new PriceEntry(contentHandler);
        }

        @Override
        protected void endState() {
            super.endState();

            if (!this.priceEntry.id.validateGUIDParse(this, "price:id")) {
                return;
            }
            if (!this.priceEntry.commodityRef.validateRef(this, "price:commodity")) {
                return;
            }
            if (!this.priceEntry.currencyRef.validateRef(this, "price:currency")) {
                return;
            }
            if (!this.priceEntry.time.validateParse(this, "price:time")) {
                return;
            }
            if (!this.priceEntry.value.validateParse(this, "price:value")) {
                return;
            }

            this.contentHandler.addPriceEntry(priceEntry);
        }

        @Override
        protected GnuCashToJGnashContentHandler.StateHandler getStateHandlerForElement(String qName) {
            switch (qName) {
                case "price:id":
                    return new IdEntry.IdStateHandler(this.priceEntry.id, this.contentHandler, this, qName);

                case "price:commodity":
                    return new CommodityEntry.CommodityRefStateHandler(this.priceEntry.commodityRef, this.contentHandler, this, qName);

                case "price:currency":
                    return new CommodityEntry.CurrencyRefStateHandler(this.priceEntry.currencyRef, this.contentHandler, this, qName);

                case "price:time":
                    return new TimeEntry.TimeStateHandler(this.priceEntry.time, this.contentHandler, this, qName);

                case "price:value":
                    return new NumericEntry.NumericStateHandler(this.priceEntry.value, this.contentHandler, this, qName);
                    
                case "price:source":
                    return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                            @Override
                            protected void setPriceEntryField(PriceEntry priceEntry, String value) {
                                priceEntry.source = value;
                            }
                        }); 
                    
                case "price:type":
                    return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                            @Override
                            protected void setPriceEntryField(PriceEntry priceEntry, String value) {
                                priceEntry.type = value;
                            }
                        }); 
            }

            return super.getStateHandlerForElement(qName);
        }
    }

    static abstract class SimpleDataSetterImpl extends GnuCashToJGnashContentHandler.AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            PriceStateHandler parentStateHandler = (PriceStateHandler)stateHandler.parentStateHandler;
            setPriceEntryField(parentStateHandler.priceEntry, characters);
        }

        protected abstract void setPriceEntryField(PriceEntry priceEntry, String value);
    }



    public boolean generateJGnashSecurityHistoryNode(GnuCashToJGnashContentHandler contentHandler, Engine engine, SecurityNode securityNode) {
        if (securityNode == null) {
            securityNode = contentHandler.jGnashSecurities.get(this.commodityRef.id);
            if (securityNode == null) {
                contentHandler.recordWarning("PriceCommodityMissing_" + this.commodityRef.id, "Message.Warning.PriceCommodityMissing", this.commodityRef.id);
                return true;
            }
        }

        LocalDate date = this.time.localDate;
        BigDecimal price = null;
        try {
            price = this.value.toBigDecimal();
        }
        catch (Exception e) {
            contentHandler.recordWarning("SecurityHistoryPriceInvalid_" + this.commodityRef.id, "Message.Warning.SecurityHistoryValueInvalid", this.commodityRef.id);
            return true;
        }
        
        BigDecimal high = price;
        BigDecimal low = price;
        long volume = 0;
        SecurityHistoryNode historyNode = new SecurityHistoryNode(date, price, volume, high, low);

        if (!engine.addSecurityHistory(securityNode, historyNode)) {
            return false;
        }

        // We're not going to bother saving the history nodes...
        return true;
    }

}
