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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractSimpleDataSetter;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractVersionStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;
import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;

/**
 * @author albert
 *
 */
public class TransactionImportEntry {
	IdEntry id = new IdEntry();
	CommodityEntry.CurrencyRef currencyRef = new CommodityEntry.CurrencyRef();
	String num;
	TimeEntry datePosted = new TimeEntry();
	TimeEntry dateEntered = new TimeEntry();
	String description;
    Map<String, SlotEntry> slots = new HashMap<>();
    Map<String, SplitEntry> splits = new HashMap<>();
    List<SplitEntry> splitsList = new ArrayList<>();
    
    
    public static class TransactionStateHandler extends AbstractVersionStateHandler {
    	TransactionImportEntry transactionEntry = new TransactionImportEntry();

		/**
		 * @param contentHandler
		 * @param parentStateHandler
		 * @param elementName
		 */
		TransactionStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
				String elementName) {
			super(contentHandler, parentStateHandler, elementName, null);
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractVersionStateHandler#validateVersion(java.lang.String)
		 */
		@Override
		protected boolean validateVersion(String version) {
            if (!version.equals("2.0.0")) {
                this.contentHandler.recordError("Message.Parse.XMLTransactionVersionUnsupported", version);
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
			case "trn:id" :
				return new IdEntry.IdStateHandler(this.transactionEntry.id, this.contentHandler, this, qName);
				
			case "trn:currency" :
                return new CommodityEntry.CurrencyRefStateHandler(this.transactionEntry.currencyRef, this.contentHandler, this, qName);
                
			case "trn:num" :
				return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
		                @Override
		                protected void setTransactionEntryField(TransactionImportEntry priceEntry, String value) {
		                    priceEntry.num = value;
		                }
		            }); 
				
			case "trn:date-posted" :
				return new TimeEntry.TimeStateHandler(this.transactionEntry.datePosted, this.contentHandler, this, qName);
				
			case "trn:date-entered" :
				return new TimeEntry.TimeStateHandler(this.transactionEntry.dateEntered, this.contentHandler, this, qName);
				
			case "trn:description" :
				return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
		                @Override
		                protected void setTransactionEntryField(TransactionImportEntry priceEntry, String value) {
		                    priceEntry.description = value;
		                }
		            }); 
				
			case "trn:slots":
				return new SlotEntry.SlotsStateHandler(this.transactionEntry.slots, this.contentHandler, this, qName);
				
			case "trn:splits":
				return new SplitEntry.SplitsStateHandler(this.transactionEntry.splits, this.transactionEntry.splitsList, "trn:split", this.contentHandler, this, qName);
			}
			return super.getStateHandlerForElement(qName);
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#endState()
		 */
		@Override
		protected void endState() {
			super.endState();
			
			final TransactionImportEntry entry = this.transactionEntry;
			
			if (!entry.id.validateGUIDParse(this, "trn:id")) {
				return;
			}
			if (!entry.currencyRef.validateRef(this, "trn:currency")) {
				return;
			}
			if (!entry.datePosted.validateParse(this, "trn:date-posted")) {
				return;
			}
			if (!entry.dateEntered.validateParse(this, "trn:date-entered")) {
				return;
			}
			if (entry.splits.size() < 2) {
				recordWarning("TooFewSplits", "Message.Parse.XMLTooFewTransactionSplits", entry.id, entry.datePosted);
				return;
			}
			
			if (this.contentHandler.transactionEntries.containsKey(entry.id.id)) {
				recordWarning("DuplicateTransaction", "Message.Parse.XMLDuplicateTransaction", entry.id, entry.datePosted);
			}
			this.contentHandler.transactionEntries.put(entry.id.id, entry);
		}
    	
    }

    
    static abstract class SimpleDataSetterImpl extends AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            TransactionStateHandler parentStateHandler = (TransactionStateHandler)stateHandler.parentStateHandler;
            setTransactionEntryField(parentStateHandler.transactionEntry, characters);
        }

        protected abstract void setTransactionEntryField(TransactionImportEntry priceEntry, String value);
    }

    
    
    public boolean generateJGnashTransaction(GnuCashToJGnashContentHandler contentHandler, Engine engine) {
    	for (SplitEntry splitEntry : this.splitsList) {
    		if (!splitEntry.validateForJGnash(contentHandler)) {
    			return true;
    		}
    	}
    	
    	final Transaction transaction = new Transaction();
    	
    	if (this.description != null) {
    		transaction.setMemo(this.description);
    	}
    	if (this.num != null) {
    		transaction.setNumber(this.num);
    	}
    	
    	// TODO There is a  slot with key date-posted and value type gdate, do we want to use that?
    	transaction.setDate(this.datePosted.localDate);
    	
    	if (this.splits.size() == 2) {
    		final TransactionEntry transactionEntry = generateJGnashTransactionEntry(contentHandler, this.splitsList.get(0), this.splitsList.get(1));
    		transaction.addTransactionEntry(transactionEntry);
    	}
    	else {
    		if (!generateJGnashSplitTransactionEntries(contentHandler, transaction)) {
    			return false;
    		}
    	}
    	
    	engine.addTransaction(transaction);
    	return true;
    }
    
    protected boolean generateJGnashSplitTransactionEntries(GnuCashToJGnashContentHandler contentHandler, Transaction transaction) {
    	// Look for an account to serve as the main account. This will receive all the credits, and disburse all the debits.
    	SplitEntry masterSplitEntry = null;
    	
    	// Look for a bank, cash, or checking account first, these in case we don't find the others.
    	SplitEntry assetSplitEntry = null;
    	SplitEntry investSplitEntry = null;
    	SplitEntry creditSplitEntry = null;
    	SplitEntry simpleInvestSplitEntry = null;
    	
    	for (SplitEntry splitEntry : this.splitsList) {
    		Account jGnashAccount = splitEntry.jGnashAccount;
    		switch (jGnashAccount.getAccountType()) {
    		case BANK :
    		case CASH :
    		case CHECKING :
    			masterSplitEntry = splitEntry;
    			break;
    			
    		case ASSET :
    			assetSplitEntry = splitEntry;
    			break;
    			
    		case CREDIT:
    			creditSplitEntry = splitEntry;
    			break;
    			
    		case INVEST :
    			investSplitEntry = splitEntry;
    			break;
    			
    		case SIMPLEINVEST :
    			simpleInvestSplitEntry = splitEntry;
    			break;
    			
    		default :
    			break;
    		}
    		
    		if (masterSplitEntry != null) {
    			break;
    		}
    	}
    	
    	if (masterSplitEntry == null) {
    		masterSplitEntry = (creditSplitEntry != null) ? creditSplitEntry
    				: (assetSplitEntry != null) ? assetSplitEntry
    						: (investSplitEntry != null) ? investSplitEntry
    								: (simpleInvestSplitEntry != null) ? simpleInvestSplitEntry
    										: null;
    	}
    	if (masterSplitEntry == null) {
    		contentHandler.recordWarning("SplitNoSupportedAccounts", "Message.Warning.SplitNoSupportedAccounts", this.id.id);
    		return false;
    	}
    	
    	// We have our master account.
    	// Add all the credits to it and remove all the debits...
    	for (SplitEntry splitEntry : this.splitsList) {
    		if (splitEntry == masterSplitEntry) {
    			continue;
    		}
    		
    		TransactionEntry transactionEntry = new TransactionEntry();
    		BigDecimal bigDecimalValue = splitEntry.value.toBigDecimal();
    		if (bigDecimalValue.compareTo(BigDecimal.ZERO) < 0) {
    			transactionEntry.setCreditAccount(masterSplitEntry.jGnashAccount);
    			transactionEntry.setCreditAmount(bigDecimalValue.negate());
    			transactionEntry.setDebitAccount(splitEntry.jGnashAccount);
    			transactionEntry.setDebitAmount(bigDecimalValue);
    		}
    		else {
    			transactionEntry.setDebitAccount(masterSplitEntry.jGnashAccount);
    			transactionEntry.setDebitAmount(bigDecimalValue.negate());
    			transactionEntry.setCreditAccount(splitEntry.jGnashAccount);
    			transactionEntry.setCreditAmount(bigDecimalValue);
    		}
    		
    		transactionEntry.setReconciled(masterSplitEntry.jGnashAccount, masterSplitEntry.jGnashReconciledState);
    		transactionEntry.setReconciled(splitEntry.jGnashAccount, splitEntry.jGnashReconciledState);
    		
    		if (splitEntry.memo != null) {
    			transactionEntry.setMemo(splitEntry.memo);
    		}
    		else if (masterSplitEntry.memo != null) {
    			transactionEntry.setMemo(masterSplitEntry.memo);
    		}
    		
    		transaction.addTransactionEntry(transactionEntry);
    	}    	
    	
    	return true;
    }
    
    protected TransactionEntry generateJGnashTransactionEntry(GnuCashToJGnashContentHandler contentHandler, SplitEntry splitEntryA, SplitEntry splitEntryB) {
    	SplitEntry creditSplitEntry;
    	SplitEntry debitSplitEntry;
    	if (splitEntryA.value.toBigDecimal().compareTo(BigDecimal.ZERO) < 0) {
    		creditSplitEntry = splitEntryB;
    		debitSplitEntry = splitEntryA;
    	}
    	else {
    		creditSplitEntry = splitEntryA;
    		debitSplitEntry = splitEntryB;
    	}

    	TransactionEntry transactionEntry = new TransactionEntry();
    	transactionEntry.setCreditAccount(creditSplitEntry.jGnashAccount);
    	transactionEntry.setDebitAccount(debitSplitEntry.jGnashAccount);
    	
    	transactionEntry.setCreditAmount(creditSplitEntry.value.toBigDecimal());
    	transactionEntry.setDebitAmount(debitSplitEntry.value.toBigDecimal());
    	
    	transactionEntry.setReconciled(creditSplitEntry.jGnashAccount, creditSplitEntry.jGnashReconciledState);
    	transactionEntry.setReconciled(debitSplitEntry.jGnashAccount, debitSplitEntry.jGnashReconciledState);
    	
    	if (creditSplitEntry.memo != null) {
    		transactionEntry.setMemo(creditSplitEntry.memo);
    	}
    	else if (debitSplitEntry.memo != null) {
    		transactionEntry.setMemo(debitSplitEntry.memo);
    	}
    	
    	return transactionEntry;
    }
    
}
