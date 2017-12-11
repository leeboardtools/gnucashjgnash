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

import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.Map;

public class SlotEntry {
    String key;
    String valueType;
    String value;


    public static String getStringSlotValue(final Map<String, SlotEntry> slotEntries, final String key, final String defValue) {
        final SlotEntry slotEntry = slotEntries.get(key);
        if (slotEntry == null) {
            return defValue;
        }
        if (!"string".equals(slotEntry.valueType)) {
            return defValue;
        }
        return slotEntry.value;
    }


    static class SlotsStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final Map<String, SlotEntry> slotEntries;
        SlotsStateHandler(GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName,
                          Map<String, SlotEntry> slotEntries) {
            super(contentHandler, parentStateHandler, elementName, _getSlotsStateHandlerQNameToStateHandlers());
            this.slotEntries = slotEntries;
        }
    }

    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _SlotsStateHandlerQNameToStateHandlers = null;
    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _getSlotsStateHandlerQNameToStateHandlers() {
        if (_SlotsStateHandlerQNameToStateHandlers == null) {
            _SlotsStateHandlerQNameToStateHandlers = new HashMap<>();
            _SlotsStateHandlerQNameToStateHandlers.put("slot", new GnuCashToJGnashContentHandler.StateHandlerCreator() {
                @Override
                public GnuCashToJGnashContentHandler.StateHandler createStateHandler(GnuCashToJGnashContentHandler contentHandler,
                                                                                     GnuCashToJGnashContentHandler.StateHandler parentStateHandler,
                                                                                     String elementName) {
                    SlotsStateHandler slotsStateHandler = (SlotsStateHandler)parentStateHandler;
                    return new SlotStateHandler(contentHandler, parentStateHandler, elementName, slotsStateHandler.slotEntries);
                }
            });
        }
        return _SlotsStateHandlerQNameToStateHandlers;
    }



    static class SlotStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final Map<String, SlotEntry> slotEntries;
        final SlotEntry slotEntry = new SlotEntry();

        SlotStateHandler(GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName,
                         Map<String, SlotEntry> slotEntries) {
            super(contentHandler, parentStateHandler, elementName, _getSlotStateHandlerQNameToStateHandlers());
            this.slotEntries = slotEntries;
        }

        @Override
        protected void endState() {
            super.endState();

            if (this.slotEntry.key == null) {
                recordWarning("SlotKeyMissing", "Message.Parse.XMLSlotKeyMissing", this.elementName);
                return;
            }
            if (this.slotEntry.valueType == null) {
                recordWarning("SlotValueTypeMissing", "Message.Parse.XMLSlotValueTypeMissing",
                        this.elementName, this.slotEntry.key, "slot:value");
                return;
            }

            if (this.slotEntries.containsKey(this.slotEntry.key)) {
                recordWarning("DuplicateSlotKey", "Message.Parse.XMLDuplicateSlotKey", this.elementName, this.slotEntry.key);
            }
            this.slotEntries.put(this.slotEntry.key, this.slotEntry);
        }
    }

    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _SlotStateHandlerQNameToStateHandlers = null;
    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _getSlotStateHandlerQNameToStateHandlers() {
        if (_SlotStateHandlerQNameToStateHandlers == null) {
            Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> stateHandlers = _SlotStateHandlerQNameToStateHandlers = new HashMap<>();

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "slot:key", new SimpleDataSetterImpl() {
                @Override
                protected void setSlotEntryField(SlotEntry slotEntry, String value) {
                    slotEntry.key = value;
                }
            });

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "slot:value", new SimpleDataSetterImpl() {
                @Override
                protected void setSlotEntryAttributes(SlotEntry slotEntry, Attributes attr) {
                    slotEntry.valueType = attr.getValue("type");
                }

                @Override
                protected void setSlotEntryField(SlotEntry slotEntry, String value) {
                    slotEntry.value = value;
                }
            });
        }
        return _SlotStateHandlerQNameToStateHandlers;
    }

    static abstract class SimpleDataSetterImpl extends GnuCashToJGnashContentHandler.AbstractSimpleDataSetter {
        @Override
        public void setAttributes(Attributes atts, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            SlotStateHandler parentStateHandler = (SlotStateHandler)stateHandler.parentStateHandler;
            setSlotEntryAttributes(parentStateHandler.slotEntry, atts);
        }

        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            SlotStateHandler parentStateHandler = (SlotStateHandler)stateHandler.parentStateHandler;
            setSlotEntryField(parentStateHandler.slotEntry, characters);
        }

        protected void setSlotEntryAttributes(SlotEntry slotEntry, Attributes attr) {

        }
        protected abstract void setSlotEntryField(SlotEntry slotEntry, String value);
    }
}
