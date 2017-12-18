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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
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
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.SecurityNode;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionTag;

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
            	if (entry.splits.size() == 1) {
	            	SplitEntry splitEntry = entry.splitsList.get(0);
	            	AccountImportEntry accountImportEntry = this.contentHandler.accountImportEntries.get(splitEntry.account.id);
	            	String accountName = (accountImportEntry != null) ? accountImportEntry.name : null;
	                recordWarning("TooFewSplits", "Message.Parse.XMLTooFewTransactionSplits", entry.id, entry.datePosted.toDateString(), accountName,
	                		splitEntry.value.toBigDecimal(), splitEntry.quantity.toBigDecimal());
            	}
                return;
            }
            
            if (this.contentHandler.transactionEntries.containsKey(entry.id.id)) {
                recordWarning("DuplicateTransaction", "Message.Parse.XMLDuplicateTransaction", entry.id, entry.datePosted.toDateString());
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

    
    /**
     * The main entry point for generating the jGnash transaction.
     * @param contentHandler
     * @param engine
     * @return
     */
    public boolean generateJGnashTransaction(GnuCashToJGnashContentHandler contentHandler, Engine engine) {
        for (SplitEntry splitEntry : this.splitsList) {
            if (!splitEntry.validateForJGnash(contentHandler)) {
                return true;
            }
        }
        
        // Is this an investment transaction?
        ArrayList<SplitEntry> investmentSplitEntries = new ArrayList<>();
        for (SplitEntry splitEntry : this.splitsList) {
            if (splitEntry.jGnashSecurity != null) {
                investmentSplitEntries.add(splitEntry);
            }
        }
        
        if (!investmentSplitEntries.isEmpty()) {
        	return handleJGnashInvestmentTransaction(investmentSplitEntries, contentHandler, engine);
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

        if (!generateJGnashSplitTransactionEntries(contentHandler, transaction)) {
            return false;
        }
        
        engine.addTransaction(transaction);
        return true;
    }
    
    protected boolean handleJGnashInvestmentTransaction(List<SplitEntry> investmentSplitEntries, GnuCashToJGnashContentHandler contentHandler, Engine engine) {
    	InvestmentTransaction transaction = null;
    	
    	if (investmentSplitEntries.size() == 1) {
    		// Buy or sell?
    		SplitEntry securitySplitEntry = investmentSplitEntries.get(0);
    		SecurityNode securityNode = securitySplitEntry.jGnashSecurity;
    		BigDecimal quantity = securitySplitEntry.quantity.toBigDecimal();
    		BigDecimal price;
    		try {
    			price = securitySplitEntry.value.divide(securitySplitEntry.quantity);
    		}
    		catch (ArithmeticException e) {
    			price = BigDecimal.ZERO;
    		}
    		
    		BigDecimal exchangeRate = BigDecimal.ONE;
    		
    		SplitEntry accountSplitEntry = null;
    		Account account = null;
			List<SplitEntry> feeSplitEntries = new ArrayList<>();
			
    		if (this.splitsList.size() == 2) {
    			accountSplitEntry = (this.splitsList.get(0) == securitySplitEntry) ? this.splitsList.get(1) : this.splitsList.get(0);
    			account = contentHandler.jGnashAccounts.get(accountSplitEntry.account.id);
    			if (account == null) {
					// TODO Record Warning 
    				//return true;
    			}
    		}
    		else {
	    		for (SplitEntry splitEntry : this.splitsList) {
	    			if (splitEntry == securitySplitEntry) {
	    				continue;
	    			}
	    			
	    			Account jGnashAccount = contentHandler.jGnashAccounts.get(splitEntry.account.id);
	    			if (jGnashAccount != null) {
	    				switch (jGnashAccount.getAccountType()) {
	    				case ASSET:
	    				case BANK:
	    				case CASH:
	    				case CHECKING:
						case INVEST:
						case MUTUAL :
							if (account != null) {
								// TODO Record Warning
					            System.out.print("Multiple Investment Transaction Accounts: _Investment Split Entries:");
					            System.out.print("\t" + this.datePosted.localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
					            for (SplitEntry entry : this.splitsList) {
					                if (splitEntry.jGnashSecurity != null) {
					                    System.out.print("\t" + entry.jGnashSecurity.getSymbol());
					                }
					                else {
					                    System.out.print("\t" + entry.jGnashAccount.getName());
					                }
					                System.out.print("\t" + entry.value.toBigDecimal() + "\t");
					            }
					            System.out.println();
								
								return false;
							}
							accountSplitEntry = splitEntry;
							account = jGnashAccount; 
							continue;
							
						case EXPENSE:
							// A fee, gotta generate the transaction entry later because we need the account.
							feeSplitEntries.add(splitEntry);
							continue;
							
						default:
							break;
	    				
	    				}
	    			}
	    			
	    			// TODO: Record this warning...
	    			if (jGnashAccount == null) {
	    				break;
	    			}
	    		}
    		}
    		
    		if (account != null) {
        		List<TransactionEntry> fees = new ArrayList<>();
        		List<TransactionEntry> gains = new ArrayList<>();

        		for (SplitEntry feeSplitEntry : feeSplitEntries) {
    				BigDecimal feeAmount = feeSplitEntry.value.toBigDecimal();
    				TransactionEntry transactionEntry = generateJGnashTransactionEntry(contentHandler, accountSplitEntry, feeAmount.negate(),
    						feeSplitEntry, feeAmount);
    				transactionEntry.setTransactionTag(TransactionTag.INVESTMENT_FEE);
    				fees.add(transactionEntry);
    			}
    			
        		Account investmentAccount = (securitySplitEntry.jGnashAccount != null) ? securitySplitEntry.jGnashAccount : account;  

        		if (quantity.compareTo(BigDecimal.ZERO) < 0) {
    				quantity = quantity.abs();
    				price = price.abs();
    				transaction = TransactionFactory.generateSellXTransaction(account, investmentAccount, securityNode, price, quantity, exchangeRate, 
    						this.datePosted.localDate, this.description, fees, gains);
    			}
    			else {
    				// If one account is an income account, then it's most likely a reinvested dividend.
    				// We want to track the income, so we can't use InvestmentTransaction generateReinvestDividendXTransaction() because
    				// it doesn't create an income entry anywhere. 
    				// Instead we need to create a dividend transaction followed by a buy transaction.
    				if (account.getAccountType() == AccountType.INCOME) {
    		            BigDecimal amount = securitySplitEntry.value.toBigDecimal();
    		            transaction = TransactionFactory.generateDividendXTransaction(account, investmentAccount, investmentAccount, 
    		            		securityNode, amount, amount, amount, this.datePosted.localDate, this.description);
    		            engine.addTransaction(transaction);
    		            
    		            // The dividend is now in the investment account, we're buying from there...
    		            account = investmentAccount;
    				}
    				
    				transaction = TransactionFactory.generateBuyXTransaction(account, investmentAccount, securityNode, price, quantity, exchangeRate, 
    						this.datePosted.localDate, this.description, fees);
    			}
    		}
    	}
        
    	if (transaction == null) {
            System.out.print("Skipping: _Investment Split Entries:");
            System.out.print("\t" + this.datePosted.localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            for (SplitEntry splitEntry : this.splitsList) {
                if (splitEntry.jGnashSecurity != null) {
                    System.out.print("\t[" + splitEntry.jGnashSecurity.getSymbol() + "]");
                }
                else {
                    System.out.print("\t" + splitEntry.jGnashAccount.getName());
                }
                System.out.print("\t" + splitEntry.value.toBigDecimal() + "\t" + splitEntry.quantity.toBigDecimal() + "\t");
            }
            System.out.println();
    		return true;
    	}
    	
    	engine.addTransaction(transaction);
    	return true;
    }

    /*
    public static InvestmentTransaction generateBuyXTransaction(final Account account, final Account investmentAccount,
            final SecurityNode node, final BigDecimal price,
            final BigDecimal quantity, final BigDecimal exchangeRate,
            final LocalDate date, final String memo,
            final Collection<TransactionEntry> fees) {
    */
    
    final static SplitEntry assignEntryIfNull(SplitEntry refEntry, SplitEntry newEntry) {
    	return (refEntry == null) ? newEntry : refEntry;
    }
    
    protected boolean generateJGnashSplitTransactionEntries(GnuCashToJGnashContentHandler contentHandler, Transaction transaction) {
        // Look for an account to serve as the main account. This will receive all the credits, and disburse all the debits.
        SplitEntry masterSplitEntry = null;
        
        // Look for a bank, cash, or checking account first, these in case we don't find the others.
        SplitEntry assetSplitEntry = null;
        SplitEntry investSplitEntry = null;
        SplitEntry creditSplitEntry = null;
        SplitEntry simpleInvestSplitEntry = null;
        SplitEntry incomeSplitEntry = null;
        SplitEntry expenseSplitEntry = null;
        
        for (SplitEntry splitEntry : this.splitsList) {
            Account jGnashAccount = splitEntry.jGnashAccount;
            switch (jGnashAccount.getAccountType()) {
            case BANK :
            case CASH :
            case CHECKING :
                masterSplitEntry = assignEntryIfNull(masterSplitEntry, splitEntry);
                break;
                
            case ASSET :
                assetSplitEntry = assignEntryIfNull(assetSplitEntry, splitEntry);
                break;
                
            case CREDIT:
                creditSplitEntry = assignEntryIfNull(creditSplitEntry, splitEntry);
                break;
                
            case INVEST :
                investSplitEntry = assignEntryIfNull(investSplitEntry, splitEntry);
                break;
                
            case SIMPLEINVEST :
                simpleInvestSplitEntry = assignEntryIfNull(simpleInvestSplitEntry, splitEntry);
                break;
                
            case INCOME :
            	incomeSplitEntry = assignEntryIfNull(incomeSplitEntry, splitEntry);
            	break;
            	
            case EXPENSE :
            	expenseSplitEntry = assignEntryIfNull(expenseSplitEntry, splitEntry);
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
                                    		: (incomeSplitEntry != null) ? incomeSplitEntry
                                    				: (expenseSplitEntry != null) ? expenseSplitEntry
                                    						: null;
        }
        if (masterSplitEntry == null) {
            contentHandler.recordWarning("SplitNoSupportedAccounts", "Message.Warning.SplitNoSupportedAccounts", this.id.id, this.datePosted.toDateString());
            return false;
        }
        
        // We have our master account.
        // Add all the credits to it and remove all the debits...
        for (SplitEntry splitEntry : this.splitsList) {
            if (splitEntry == masterSplitEntry) {
                continue;
            }
            
            BigDecimal bigDecimalValue = splitEntry.value.toBigDecimal();
            TransactionEntry transactionEntry = generateJGnashTransactionEntry(contentHandler,
                    masterSplitEntry, bigDecimalValue.negate(), splitEntry, bigDecimalValue); 
            if (transactionEntry == null) {
                return false;
            }
            
            transaction.addTransactionEntry(transactionEntry);
        }        
        
        return true;
    }
    
    protected TransactionEntry generateJGnashTransactionEntry(GnuCashToJGnashContentHandler contentHandler, SplitEntry splitEntryA, BigDecimal amountA,
        SplitEntry splitEntryB, BigDecimal amountB) {
        SplitEntry creditSplitEntry;
        BigDecimal creditAmount;
        SplitEntry debitSplitEntry;
        BigDecimal debitAmount;
        if (splitEntryA.value.toBigDecimal().compareTo(BigDecimal.ZERO) < 0) {
            creditSplitEntry = splitEntryB;
            creditAmount = amountB;
            debitSplitEntry = splitEntryA;
            debitAmount = amountA;
        }
        else {
            creditSplitEntry = splitEntryA;
            creditAmount = amountA;
            debitSplitEntry = splitEntryB;
            debitAmount = amountB;
        }
        
        // Special security transactions...
        TransactionEntry transactionEntry = null;
        if (creditSplitEntry.jGnashSecurity != null) {
            // Buy
        }
        else if (debitSplitEntry.jGnashSecurity != null) {
            // Sell
        }
        else {
            transactionEntry = new TransactionEntry();
        }
        if (transactionEntry == null) {
            return null;
        }
        
        transactionEntry.setCreditAccount(creditSplitEntry.jGnashAccount);
        transactionEntry.setDebitAccount(debitSplitEntry.jGnashAccount);
        
        transactionEntry.setCreditAmount(creditAmount);
        transactionEntry.setDebitAmount(debitAmount);
        
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
