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

import java.util.List;

import org.xml.sax.SAXException;

import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractSimpleDataSetter;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractVersionStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;

/**
 * Represents a parsed GnuCash RecurrenceContent from <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>
 * @author albert
 *
 */
public class RecurrenceEntry {
	IntEntry mult = new IntEntry();
	String periodType;
	GDateEntry start = new GDateEntry();
	String weekendAdj;
	
	
	boolean validateParse(StateHandler stateHandler, String qName) {
		if (!mult.validateParse(stateHandler, "recurrence:mult")) {
			return false;
		}
		if (!start.validateParse(stateHandler, "recurrence:start")) {
			return false;
		}
		
		return true;
	}
	
	
	
	static class RecurrencesStateHandler extends AbstractStateHandler {
		final List<RecurrenceEntry> recurrenceEntries;

		RecurrencesStateHandler(List<RecurrenceEntry> recurrenceEntries, GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
				String elementName) {
			super(contentHandler, parentStateHandler, elementName);
			this.recurrenceEntries = recurrenceEntries;
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "gnc:recurrence" :
				RecurrenceEntry recurrenceEntry = new RecurrenceEntry();
				this.recurrenceEntries.add(recurrenceEntry);
				return new RecurrenceStateHandler(recurrenceEntry, contentHandler, parentStateHandler, qName);
				
			}
			
			return super.getStateHandlerForElement(qName);
		}

	}
	
	
	static class RecurrenceStateHandler extends AbstractVersionStateHandler {
		final RecurrenceEntry recurrenceEntry;

		RecurrenceStateHandler(RecurrenceEntry recurrenceEntry, GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
				String elementName) {
			super(contentHandler, parentStateHandler, elementName);
			this.recurrenceEntry = recurrenceEntry;
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractVersionStateHandler#validateVersion(java.lang.String)
		 */
		@Override
		protected boolean validateVersion(String version) {
            if (!version.equals("1.0.0")) {
                this.contentHandler.recordError("Message.Parse.XMLRecurrenceVersionUnsupported", version);
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
			case "recurrence:mult":
				return new IntEntry.IntEntryStateHandler(this.recurrenceEntry.mult, this.contentHandler, this, qName);

			case "recurrence:period_type":
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                    @Override
                    protected void setRecurrenceEntryField(RecurrenceEntry entry, String value,
                    		RecurrenceStateHandler stateHandler) {
                        entry.periodType = value;
                    }
                }); 

			case "recurrence:start":
				return new GDateEntry.GDateStateHandler(this.recurrenceEntry.start, this.contentHandler, this, qName);
				
			case "recurrence:weekend_adj":
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                    @Override
                    protected void setRecurrenceEntryField(RecurrenceEntry entry, String value,
                    		RecurrenceStateHandler stateHandler) {
                        entry.weekendAdj = value;
                    }
                }); 

			}
			
			return super.getStateHandlerForElement(qName);
		}
		
	}
	

    static abstract class SimpleDataSetterImpl extends AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            RecurrenceStateHandler parentStateHandler = (RecurrenceStateHandler)stateHandler.parentStateHandler;
            setRecurrenceEntryField(parentStateHandler.recurrenceEntry, characters, parentStateHandler);
        }

        protected abstract void setRecurrenceEntryField(RecurrenceEntry entry, String value,
        		RecurrenceStateHandler stateHandler);
    }

}
