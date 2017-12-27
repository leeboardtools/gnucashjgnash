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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;

/**
 * Represents a parsed GnuCash GDate from <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>
 * @author albert
 *
 */
public class GDateEntry extends ParsedEntry {
	LocalDate localDate = LocalDate.now();
    boolean isParsed = false;
    String parseError;
    
    /**
	 * @param contentHandler
	 */
	protected GDateEntry(ParsedEntry parentParsedEntry) {
		super(null);
		this.parentSource = parentParsedEntry;
	}


    /* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getIndentifyingText(gnucashjgnash.imports.GnuCashToJGnashContentHandler)
	 */
	@Override
	public String getIndentifyingText(GnuCashToJGnashContentHandler contentHandler) {
		return null;
	}


	public boolean isParsed() {
    	return this.isParsed;
    }
    
    public boolean validateParse(StateHandler stateHandler, String qName) {
    	if (this.parseError != null) {
            stateHandler.recordWarningOld("GDateParseError_" + qName, "Message.Parse.XMLGDateParseError", qName, this.parseError);
    		return false;
    	}
    	return true;
    }
    
    
    public static class GDateStateHandler extends AbstractStateHandler {
        final GDateEntry gDateEntry;
        String gDateValueStr;
        
        GDateStateHandler(GDateEntry gDateEntry, GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                String elementName) {
            super(contentHandler, parentStateHandler, elementName);
            this.gDateEntry = gDateEntry;
            gDateEntry.updateLocatorInfo(contentHandler);
        }
        
        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return this.gDateEntry;
        }
        

        /* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "gdate" :
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                    @Override
                    protected void setGDateEntryField(GDateEntry timeEntry, String value,
                    		GDateStateHandler parentStateHandler) {
                       	parentStateHandler.gDateValueStr = value;
                    }
                });
			}
			
			return super.getStateHandlerForElement(qName);
		}


		/* (non-Javadoc)
         * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#endState()
         */
        @Override
        protected void endState() {
            super.endState();
            
            try {
                this.gDateEntry.parseError = null;
                String valueStr = (this.gDateValueStr == null) ? this.characters : this.gDateValueStr;
                this.gDateEntry.localDate = LocalDate.parse(valueStr);
                this.gDateEntry.isParsed = true;
            }
            catch (DateTimeParseException e) {
                this.gDateEntry.parseError = e.getLocalizedMessage();
            }
        }
        
    }


    static abstract class SimpleDataSetterImpl extends GnuCashToJGnashContentHandler.AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            GDateStateHandler parentStateHandler = (GDateStateHandler)stateHandler.parentStateHandler;
            setGDateEntryField(parentStateHandler.gDateEntry, characters, parentStateHandler);
        }

        protected abstract void setGDateEntryField(GDateEntry timeEntry, String value,
        		GDateStateHandler parentStateHandler);
    }
    
}
