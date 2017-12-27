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

import gnucashjgnash.GnuCashConvertUtil;
import gnucashjgnash.NoticeTree.Source;
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
 * Represents a parsed GnuCash Transaction from <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>
 * @author albert
 *
 */
public class TransactionImportEntry extends ParsedEntry {

	IdEntry id = new IdEntry(this);
    CommodityEntry.CurrencyRef currencyRef = new CommodityEntry.CurrencyRef();
    String num;
    TimeEntry datePosted = new TimeEntry(this);
    TimeEntry dateEntered = new TimeEntry(this);
    String description;
    Map<String, SlotEntry> slots = new HashMap<>();
    Map<String, SplitEntry> splits = new HashMap<>();
    List<SplitEntry> originalSplitsList = new ArrayList<>();
    
    boolean isTemplateTransaction = false;

    /**
	 * @param contentHandler
	 */
	protected TransactionImportEntry(GnuCashToJGnashContentHandler contentHandler) {
		super(contentHandler);
	}
	
	
    
    /* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getParentSource()
	 */
	@Override
	public Source getParentSource() {
		return this.contentHandler.getTransactionParentSource(this);
	}



	/* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getIndentifyingText(gnucashjgnash.imports.GnuCashToJGnashContentHandler)
	 */
	@Override
	public String getIndentifyingText(GnuCashToJGnashContentHandler contentHandler) {
		if (this.isTemplateTransaction) {
			return this.description;
		}
		
		if (this.datePosted.isParsed()) {
			return this.datePosted.toDateString();
		}
		return this.description;
	}


	/* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getUniqueId()
	 */
	@Override
	public String getUniqueId() {
		return this.id.id;
	}
	
	
	/**
	 * @return	The id of an account referenced by the transaction, <code>null</code> if none found.
	 */
	public String getAccountId() {
		for (SplitEntry splitEntry : this.originalSplitsList) {
			if (splitEntry.account.isParsed()) {
				return splitEntry.account.id;
			}
		}
		
		return null;
	}



	public static class TransactionStateHandler extends AbstractVersionStateHandler {
        final TransactionImportEntry transactionEntry;

        /**
         * @param contentHandler
         * @param parentStateHandler
         * @param elementName
         */
        TransactionStateHandler(GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                String elementName) {
            super(contentHandler, parentStateHandler, elementName);
            this.transactionEntry = new TransactionImportEntry(contentHandler);
        }


		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
		@Override
		public ParsedEntry getParsedEntry() {
			return this.transactionEntry;
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
                return new SlotEntry.SlotsStateHandler(this.transactionEntry.slots, this.transactionEntry, this.contentHandler, this, qName);
                
            case "trn:splits":
                return new SplitEntry.SplitsStateHandler(this.transactionEntry.splits, this.transactionEntry.originalSplitsList, "trn:split", 
                		this.transactionEntry, this.contentHandler, this, qName);
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
            
            this.contentHandler.addTransactionEntry(entry);
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
     * The main entry point for generating the jGnash transactions, this one adds them to the jGnash engine.
     * @param contentHandler
     * @param engine
     * @return	<code>false</code> if failed.
     */
    public boolean generateJGnashTransaction(GnuCashToJGnashContentHandler contentHandler, Engine engine) {
    	ArrayList<Transaction> jGnashTransactions = new ArrayList<>();
    	if (!generateJGnashTransaction(contentHandler, jGnashTransactions)) {
    		return false;
    	}
    	
    	for (Transaction jGnashTransaction : jGnashTransactions) {
    		engine.addTransaction(jGnashTransaction);
    	}
    	
    	return true;
    }
    

    /**
     * The main entry point for generating the jGnash transactions.
     * @param contentHandler
     * @param jGnashTransactions
     * @return	<code>false</code> if failed.
     */
    public boolean generateJGnashTransaction(GnuCashToJGnashContentHandler contentHandler, List<Transaction> jGnashTransactions) {
        for (SplitEntry splitEntry : this.originalSplitsList) {
            if (!splitEntry.validateForJGnash(contentHandler)) {
                return true;
            }
        }
        
        ArrayList<SplitEntry> splitsList = new ArrayList<>();
        String memo = this.description;
        
        // Handle special splits...
        for (SplitEntry splitEntry : this.originalSplitsList) {
            if ("Split".equals(splitEntry.action)) {
                if (!handleSecuritySplitSplit(splitEntry, contentHandler, jGnashTransactions)) {
                    return false;
                }
                memo = null;
            }
            else {
                splitsList.add(splitEntry);
            }
        }
        
        if (splitsList.isEmpty()) {
            return true;
        }
        
        // Is this an investment transaction?
        ArrayList<SplitEntry> investmentSplitEntries = new ArrayList<>();
        for (SplitEntry splitEntry : splitsList) {
            if (splitEntry.jGnashSecurity != null) {
                investmentSplitEntries.add(splitEntry);
            }
        }
        
        if (!investmentSplitEntries.isEmpty()) {
            return handleJGnashInvestmentTransaction(splitsList, investmentSplitEntries, contentHandler, jGnashTransactions);
        }
        else if (splitsList.size() == 1) {
            SplitEntry splitEntry = splitsList.get(0);
            Transaction transaction = TransactionFactory.generateSingleEntryTransaction(splitEntry.jGnashAccount,
                    splitEntry.value.toBigDecimal(), this.datePosted.localDate, null, this.description, this.num);
            jGnashTransactions.add(transaction);
            return true;
        }
        
        
        final Transaction transaction = new Transaction();
        
        if (memo != null) {
            transaction.setMemo(memo);
        }
        if (this.num != null) {
            transaction.setNumber(this.num);
        }
        
        transaction.setDate(this.datePosted.localDate);

        if (!generateJGnashSplitTransactionEntries(splitsList, contentHandler, transaction)) {
            return false;
        }
        
        jGnashTransactions.add(transaction);
    	return true;
    }
    

    protected boolean handleSecuritySplitSplit(SplitEntry splitEntry, GnuCashToJGnashContentHandler contentHandler, 
    		List<Transaction> jGnashTransactions) {
        SecurityNode securityNode = splitEntry.jGnashSecurity;
        Account account = splitEntry.jGnashAccount;
        BigDecimal value = splitEntry.value.toBigDecimal();
        BigDecimal quantity = splitEntry.quantity.toBigDecimal();

        InvestmentTransaction transaction;
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            String memo = this.description;
            if (memo.equals("Stock Split")) {
                memo = "Stock Merge";
            }
            value = value.negate();
            quantity = quantity.negate();
            transaction = TransactionFactory.generateMergeXTransaction(account, securityNode,
                    value, quantity, this.datePosted.localDate, memo);
        }
        else {
            transaction = TransactionFactory.generateSplitXTransaction(account, securityNode,
                    value, quantity, this.datePosted.localDate, this.description);
        }
        
        jGnashTransactions.add(transaction);
        return true;
    }
    
    
    protected boolean handleJGnashInvestmentTransaction(List<SplitEntry> splitsList, List<SplitEntry> investmentSplitEntries, 
            GnuCashToJGnashContentHandler contentHandler, List<Transaction> jGnashTransactions) {
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
            
            for (SplitEntry splitEntry : splitsList) {
                if (splitEntry == securitySplitEntry) {
                    continue;
                }
                
                Account jGnashAccount = splitEntry.jGnashAccount;
                if (jGnashAccount != null) {
                    switch (jGnashAccount.getAccountType()) {
                    case ASSET:
                    case BANK:
                    case CASH:
                    case CHECKING:
                    case INVEST:
                    case MUTUAL :
                    case INCOME :
                    case EQUITY :
                        if (account != null) {
                            recordSkippingTransaction(splitsList, "Message.Warning.MultipleInvestmentTransactionAccounts");
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
                        recordSkippingTransaction(splitsList, "Message.Warning.UnsupportedInvestmentSplitAccount", 
                                jGnashAccount.getName(), jGnashAccount.getAccountType());
                        return false;
                    
                    }
                }
                
                if (jGnashAccount == null) {
                    recordSkippingTransaction(splitsList, "Message.Warning.InvestmentTransactionAccountMissing",
                            splitEntry.account.id);
                    return false;
                }
            }
            
            if (account == null) {
                if (!feeSplitEntries.isEmpty()) {
                    accountSplitEntry = feeSplitEntries.get(0);
                    account = accountSplitEntry.jGnashAccount;
                    feeSplitEntries.clear();
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
                                securityNode, amount, amount.negate(), BigDecimal.ZERO, this.datePosted.localDate, this.description);
                        jGnashTransactions.add(transaction);
                        
                        // The dividend is now in the investment account, we're buying from there...
                        account = investmentAccount;
                    }
                    
                    transaction = TransactionFactory.generateBuyXTransaction(account, investmentAccount, securityNode, price, quantity, exchangeRate, 
                            this.datePosted.localDate, this.description, fees);
                }
                jGnashTransactions.add(transaction);
                return true;
            }
        }
        else if (investmentSplitEntries.size() == 2) {
            // Some special case transactions I had, probably hacks, or results of importing Quicken into GnuCash.
            SplitEntry splitEntryA = investmentSplitEntries.get(0);
            SplitEntry splitEntryB = investmentSplitEntries.get(1);
            if (splitEntryA.account.id.equals(splitEntryB.account.id)) {
                BigDecimal valueA = splitEntryA.value.toBigDecimal();
                BigDecimal quantityA = splitEntryA.quantity.toBigDecimal();
                BigDecimal valueB = splitEntryB.value.toBigDecimal();
                BigDecimal quantityB = splitEntryB.quantity.toBigDecimal();
                if (valueA.equals(BigDecimal.ZERO) && valueB.equals(BigDecimal.ZERO)
                 && !quantityA.equals(BigDecimal.ZERO) && !quantityB.equals(BigDecimal.ZERO)) {
                    if (!quantityA.equals(quantityB)) {
                        // Most likely a stock split. Since we don't have a value, we'll just use 1.
                        Account account = contentHandler.jGnashAccounts.get(splitEntryA.account.id);
                        BigDecimal quantity = quantityA.add(quantityB);
                        transaction = TransactionFactory.generateSplitXTransaction(account, splitEntryA.jGnashSecurity,
                                BigDecimal.ONE, quantity, this.datePosted.localDate, this.description);
                        jGnashTransactions.add(transaction);
                        return true;
                    }
                }
            }
        }
        
        recordSkippingTransaction(splitsList, "Message.Warning.UnsupportedInvestmentTransaction");
        return false;
    }
    
    void recordSkippingTransaction(List<SplitEntry> splitsList, String key, Object ...arguments) {
        String description = "";
        String separator = "";
        for (SplitEntry splitEntry : splitsList) {
            if (splitEntry.jGnashSecurity != null) {
                description += separator + "[" + splitEntry.jGnashSecurity.getSymbol() + "]";
            }
            else {
                description += separator + splitEntry.jGnashAccount.getName();
            }
            separator = ";";
            description += " V=" + splitEntry.value.toBigDecimal() + " Q=" + splitEntry.quantity.toBigDecimal() + " ";
        }
        
        switch (arguments.length) {
            case 0 :
            default :
                contentHandler.recordWarning(this, key, this.datePosted.toDateString(), description);
                break;
            case 1 :
                contentHandler.recordWarning(this, key, this.datePosted.toDateString(), description, arguments[0]);
                break;
            case 2 :
                contentHandler.recordWarning(this, key, this.datePosted.toDateString(), description, arguments[0], arguments[1]);
                break;
            case 3 :
                contentHandler.recordWarning(this, key, this.datePosted.toDateString(), description, arguments[0], arguments[1], arguments[2]);
                break;
            case 4:
                contentHandler.recordWarning(this, key, this.datePosted.toDateString(), description, arguments[0], arguments[1], arguments[3]);
                break;
        }
    }

    
    final static SplitEntry assignEntryIfNull(SplitEntry refEntry, SplitEntry newEntry) {
        return (refEntry == null) ? newEntry : refEntry;
    }
    
    
    protected boolean generateJGnashSplitTransactionEntries(List<SplitEntry> splitsList, 
            GnuCashToJGnashContentHandler contentHandler, Transaction transaction) {
        
        // Look for an account to serve as the main account. This will receive all the credits, and disburse all the debits.
        SplitEntry masterSplitEntry = null;
        
        // Look for a bank, cash, or checking account first, these in case we don't find the others.
        SplitEntry assetSplitEntry = null;
        SplitEntry investSplitEntry = null;
        SplitEntry creditSplitEntry = null;
        SplitEntry simpleInvestSplitEntry = null;
        SplitEntry incomeSplitEntry = null;
        SplitEntry expenseSplitEntry = null;
        
        for (SplitEntry splitEntry : splitsList) {
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
            contentHandler.recordWarning(this, "Message.Warning.SplitNoSupportedAccounts");
            return false;
        }
        
        // We have our master account.
        // Add all the credits to it and remove all the debits...
        for (SplitEntry splitEntry : splitsList) {
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
        TransactionEntry transactionEntry = new TransactionEntry();
        
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
