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

import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;

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
        SlotsStateHandler(Map<String, SlotEntry> slotEntries, GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler,
                          String elementName) {
            super(contentHandler, parentStateHandler, elementName, null);
            this.slotEntries = slotEntries;
        }
        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "slot": 
				return new SlotStateHandler(this.slotEntries, this.contentHandler, this, qName); 
			}
			return super.getStateHandlerForElement(qName);
		}
        
        
    }


    static class SlotStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final Map<String, SlotEntry> slotEntries;
        final SlotEntry slotEntry = new SlotEntry();

        SlotStateHandler(Map<String, SlotEntry> slotEntries, GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler,
                         String elementName) {
            super(contentHandler, parentStateHandler, elementName, null);
            this.slotEntries = slotEntries;
        }

        /* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "slot:key" : 
				return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
		                @Override
		                protected void setSlotEntryField(SlotEntry slotEntry, String value) {
		                    slotEntry.key = value;
		                }
		            });
				
			case "slot:value" : 
				// TODO Need to handle the different attribute types.
				return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
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
			return super.getStateHandlerForElement(qName);
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
