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

import jgnash.engine.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;
import gnucashjgnash.imports.CommodityEntry.CommodityRef;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;

class AccountImportEntry {
    private static final Logger LOG = Logger.getLogger(AccountImportEntry.class.getName());

    String name;
    IdEntry id = new IdEntry();
    String type;
	CommodityRef commodityRef = new CommodityRef();
    String code;
    String description;
    IdEntry parentId = new IdEntry();
    Map<String, SlotEntry> slots = new HashMap<>();

    Map<String, AccountImportEntry> childAccountEntries = new HashMap<>();

    // Not yet supported:
    // commodity-scu
    // non-standard-scu

    static class AccountStateHandler extends GnuCashToJGnashContentHandler.AbstractVersionStateHandler {
        AccountImportEntry accountEntry = new AccountImportEntry();

        AccountStateHandler(GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName, null);
        }

        @Override
        protected boolean validateVersion(String version) {
            if (!version.equals("2.0.0")) {
                this.contentHandler.recordError("Message.Parse.XMLAccountVersionUnsupported", version);
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
			case "act:name" :
				return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
		                @Override
		                protected void setAccountEntryField(AccountImportEntry accountEntry, String value) {
		                    accountEntry.name = value;
		                }
		            });

			case "act:id" :
	            return new IdEntry.IdStateHandler(this.accountEntry.id, this.contentHandler, this, qName);

			case "act:type" :
				return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
		                @Override
		                protected void setAccountEntryField(AccountImportEntry accountEntry, String value) {
		                    accountEntry.type = value;
		                }
		            });

			case "act:description" :
				return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
		                @Override
		                protected void setAccountEntryField(AccountImportEntry accountEntry, String value) {
		                    accountEntry.description = value;
		                }
		            });
				
			case "act:slots" :
                return new SlotEntry.SlotsStateHandler(this.accountEntry.slots, this.contentHandler, this, qName);
                
			case "act:parent" :
				return new IdEntry.IdStateHandler(this.accountEntry.parentId, this.contentHandler, this, qName);
				
			case "act:commodity" :
				return new CommodityEntry.CommodityRefStateHandler(this.accountEntry.commodityRef, this.contentHandler, this, qName);
				
			case "act:commodity-scu" :
				// Smallest currency unit
				return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
	                @Override
	                protected void setAccountEntryField(AccountImportEntry accountEntry, String value) {
	                }
	            });

			}

			return super.getStateHandlerForElement(qName);
		}

        @Override
        protected void endState() {
            super.endState();

            if (this.ignoreChildElements) {
                return;
            }

            if (!this.accountEntry.id.validateGUIDParse(this, "act:id")) {
                return;
            }
            if (this.accountEntry.name == null) {
                recordWarning("AccountMissingName", "Message.Parse.XMLAccountMissingElement", this.accountEntry.id, "act:name");
                return;
            }
            if (this.accountEntry.type == null) {
                recordWarning("AccountMissingType", "Message.Parse.XMLAccountMissingElement", this.accountEntry.name, "act:type");
                return;
            }

            if (this.contentHandler.accountImportEntries.containsKey(this.accountEntry.id.id)) {
                recordWarning("MultipleAccountEntries", "Message.Parse.XMLMultipleAccountEntries", this.accountEntry.name, this.accountEntry.id);
            }
            this.contentHandler.accountImportEntries.put(this.accountEntry.id.id, this.accountEntry);
            LOG.info("Added account entry " + this.accountEntry.name);
        }
    }

    static abstract class SimpleDataSetterImpl extends GnuCashToJGnashContentHandler.AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            AccountStateHandler parentStateHandler = (AccountStateHandler)stateHandler.parentStateHandler;
            setAccountEntryField(parentStateHandler.accountEntry, characters);
        }

        protected abstract void setAccountEntryField(AccountImportEntry accountEntry, String value);
    }


    /**
     * Called by GnuCashToJGnashContentHandler to build the child AccountImportEntry objects for this entry.
     * @param accountEntries
     */
    public void gatherChildAccountEntries(Map<String, AccountImportEntry> accountEntries) {
        for (Iterator<Map.Entry<String, AccountImportEntry>> iterator = accountEntries.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, AccountImportEntry> entry = iterator.next();
            AccountImportEntry accountEntry = entry.getValue();
            if ((accountEntry.parentId.id != null) && accountEntry.parentId.equals(this.id)) {
                this.childAccountEntries.put(entry.getKey(), accountEntry);
                iterator.remove();
            }
        }

        for (Map.Entry<String, AccountImportEntry> entry : this.childAccountEntries.entrySet()) {
            entry.getValue().gatherChildAccountEntries(accountEntries);
        }
    }


    /**
     * Called by GnuCashToJGnashContentHandler to generate the jGnash accounts for this and all the child AccountImportEntry objects.
     * @param contentHandler
     * @param engine
     * @param jGnashAccountEntries
     * @param accountIdsToIgnore
     * @return
     */
    public boolean createJGnashAccounts(GnuCashToJGnashContentHandler contentHandler, Engine engine,
                                 Map<String, Account> jGnashAccountEntries, Set<String> accountIdsToIgnore) {
        // Can we generate the account?
        AccountType accountType = null;

        switch (this.type) {
            case "NONE" :
                accountIdsToIgnore.add(this.id.id);
                return true;

            case "BANK" :
                accountType = detectInvestmentAccount(AccountType.BANK);
                break;

            case "CASH" :
                accountType = detectInvestmentAccount(AccountType.CASH);
                break;

            case "CREDIT":
                accountType = AccountType.CREDIT;
                break;

            case "ASSET":
                accountType = detectInvestmentAccount(AccountType.ASSET);
                break;

            case "LIABILITY":
                accountType = AccountType.LIABILITY;
                break;

            case "STOCK":
                return handleStockAccount(contentHandler, engine, jGnashAccountEntries, accountIdsToIgnore);

            case "MUTUAL":
            	if (isTreatMutualFundAsStock(contentHandler)) {
                    return handleStockAccount(contentHandler, engine, jGnashAccountEntries, accountIdsToIgnore);
            	}
                accountType = AccountType.MUTUAL;
                break;

            case "CURRENCY":
                break;

            case "INCOME":
                accountType = AccountType.INCOME;
                break;

            case "EXPENSE":
                accountType = AccountType.EXPENSE;
                break;

            case "EQUITY":
                accountType = AccountType.EQUITY;
                break;

            case "RECEIVABLE":
                contentHandler.recordWarning("AccountTypeUnsupported_" + this.type, "Message.Warning.UnsupportedAccountType", this.type);
                accountIdsToIgnore.add(this.id.id);
                return true;

            case "PAYABLE":
                contentHandler.recordWarning("AccountTypeUnsupported_" + this.type, "Message.Warning.UnsupportedAccountType", this.type);
                accountIdsToIgnore.add(this.id.id);
                return true;

            case "ROOT":
                accountType = AccountType.ROOT;
                break;

            case "TRADING":
                contentHandler.recordWarning("AccountTypeUnsupported_" + this.type, "Message.Warning.UnsupportedAccountType", this.type);
                accountIdsToIgnore.add(this.id.id);
                return true;

            case "CHECKING":
                accountType = detectInvestmentAccount(AccountType.CHECKING);
                break;

            case "SAVINGS":
                accountType = detectInvestmentAccount(AccountType.CHECKING);
                if (accountType == AccountType.CHECKING) {
                	contentHandler.recordWarning("SavingsAsChecking", "Message.Info.AccountMapped", this.type, "CHECKING");
                }
                break;

            case "MONEYMRKT":
                accountType = AccountType.MONEYMKRT;
                break;

            case "CREDITLINE":
                contentHandler.recordWarning("AccountTypeUnsupported_" + this.type, "Message.Warning.UnsupportedAccountType", this.type);
                return true;

        }

        if (accountType == AccountType.ROOT) {
            jGnashAccountEntries.put(this.id.id, engine.getRootAccount());
        }
        else {
            Account parentAccount = null;
            if (this.parentId.id != null) {
                parentAccount = jGnashAccountEntries.get(this.parentId.id);
            }

            CurrencyNode currencyNode = getCurrencyNode(engine);

            Account newAccount = new Account(accountType, currencyNode);
            if (this.name != null) {
                newAccount.setName(this.name);
            }
            if (this.description != null) {
                newAccount.setDescription(this.description);
            }
            if (this.code != null) {
                try {
                    int code = Integer.parseInt(this.code);
                    newAccount.setAccountCode(code);
                }
                catch (NumberFormatException e) {
                    contentHandler.recordWarning("NonIntegerAccountCode_" + this.name, "Message.Warning.NonIntegerAccountCode", this.name, this.code);
                }
            }
            
            if (accountType == AccountType.MUTUAL) {
            	if (!setupMutualFundAccount(newAccount, contentHandler, engine,
                        jGnashAccountEntries, accountIdsToIgnore)) {
            		return false;
            	}
            }

            if ("true".equals(SlotEntry.getStringSlotValue(this.slots, "placeholder", null))) {
                newAccount.setPlaceHolder(true);
            }
            if ("true".equals(SlotEntry.getStringSlotValue(this.slots, "hidden", null))) {
                newAccount.setVisible(false);
            }

            engine.addAccount(parentAccount, newAccount);

            jGnashAccountEntries.put(this.id.id, newAccount);
        }

        for (Map.Entry<String, AccountImportEntry> entry : this.childAccountEntries.entrySet()) {
            if (!entry.getValue().createJGnashAccounts(contentHandler, engine, jGnashAccountEntries, accountIdsToIgnore)) {
                return false;
            }
        }

        return true;
    }

    boolean handleStockAccount(GnuCashToJGnashContentHandler contentHandler, Engine engine,
                               Map<String, Account> jGnashAccountEntries, Set<String> accountIdsToIgnore) {
    	// We need to be able to refer to the stock account's security node from transactions.
    	SecurityNode securityNode = contentHandler.jGnashSecurities.get(this.commodityRef.id);
    	if (securityNode == null) {
    		CurrencyNode currencyNode = contentHandler.jGnashCurrencies.get(this.commodityRef.id);
    		if (currencyNode == null) {
	    		contentHandler.recordWarning("StockAccountSecurityNotFound_" + this.commodityRef.id, "Message.Warning.StockAccountSecurityNotFound", 
	    				this.id.id, this.commodityRef.id);
	    		return false;
    		}
    	}
    	
    	contentHandler.jGnashSecuritiesByStockAccountId.put(this.id.id, securityNode);
    	
    	Account jGnashParentAccount = contentHandler.jGnashAccounts.get(this.parentId.id);
    	if (jGnashParentAccount != null) {
    		jGnashParentAccount.addSecurity(securityNode);
    	}
    			
        return true;
    }
    
    
    boolean setupMutualFundAccount(Account account, GnuCashToJGnashContentHandler contentHandler, Engine engine,
                               Map<String, Account> jGnashAccountEntries, Set<String> accountIdsToIgnore) {
    	// We need to be able to refer to the stock account's security node from transactions.
    	SecurityNode securityNode = contentHandler.jGnashSecurities.get(this.commodityRef.id);
    	if (securityNode == null) {
    		CurrencyNode currencyNode = contentHandler.jGnashCurrencies.get(this.commodityRef.id);
    		if (currencyNode == null) {
	    		contentHandler.recordWarning("MutualFundAccountSecurityNotFound_" + this.commodityRef.id, "Message.Warning.MutualFundAccountSecurityNotFound", 
	    				this.id.id, this.commodityRef.id);
	    		return false;
    		}
    	}
    	
    	contentHandler.jGnashSecuritiesByStockAccountId.put(this.id.id, securityNode);
    	account.addSecurity(securityNode);
    	return true;
    }
    
    
    AccountType detectInvestmentAccount(AccountType accountType) {
    	// If the account has stock accounts as kids, then it's an investment account.
    	for (Map.Entry<String, AccountImportEntry> entry : this.childAccountEntries.entrySet()) {
    		if (entry.getValue().type.equals("STOCK")) {
    			return AccountType.INVEST;
    		}
    	}
    	return accountType;
    }
    
    
    boolean isTreatMutualFundAsStock(GnuCashToJGnashContentHandler contentHandler) {
    	Account jGnashParentAccount = contentHandler.jGnashAccounts.get(this.parentId.id);
    	if ((jGnashParentAccount != null) && (jGnashParentAccount.getAccountType() == AccountType.INVEST)) {
    		return true;
    	}
    	return false;
    }
    

    CurrencyNode getCurrencyNode(Engine engine) {
        CurrencyNode currencyNode = engine.getDefaultCurrency();
        return currencyNode;
    }
}
