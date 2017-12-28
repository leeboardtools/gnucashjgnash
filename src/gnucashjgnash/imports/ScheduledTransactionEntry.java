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

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnucashjgnash.GnuCashConvertUtil;
import gnucashjgnash.NoticeTree.Source;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractSimpleDataSetter;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractVersionStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.Transaction;
import jgnash.engine.recurring.DailyReminder;
import jgnash.engine.recurring.MonthlyReminder;
import jgnash.engine.recurring.OneTimeReminder;
import jgnash.engine.recurring.RecurringIterator;
import jgnash.engine.recurring.Reminder;
import jgnash.engine.recurring.WeeklyReminder;

/**
 * Represents a parsed GnuCash ScheduledTransaction from <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>
 * @author albert
 *
 */
public class ScheduledTransactionEntry extends ParsedEntry {
	IdEntry id = new IdEntry(this);
	String name;
	YesNoEntry enabled = new YesNoEntry(this);
	YesNoEntry autoCreate = new YesNoEntry(this);
	YesNoEntry autoCreateNotify = new YesNoEntry(this);
	IntEntry advanceCreateDays = new IntEntry(this);
	IntEntry advanceRemindDays = new IntEntry(this);
	IntEntry instanceCount = new IntEntry(this);
	GDateEntry start = new GDateEntry(this);
	GDateEntry last = new GDateEntry(this);	// Optional
	
	IntEntry numOccur = new IntEntry(this);
	IntEntry remOccur = new IntEntry(this);
	GDateEntry end = new GDateEntry(this);	// Optional
	
	IdEntry templateAccount = new IdEntry(this);
	
	List<RecurrenceEntry> recurrances = new ArrayList<>();
	List<DeferredInstanceEntry> deferredInstances = new ArrayList<>();
	
	Map<String, SlotEntry> slots = new HashMap<>();


	/**
	 * @param contentHandler
	 */
	protected ScheduledTransactionEntry(GnuCashToJGnashContentHandler contentHandler) {
		super(contentHandler);
	}


	
    /* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getParentSource()
	 */
	@Override
	public Source getParentSource() {
		return this.contentHandler.getScheduledTransactionParentSource(this);
	}


	
	/* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getIndentifyingText(gnucashjgnash.imports.GnuCashToJGnashContentHandler)
	 */
	@Override
	public String getIndentifyingText(GnuCashToJGnashContentHandler contentHandler) {
		return this.name;
	}



	/* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getUniqueId()
	 */
	@Override
	public String getUniqueId() {
		return this.id.id;
	}



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
		final ScheduledTransactionEntry scheduledTransactionEntry;

		ScheduledTransactionStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
				String elementName) {
			super(contentHandler, parentStateHandler, elementName);
			this.scheduledTransactionEntry = new ScheduledTransactionEntry(contentHandler);
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
		@Override
		public ParsedEntry getParsedEntry() {
			return this.scheduledTransactionEntry;
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
				return new RecurrenceEntry.RecurrencesStateHandler(this.scheduledTransactionEntry.recurrances, this.scheduledTransactionEntry, 
						this.contentHandler, this, qName);

			case "sx:deferredInstance":
				DeferredInstanceEntry deferredInstanceEntry = new DeferredInstanceEntry(this.contentHandler, this.scheduledTransactionEntry);
				this.scheduledTransactionEntry.deferredInstances.add(deferredInstanceEntry);
				return new DeferredInstanceEntry.DeferredInstanceStateHandler(deferredInstanceEntry, this.contentHandler, this, qName);
				
			case "sx:slots":
                return new SlotEntry.SlotsStateHandler(this.scheduledTransactionEntry.slots, this.scheduledTransactionEntry, 
                		this.contentHandler, this, qName);
				
			}

			return super.getStateHandlerForElement(qName);
		}
		

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#endState()
		 */
		@Override
		protected void endState() {
			super.endState();
			
			if (!this.scheduledTransactionEntry.validateParse(this, "gnc:schedxaction")) {
				return;
			}
			
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
 
    
    
    /**
     * Called by {@link GnuCashToJGnashContentHandler} to process the template transactions.
     * @param contentHandler
     * @param engine
     * @return	<code>false</false> if failed.
     */
    public static boolean processTemplateTransactions(GnuCashToJGnashContentHandler contentHandler, Engine engine) {
    	// Each transaction...
    	for (Map.Entry<String, TransactionImportEntry> entry : contentHandler.templateTransactionImportEntries.entrySet()) {
    		TransactionImportEntry transactionEntry = entry.getValue();
    		if (!processTemplateTransaction(transactionEntry, contentHandler, engine)) {
    			return false;
    		}
    	}
    	return true;
    }
    
    protected static boolean processTemplateTransaction(TransactionImportEntry transactionEntry, 
    		GnuCashToJGnashContentHandler contentHandler, Engine engine) {
    	AccountImportEntry accountImportEntry = null;
    	for (SplitEntry splitEntry : transactionEntry.originalSplitsList) {
    		AccountImportEntry splitAccountImportEntry = contentHandler.templateAccountImportEntries.get(splitEntry.account.id);
    		if (splitAccountImportEntry == null) {
    			contentHandler.recordWarning(transactionEntry, "Message.Warning.TemplateTransactionSplitAccountMissing", 
    					splitEntry.id.id, splitEntry.account.id);
    			return true;
    		}
    		else if (accountImportEntry != null) {
    			if (splitAccountImportEntry != accountImportEntry) {
    				contentHandler.recordWarning(transactionEntry, "Message.Warning.TemplateTransactionSplitAccountsDifferent", 
        					transactionEntry.id.id);
    				return true;
    			}
    		}
    		else {
    			accountImportEntry = splitAccountImportEntry;
    		}
    	}
    	if (accountImportEntry == null) {
    		return true;
    	}
    	
    	// OK, generate the transaction(s).
    	TransactionImportEntry normalTransactionEntry = templateTransactionToNormal(transactionEntry, contentHandler);
    	if (normalTransactionEntry == null) {
    		return true;
    	}
    	
    	if (!normalTransactionEntry.generateJGnashTransaction(contentHandler, accountImportEntry.jGnashTemplateTransactions)) {
    		return false;
    	}
    	
    	return true;
    }
    
    
    /**
     * Converts a parsed template transaction import entry into a transaction import entry that can be used
     * to generate the jGnash transactions. 
     * @param originalTransactionEntry
     * @param contentHandler
     * @return	<code>false</code> if failed.
     */
    public static TransactionImportEntry templateTransactionToNormal(TransactionImportEntry originalTransactionEntry,
    		GnuCashToJGnashContentHandler contentHandler) {
    	TransactionImportEntry normalTransactionEntry = new TransactionImportEntry(null);
    	normalTransactionEntry.lineNumber = originalTransactionEntry.lineNumber;
    	normalTransactionEntry.columnNumber = originalTransactionEntry.columnNumber;
    	normalTransactionEntry.isTemplateTransaction = true;
    	
    	normalTransactionEntry.currencyRef = originalTransactionEntry.currencyRef;
    	normalTransactionEntry.num = originalTransactionEntry.num;
    	normalTransactionEntry.dateEntered = originalTransactionEntry.dateEntered;
    	normalTransactionEntry.datePosted = originalTransactionEntry.datePosted;
    	normalTransactionEntry.description = originalTransactionEntry.description;
    	normalTransactionEntry.slots = originalTransactionEntry.slots;
    	
    	for (SplitEntry originalSplitEntry : originalTransactionEntry.originalSplitsList) {
    		SplitEntry normalSplitEntry = new SplitEntry(contentHandler, normalTransactionEntry);
    		normalSplitEntry.columnNumber = originalSplitEntry.columnNumber;
    		normalSplitEntry.lineNumber = originalSplitEntry.lineNumber;
    		normalSplitEntry.memo = originalSplitEntry.memo;
    		normalSplitEntry.reconciledState = originalSplitEntry.reconciledState;
    		normalSplitEntry.value = originalSplitEntry.value;
    		normalSplitEntry.quantity = originalSplitEntry.quantity;
    		
    		if (!templateSplitEntrySlotsToNormal(originalSplitEntry, normalSplitEntry, contentHandler)) {
    			return null;
    		}
    		
    		normalTransactionEntry.originalSplitsList.add(normalSplitEntry);
    	}
    	
    	return normalTransactionEntry;
    }
    
    
    protected static boolean templateSplitEntrySlotsToNormal(SplitEntry originalSplitEntry, SplitEntry normalSplitEntry,
    		GnuCashToJGnashContentHandler contentHandler) {
    	SlotEntry slotEntry = originalSplitEntry.slots.get("sched-xaction");
    	if (slotEntry == null) {
    		contentHandler.recordWarning(originalSplitEntry, "Message.Warning.SchedXActionSlotMissing", originalSplitEntry.id.id);
    		return false;
    	}
    	if (slotEntry.frameSlotEntries == null) {
    		contentHandler.recordWarning(originalSplitEntry, "Message.Warning.SchedXActionSlotNotFrame", originalSplitEntry.id.id);
    		return false;
    	}
    	SlotEntry accountSlotEntry = slotEntry.frameSlotEntries.get("account");
    	if (accountSlotEntry == null) {
    		contentHandler.recordWarning(originalSplitEntry, "Message.Warning.SchedXActionSlotMissingAccount", originalSplitEntry.id.id);
    		return false;
    	}
    	
    	normalSplitEntry.account.id = accountSlotEntry.value;
    	
    	SlotEntry creditFormulaSlotEntry = slotEntry.frameSlotEntries.get("credit-formula");
    	SlotEntry debitFormulaSlotEntry = slotEntry.frameSlotEntries.get("debit-formula");
    	
    	if ((creditFormulaSlotEntry != null) && !creditFormulaSlotEntry.value.isEmpty()) {
    		try {
    			normalSplitEntry.value.fromRealString(creditFormulaSlotEntry.value, BigInteger.valueOf(1000));
    			normalSplitEntry.value.numerator = normalSplitEntry.value.numerator.negate(); 
    		}
    		catch (NumberFormatException e) {
        		contentHandler.recordWarning(originalSplitEntry, "Message.Warning.SchedXActionSlotCreditFormulaValueInvalid", 
        				originalSplitEntry.id.id, e.getLocalizedMessage());
    			return false;
    		}
    	}
    	else if ((debitFormulaSlotEntry != null) && !debitFormulaSlotEntry.value.isEmpty()) {
    		try {
    			normalSplitEntry.value.fromRealString(debitFormulaSlotEntry.value, BigInteger.valueOf(1000));
    		}
    		catch (NumberFormatException e) {
        		contentHandler.recordWarning(originalSplitEntry, "Message.Warning.SchedXActionSlotDebitFormulaValueInvalid", 
        				originalSplitEntry.id.id, e.getLocalizedMessage());
    			return false;
    		}
    	}
    	else {
    	}
    	
    	normalSplitEntry.quantity.fromRealString("1", BigInteger.ONE);

    	return true;
    }
    
    
    /**
     * Called by {@link GnuCashToJGnashContentHandler} to generate and add the jGnash Reminders for this scheduled transaction.
     * @param contentHandler
     * @param engine
     * @return	<code>false</code> if failed.
     */
    public boolean generateJGnashScheduledTransaction(GnuCashToJGnashContentHandler contentHandler, Engine engine) {
    	AccountImportEntry templateAccount = contentHandler.templateAccountImportEntries.get(this.templateAccount.id);
    	if (templateAccount == null) {
    		contentHandler.recordWarning(this, "Message.Warning.TemplateAccountMissing", this.name, this.templateAccount.id);
    		return true;
    	}
    	
    	String recurrenceSuffix = "";
    	int recurrenceIndex = 0;
    	for (RecurrenceEntry recurrenceEntry : this.recurrances) {
    		if (recurrenceIndex > 0) {
    			recurrenceSuffix = "_" + (recurrenceIndex + 1);
    		}
    		for (Transaction jGnashTemplateTransaction : templateAccount.jGnashTemplateTransactions) {
	    		if (!generateJGnashReminder(recurrenceEntry, recurrenceSuffix, jGnashTemplateTransaction, templateAccount, contentHandler, engine)) {
	    			return false;
	    		}
    		}
    		++recurrenceIndex;
    	}
    	
    	return true;
    }
    
    protected boolean generateJGnashReminder(RecurrenceEntry recurrenceEntry, String recurrenceSuffix, Transaction jGnashTemplateTransaction,
    		AccountImportEntry templateAccount, GnuCashToJGnashContentHandler contentHandler, Engine engine) {
    	
    	Account jGnashAccount = jGnashTemplateTransaction.getAccounts().iterator().next();
    	MonthlyReminder monthlyReminder;
    	Reminder jGnashReminder = null;
    	int increment = recurrenceEntry.mult.value;
    	LocalDate startDate = (recurrenceEntry.start.isParsed) ? recurrenceEntry.start.localDate : this.start.localDate;
    	LocalDate endDate = (this.end.isParsed) ? this.end.localDate : null;
    	GDateEntry lastDate = this.last;
    	
    	switch (recurrenceEntry.periodType) {
    	case "once" :
    		jGnashReminder = new OneTimeReminder();
    		break;
    		
    	case "day" :
    		jGnashReminder = new DailyReminder();
    		break;
    		
    	case "week" :
    		jGnashReminder = new WeeklyReminder();
    		break;
    		
    	case "month" :
    		jGnashReminder = new MonthlyReminder();
    		break;
    		
    	case "end of month" :
    		jGnashReminder = new MonthlyReminder();
    		break;
    		
    	case "nth weekday" :
    		//jGnashReminder = new WeeklyReminder();
    		break;
    		
    	case "last weekday" :
    		monthlyReminder = new MonthlyReminder();
    		monthlyReminder.setType(1);
    		jGnashReminder = monthlyReminder;
    		break;
    		
    	case "year" :
    		jGnashReminder = new MonthlyReminder();
    		increment = 12;
    		break;
    	}
    	
    	if (jGnashReminder == null) {
    		contentHandler.recordWarning(this, "Message.Warning.UnsupportedRecurrencePeriod", 
    				this.name, recurrenceEntry.periodType);
    		return true;
    	}
    	
   		jGnashReminder.setIncrement(increment);
    	
    	jGnashReminder.setDaysAdvance(this.advanceCreateDays.value);
    	jGnashReminder.setAutoCreate(this.autoCreate.value);
    	jGnashReminder.setDescription(this.name + recurrenceSuffix);
    	jGnashReminder.setEnabled(this.enabled.value);
    	jGnashReminder.setStartDate(startDate);
    	if (endDate != null) {
    		jGnashReminder.setEndDate(endDate);
    	}
    	
    	if (lastDate.isParsed) {
    		RecurringIterator iterator = jGnashReminder.getIterator();
    		int advanceCount = 0;
    		LocalDate nextDate = iterator.next();
    		while (nextDate != null) {
    			if (nextDate.isAfter(lastDate.localDate)) {
    				break;
    			}
    			
    			++advanceCount;
    			nextDate = iterator.next();
    		}
    		
    		while (--advanceCount >= 0) {
    			jGnashReminder.setLastDate();
    		}
    	}
    	
    	jGnashReminder.setAccount(jGnashAccount);
    	jGnashReminder.setTransaction(jGnashTemplateTransaction);
    	
    	engine.addReminder(jGnashReminder);
    	
    	return true;
    }
}
