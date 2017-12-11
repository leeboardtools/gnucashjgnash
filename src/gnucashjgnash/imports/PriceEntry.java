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
import java.util.HashMap;
import java.util.Map;

public class PriceEntry {
    IdEntry id = new IdEntry();
    CommodityEntry.CommodityRef commodityRef = new CommodityEntry.CommodityRef();
    CommodityEntry.CurrencyRef currencyRef = new CommodityEntry.CurrencyRef();
    TimeEntry time = new TimeEntry();
    String source;
    String type;
    NumericEntry value = new NumericEntry();



    public static class PriceDBStateHandler extends GnuCashToJGnashContentHandler.AbstractVersionStateHandler {

        PriceDBStateHandler(GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler,
                            String elementName) {
            super(contentHandler, parentStateHandler, elementName, null);
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
        final PriceEntry priceEntry = new PriceEntry();

        PriceStateHandler(GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler,
                          String elementName) {
            super(contentHandler, parentStateHandler, elementName, _getPriceStateHandlerQNameToStateHandlers());
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

            if (this.contentHandler.priceEntries.containsKey(this.priceEntry.id.id)) {
                recordWarning("DuplicatePriceEntry", "Message.Parse.XMLDuplicatePriceEntry", this.priceEntry.id.id);
            }
            this.contentHandler.priceEntries.put(this.priceEntry.id.id, this.priceEntry);
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
                    return new NumericEntry.NumericStateEntry(this.priceEntry.value, this.contentHandler, this, qName);
            }

            return super.getStateHandlerForElement(qName);
        }
    }

    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _PriceStateHandlerQNameToStateHandlers = null;
    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _getPriceStateHandlerQNameToStateHandlers() {
        if (_PriceStateHandlerQNameToStateHandlers == null) {
            Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> stateHandlers = _PriceStateHandlerQNameToStateHandlers = new HashMap<>();

            // price:commodity
            // price:currency
            // price:time
            // price:value are handed in getStateHandlerForElement()...

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "price:source", new SimpleDataSetterImpl() {
                @Override
                protected void setPriceEntryField(PriceEntry priceEntry, String value) {
                    priceEntry.source = value;
                }
            });
        }
        return _PriceStateHandlerQNameToStateHandlers;
    }

    static abstract class SimpleDataSetterImpl extends GnuCashToJGnashContentHandler.AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            PriceStateHandler parentStateHandler = (PriceStateHandler)stateHandler.parentStateHandler;
            setPriceEntryField(parentStateHandler.priceEntry, characters);
        }

        protected abstract void setPriceEntryField(PriceEntry priceEntry, String value);
    }



    public boolean generateJGnashSecurityHistoryNode(GnuCashToJGnashContentHandler contentHandler, Engine engine) {
        SecurityNode securityNode = contentHandler.jGnashSecurities.get(this.commodityRef.id);
        if (securityNode == null) {
            contentHandler.recordWarning("PriceCommodityMissing_" + this.commodityRef.id, "Message.Warning.PriceCommodityMissing", this.commodityRef.id);
            return true;
        }

        LocalDate date = this.time.localDate;
        BigDecimal price = this.value.toBigDecimal();
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