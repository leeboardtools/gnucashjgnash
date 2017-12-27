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

import gnucashjgnash.GnuCashConvertUtil;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;

/**
 * Represents a parsed GnuCash DeferredInstance from <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>
 * @author albert
 *
 */
public class DeferredInstanceEntry extends ParsedEntry {
	GDateEntry last = new GDateEntry(this);
	IntEntry remOccur = new IntEntry(this);
	IntEntry instanceCount = new IntEntry(this);

	
	/**
	 * @param contentHandler
	 */
	protected DeferredInstanceEntry(GnuCashToJGnashContentHandler contentHandler, ParsedEntry parentParsedEntry) {
		super(contentHandler);
		this.parentSource = parentParsedEntry;
	}



    /* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getIndentifyingText(gnucashjgnash.imports.GnuCashToJGnashContentHandler)
	 */
	@Override
	public String getIndentifyingText(GnuCashToJGnashContentHandler contentHandler) {
		if (this.last.isParsed()) {
			return GnuCashConvertUtil.getString("Message.ParsedEntry.DeferredInstanceEntry", this.last.localDate.toString());
		}
		return null;
	}



	public boolean validateParse(StateHandler stateHandler, String qName) {
    	if (!this.last.validateParse(stateHandler, "sx:last")) {
    		return false;
    	}
    	if (!this.remOccur.validateParse(stateHandler, "sx:rem-occur")) {
    		return false;
    	}
    	if (!this.instanceCount.validateParse(stateHandler, "sx:instanceCount")) {
    		return false;
    	}
    	
    	return true;
    }
    
    
	
	public static class DeferredInstanceStateHandler extends AbstractStateHandler {
		final DeferredInstanceEntry deferredInstanceEntry;
		
		/**
		 * @param contentHandler
		 * @param parentStateHandler
		 * @param elementName
		 */
		DeferredInstanceStateHandler(DeferredInstanceEntry deferredInstanceEntry, 
				GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
				String elementName) {
			super(contentHandler, parentStateHandler, elementName);
			this.deferredInstanceEntry = deferredInstanceEntry;
			deferredInstanceEntry.updateLocatorInfo(contentHandler);
		}

        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return this.deferredInstanceEntry;
        }
        

        /* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "sx:last":
				return new GDateEntry.GDateStateHandler(this.deferredInstanceEntry.last, this.contentHandler, this, qName);
			
			case "sx:rem-occur":
				return new IntEntry.IntEntryStateHandler(this.deferredInstanceEntry.remOccur, this.contentHandler, this, qName);
				
			case "sx:instanceCount":
				return new IntEntry.IntEntryStateHandler(this.deferredInstanceEntry.instanceCount, this.contentHandler, this, qName);
			}

			return super.getStateHandlerForElement(qName);
		}
	}
	
    
}
