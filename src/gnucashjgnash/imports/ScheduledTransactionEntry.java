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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractSimpleDataSetter;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractVersionStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;

/**
 * @author albert
 *
 */
public class ScheduledTransactionEntry {
	IdEntry id = new IdEntry();
	String name;
	YesNoEntry enabled = new YesNoEntry();
	YesNoEntry autoCreate = new YesNoEntry();
	YesNoEntry autoCreateNotify = new YesNoEntry();
	IntEntry advanceCreateDays = new IntEntry();
	IntEntry advanceRemindDays = new IntEntry();
	IntEntry instanceCount = new IntEntry();
	GDateEntry start = new GDateEntry();
	GDateEntry last = new GDateEntry();	// Optional
	
	IntEntry numOccur = new IntEntry();
	IntEntry remOccur = new IntEntry();
	GDateEntry end = new GDateEntry();	// Optional
	
	IdEntry templateAccount = new IdEntry();
	
	List<RecurrenceEntry> recurrances = new ArrayList<>();
	List<DeferredInstanceEntry> deferredInstances = new ArrayList<>();
	
	Map<String, SlotEntry> slots = new HashMap<>();
	
	
	
    public boolean validateParse(StateHandler stateHandler, String qName) {
    	if (!this.id.validateGUIDParse(stateHandler, "sx:id")) {
    		return false;
    	}
    	if (!this.enabled.validateParse(stateHandler, "sx:enabled")) {
    		return false;
    	}
    	if (!this.autoCreate.validateParse(stateHandler, "sx:autoCreate")) {
    		return false;
    	}
    	if (!this.autoCreateNotify.validateParse(stateHandler, "sx:autoCreateNotify")) {
    		return false;
    	}
    	if (!this.advanceCreateDays.validateParse(stateHandler, "sx:advanceCreateDays")) {
    		return false;
    	}
    	if (!this.advanceRemindDays.validateParse(stateHandler, "sx:advanceREmindDays")) {
    		return false;
    	}
    	if (!this.instanceCount.validateParse(stateHandler, "sx:instanceCount")) {
    		return false;
    	}
    	if (!this.start.validateParse(stateHandler, "sx:start")) {
    		return false;
    	}
    	if (!this.last.validateParse(stateHandler, "sx:last")) {
    		return false;
    	}
    	if (!this.numOccur.validateParse(stateHandler, "sx:num-occur")) {
    		return false;
    	}
    	if (!this.remOccur.validateParse(stateHandler, "sx:rem-occur")) {
    		return false;
    	}
    	if (!this.end.validateParse(stateHandler, "sx:end")) {
    		return false;
    	}
    	if (!this.templateAccount.validateGUIDParse(stateHandler, "sx:templ-acct")) {
    		return false;
    	}
    	
    	for (RecurrenceEntry recurrenceEntry : this.recurrances) {
    		if (!recurrenceEntry.validateParse(stateHandler, "sx:schedule")) {
    			return false;
    		}
    	}
    	for (DeferredInstanceEntry deferredInstanceEntry : this.deferredInstances) {
    		if (!deferredInstanceEntry.validateParse(stateHandler, "sx:deferredInstance")) {
    			return false;
    		}
    	}
    	
    	// Slots???

    	return true;
    }

    
	static class ScheduledTransactionStateHandler extends AbstractVersionStateHandler {
		final ScheduledTransactionEntry scheduledTransactionEntry = new ScheduledTransactionEntry();

		ScheduledTransactionStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
				String elementName) {
			super(contentHandler, parentStateHandler, elementName);
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractVersionStateHandler#validateVersion(java.lang.String)
		 */
		@Override
		protected boolean validateVersion(String version) {
            if (!version.equals("2.0.0")) {
                this.contentHandler.recordError("Message.Parse.XMLScheduledTransactionVersionUnsupported", version);
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
			case "sx:id":
				return new IdEntry.IdStateHandler(this.scheduledTransactionEntry.id, this.contentHandler, this, qName);
				
			case "sx:name":
                return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
                        @Override
                        protected void setScheduledTransactionEntryField(ScheduledTransactionEntry entry, String value,
                        		ScheduledTransactionStateHandler stateHandler) {
                            entry.name = value;
                        }
                    }); 
 
			case "sx:enabled":
				return new YesNoEntry.YesNoStateHandler(this.scheduledTransactionEntry.enabled, this.contentHandler, this, qName);
                
			case "sx:autoCreate":
				return new YesNoEntry.YesNoStateHandler(this.scheduledTransactionEntry.autoCreate, this.contentHandler, this, qName);
                
			case "sx:autoCreateNotify":
				return new YesNoEntry.YesNoStateHandler(this.scheduledTransactionEntry.autoCreateNotify, this.contentHandler, this, qName);
                
			case "sx:advanceCreateDays":
				return new IntEntry.IntEntryStateHandler(this.scheduledTransactionEntry.advanceCreateDays, this.contentHandler, this, qName);
                
			case "sx:advanceRemindDays":
				return new IntEntry.IntEntryStateHandler(this.scheduledTransactionEntry.advanceRemindDays, this.contentHandler, this, qName);
 
			case "sx:instanceCount":
				return new IntEntry.IntEntryStateHandler(this.scheduledTransactionEntry.instanceCount, this.contentHandler, this, qName);
                
			case "sx:start":
				return new GDateEntry.GDateStateHandler(this.scheduledTransactionEntry.start, this.contentHandler, this, qName);
                
			case "sx:last":
				return new GDateEntry.GDateStateHandler(this.scheduledTransactionEntry.last, this.contentHandler, this, qName);
                
			case "sx:end":
				return new GDateEntry.GDateStateHandler(this.scheduledTransactionEntry.end, this.contentHandler, this, qName);
				
			case "sx:num-occur":
				return new IntEntry.IntEntryStateHandler(this.scheduledTransactionEntry.numOccur, this.contentHandler, this, qName);

			case "sx:rem-occur":
				return new IntEntry.IntEntryStateHandler(this.scheduledTransactionEntry.remOccur, this.contentHandler, this, qName);
				
			case "sx:templ-acct":
				return new IdEntry.IdStateHandler(this.scheduledTransactionEntry.templateAccount, this.contentHandler, this, qName);
				
			case "sx:schedule":
				return new RecurrenceEntry.RecurrencesStateHandler(this.scheduledTransactionEntry.recurrances, this.contentHandler, this, qName);

			case "sx:deferredInstance":
				DeferredInstanceEntry deferredInstanceEntry = new DeferredInstanceEntry();
				this.scheduledTransactionEntry.deferredInstances.add(deferredInstanceEntry);
				return new DeferredInstanceEntry.DeferredInstanceStateHandler(deferredInstanceEntry, this.contentHandler, this, qName);
				
			case "sx:slots":
                return new SlotEntry.SlotsStateHandler(this.scheduledTransactionEntry.slots, this.contentHandler, this, qName);
				
			}

			return super.getStateHandlerForElement(qName);
		}
		

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#endState()
		 */
		@Override
		protected void endState() {
			super.endState();
			
			this.contentHandler.addScheduledTransactionEntry(this.scheduledTransactionEntry);
		}
		
	}

	
    static abstract class SimpleDataSetterImpl extends AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            ScheduledTransactionStateHandler parentStateHandler = (ScheduledTransactionStateHandler)stateHandler.parentStateHandler;
            setScheduledTransactionEntryField(parentStateHandler.scheduledTransactionEntry, characters, parentStateHandler);
        }

        protected abstract void setScheduledTransactionEntryField(ScheduledTransactionEntry entry, String value,
        		ScheduledTransactionStateHandler stateHandler);
    }
 
}
