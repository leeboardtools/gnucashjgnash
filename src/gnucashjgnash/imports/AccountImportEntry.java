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
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;

class AccountImportEntry {
    private static final Logger LOG = Logger.getLogger(AccountImportEntry.class.getName());

    String name;
    IdEntry id = new IdEntry();
    String type;
    String commoditySpace;
    String commodityId;
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



    void gatherChildAccountEntries(Map<String, AccountImportEntry> accountEntries) {
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


    boolean handleStockAccount(GnuCashToJGnashContentHandler contentHandler, Engine engine,
                               Map<String, Account> jGnashAccountEntries, Set<String> accountIdsToIgnore) {
    	// TODO AccountImportEntry.handleStockAccount()
        CurrencyNode currencyNode = getCurrencyNode(engine);
        SecurityNode securityNode = new SecurityNode(currencyNode);

        if (this.description != null) {
            securityNode.setDescription(this.description);
        }
/*        node.setDescription(descriptionTextField.getText());
        node.setScale(scaleTextField.getInteger().byteValue());
        node.setSymbol(symbolTextField.getText().trim());
        node.setISIN(cusipTextField.getText());
        node.setQuoteSource(quoteSourceComboBox.getValue());
*/        return true;
    }

    boolean createJGnashAccounts(GnuCashToJGnashContentHandler contentHandler, Engine engine,
                                 Map<String, Account> jGnashAccountEntries, Set<String> accountIdsToIgnore) {
        // Can we generate the account?
        AccountType accountType = null;
/*
    //ASSET(ResourceUtils.getString("AccountType.Asset"), AccountGroup.ASSET, AccountProxy.class, true),
    //BANK(ResourceUtils.getString("AccountType.Bank"), AccountGroup.ASSET, AccountProxy.class, true),
    //CASH(ResourceUtils.getString("AccountType.Cash"), AccountGroup.ASSET, AccountProxy.class, true),
    //CHECKING(ResourceUtils.getString("AccountType.Checking"), AccountGroup.ASSET, AccountProxy.class, true),
    //CREDIT(ResourceUtils.getString("AccountType.Credit"), AccountGroup.LIABILITY, AccountProxy.class, true),
    //EQUITY(ResourceUtils.getString("AccountType.Equity"), AccountGroup.EQUITY, AccountProxy.class, true),
    //EXPENSE(ResourceUtils.getString("AccountType.Expense"), AccountGroup.EXPENSE, AccountProxy.class, true),
    //INCOME(ResourceUtils.getString("AccountType.Income"), AccountGroup.INCOME, AccountProxy.class, true),
    INVEST(ResourceUtils.getString("AccountType.Investment"), AccountGroup.INVEST, InvestmentAccountProxy.class, false),
    SIMPLEINVEST(ResourceUtils.getString("AccountType.SimpleInvestment"), AccountGroup.SIMPLEINVEST, AccountProxy.class, true),
    //LIABILITY(ResourceUtils.getString("AccountType.Liability"), AccountGroup.LIABILITY, AccountProxy.class, true),
    //MONEYMKRT(ResourceUtils.getString("AccountType.MoneyMarket"), AccountGroup.ASSET, AccountProxy.class, true),
    //MUTUAL(ResourceUtils.getString("AccountType.Mutual"), AccountGroup.INVEST, InvestmentAccountProxy.class, false),
    //ROOT(ResourceUtils.getString("AccountType.Root"), AccountGroup.ROOT, AccountProxy.class, true);

 */
        switch (this.type) {
            case "NONE" :
                accountIdsToIgnore.add(this.id.id);
                return true;

            case "BANK" :
                accountType = AccountType.BANK;
                break;

            case "CASH" :
                accountType = AccountType.CASH;
                break;

            case "CREDIT":
                accountType = AccountType.CREDIT;
                break;

            case "ASSET":
                accountType = AccountType.ASSET;
                break;

            case "LIABILITY":
                accountType = AccountType.LIABILITY;
                break;

            case "STOCK":
                return handleStockAccount(contentHandler, engine, jGnashAccountEntries, accountIdsToIgnore);

            case "MUTUAL":
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
                accountType = AccountType.CHECKING;
                break;

            case "SAVINGS":
                contentHandler.recordWarning("SavingsAsChecking", "Message.Info.AccountMapped", this.type, "CHECKING");
                accountType = AccountType.CHECKING;
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

    CurrencyNode getCurrencyNode(Engine engine) {
        CurrencyNode currencyNode = engine.getDefaultCurrency();
        return currencyNode;
    }
}
