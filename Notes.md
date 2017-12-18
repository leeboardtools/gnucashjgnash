# To Think About
- jGnash has the following accounts
	- Asset
	- Bank
	- Cash
	- Checking
	- Credit
	- Equity
	- Expense
	- Income
	- Investment
	- Liability
	- Money Market
	- Mutual Fund
	- Simple Investment
	- Root
	
 while GnuCash has the following:
 	- Bank				-> Bank
 	- Cash				-> Cash
 	- Credit			-> Credit
 	- Asset				-> Asset
 	- Liability			-> Liability
 	- Stock
 	- Mutual			-> Mutual Fund
 	- Currency
 	- Income			-> Income
 	- Expense			-> Expense
 	- Equity			-> Equity
 	- Receivable		X
 	- Payable			X
 	- Root				-> Root
 	- Trading			X
 	- Checking			-> Checking
 	- Savings			-> Bank
 	- Moneymrkt			-> Money Market
 	- Creditline		X
 	
 what should be the mapping?
 
 
 # Ideas
 - Decimate the historical quotes to say the last one for each month (or week) for anything over a year old.
 
 
 # Other Stuff
 More than 2 entries in investment split scenarios:
 - GnuCash Split, stock converted to cash-in lieu due to split. (PBW, 2017-10-24)
  This was a hack, I think.
	- Stock, Amount sold, Total Shares
	- Brokerage, Cash in lieu, Buy
	- Income, Cash in lieu, Sell

<gnc:transaction version="2.0.0">
  <trn:id type="guid">587bcd6dba83961b434bb93b5b799dfa</trn:id>
  <trn:currency>
    <cmdty:space>ISO4217</cmdty:space>
    <cmdty:id>USD</cmdty:id>
  </trn:currency>
  <trn:date-posted>
    <ts:date>2017-10-24 06:59:00 -0400</ts:date>
  </trn:date-posted>
  <trn:date-entered>
    <ts:date>2017-10-24 13:04:34 -0400</ts:date>
  </trn:date-entered>
  <trn:description>Stock Split</trn:description>
  <trn:slots>
    <slot>
      <slot:key>date-posted</slot:key>
      <slot:value type="gdate">
        <gdate>2017-10-24</gdate>
      </slot:value>
    </slot>
  </trn:slots>
  <trn:splits>
    <trn:split>
      <split:id type="guid">19138adda2f46822457ad32f731f4d6d</split:id>
      <split:action>Split</split:action>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>0/100</split:value>
      <split:quantity>-5624238/10000</split:quantity>
      <split:account type="guid">e520795c36ef1d5a289c5cd0a39e201a</split:account>
      <split:slots>
        <slot>
          <slot:key>split-type</slot:key>
          <slot:value type="string">stock-split</slot:value>
        </slot>
      </split:slots>
    </trn:split>
    <trn:split>
      <split:id type="guid">9cc917c0328e8029388ef32a37ef7401</split:id>
      <split:memo>Cash In Lieu</split:memo>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>1178/100</split:value>
      <split:quantity>1178/100</split:quantity>
      <split:account type="guid">648176f40fd7fbccc4fd79102ace03f6</split:account>
    </trn:split>
    <trn:split>
      <split:id type="guid">8c20a56f56c00754b71d9b592fe696e8</split:id>
      <split:memo>Cash In Lieu</split:memo>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>-1178/100</split:value>
      <split:quantity>-1178/100</split:quantity>
      <split:account type="guid">81de6b71f9c5ecd06bb6275d621ceea6</split:account>
    </trn:split>
  </trn:splits>

	
 
 - GnuCash Split, Opening balance (IRA 1994-04-08)
 	- Stock, Deposit
 	- Expenses, Commission, Deposit
 	- Brokerage, Withdrawal
 	
<gnc:transaction version="2.0.0">
  <trn:id type="guid">cced8dce24f1eacaeac169aa2dc2cd07</trn:id>
  <trn:currency>
    <cmdty:space>ISO4217</cmdty:space>
    <cmdty:id>USD</cmdty:id>
  </trn:currency>
  <trn:date-posted>
    <ts:date>1994-04-08 06:59:00 -0400</ts:date>
  </trn:date-posted>
  <trn:date-entered>
    <ts:date>2012-12-31 12:25:06 -0500</ts:date>
  </trn:date-entered>
  <trn:description></trn:description>
  <trn:slots>
    <slot>
      <slot:key>date-posted</slot:key>
      <slot:value type="gdate">
        <gdate>1994-04-08</gdate>
      </slot:value>
    </slot>
  </trn:slots>
  <trn:splits>
    <trn:split>
      <split:id type="guid">52322e0e972c345349d0634f2b4102dd</split:id>
      <split:reconciled-state>y</split:reconciled-state>
      <split:value>198640/100</split:value>
      <split:quantity>19100000/100000</split:quantity>
      <split:account type="guid">33553523ba9a27c436f511928c0006b4</split:account>
    </trn:split>
    <trn:split>
      <split:id type="guid">24dd23b7bfd20ce9a65ab193a257245b</split:id>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>485/100</split:value>
      <split:quantity>485/100</split:quantity>
      <split:account type="guid">5bfa386e07993c5cb346b95b4d10ae51</split:account>
    </trn:split>
    <trn:split>
      <split:id type="guid">857932e23e0c0db216422d3f943ee047</split:id>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>-199125/100</split:value>
      <split:quantity>-199125/100</split:quantity>
      <split:account type="guid">648176f40fd7fbccc4fd79102ace03f6</split:account>
    </trn:split>
  </trn:splits>
</gnc:transaction>

 	
 	
 - GnuCash Split, Buy (Roth IRA 1999-08-27)
 	- Stock, Deposit
 	- Expenses, Commission, Deposit
 	- Brokerage, Withdrawal
 	
<gnc:transaction version="2.0.0">
  <trn:id type="guid">96882c399b6f351c93bf329a64376a00</trn:id>
  <trn:currency>
    <cmdty:space>ISO4217</cmdty:space>
    <cmdty:id>USD</cmdty:id>
  </trn:currency>
  <trn:date-posted>
    <ts:date>1999-08-27 06:59:00 -0400</ts:date>
  </trn:date-posted>
  <trn:date-entered>
    <ts:date>2012-12-31 11:25:09 -0500</ts:date>
  </trn:date-entered>
  <trn:description></trn:description>
  <trn:slots>
    <slot>
      <slot:key>date-posted</slot:key>
      <slot:value type="gdate">
        <gdate>1999-08-27</gdate>
      </slot:value>
    </slot>
  </trn:slots>
  <trn:splits>
    <trn:split>
      <split:id type="guid">34078bb6a762b83d0cbb3df3852719c3</split:id>
      <split:reconciled-state>y</split:reconciled-state>
      <split:value>186063/100</split:value>
      <split:quantity>650000/10000</split:quantity>
      <split:account type="guid">09a45c78d0d744529756e61bde0713fe</split:account>
    </trn:split>
    <trn:split>
      <split:id type="guid">151ad0f6a9917ae247f26dd98eaf87b5</split:id>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>2995/100</split:value>
      <split:quantity>2995/100</split:quantity>
      <split:account type="guid">cef04db38d93575b7a1134ad988bea22</split:account>
    </trn:split>
    <trn:split>
      <split:id type="guid">cf669617f3ff8f1ec20b1c1d60cd97bf</split:id>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>-189058/100</split:value>
      <split:quantity>-189058/100</split:quantity>
      <split:account type="guid">d13a8349e26c8f74c8cc0b36db327be0</split:account>
    </trn:split>
  </trn:splits>
</gnc:transaction>

 	
 	
 -  GnuCash Split, Buy (IRA 1996-08-14)
 	- Stock, Deposit
 	- Expenses, Commission, Deposit
 	- Brokerage, Withdrawal
 
 - GnuCash Split, Buy (Roth IRA 1999-08-27)
 	- Stock, Deposit
 	- Expenses, Commission, Deposit
 	- Brokerage, Withdrawal
  
 - GnuCash Split, Buy (IRA 1999-05-24)
 	- Stock, Deposit
 	- Expenses, Commission, Deposit
 	- Brokerage, Withdrawal
  
 - GnuCash Split, Buy (Roth IRA 2001-02-28)
 	- Stock, Deposit
 	- Expenses, Commission, Deposit
 	- Brokerage, Withdrawal
  
 - GnuCash Split, Buy (Roth IRA 2000-02-25)
 	- Stock, Deposit
 	- Expenses, Commission, Deposit
 	- Brokerage, Withdrawal
 
 
 
 - WARNING: The split with id 019942ed1f0bf15a2fa051cc9854dd0d and date 2013-06-26 does not refer to a cash, checking, bank, asset, credit, or investment account, the transaction is being ignored.
 
 Reinvested Dividends
 
  <trn:splits>
    <trn:split>
      <split:id type="guid">9a92b23fb3be152b5f6366ea8d7d25f3</split:id>
      <split:action>Buy</split:action>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>63711/100</split:value>
      <split:quantity>262510/10000</split:quantity>
      <split:account type="guid">fa7cd664e8be2364473d20c4f0379182</split:account>
<!-- FMDCX Mutual Fund account -->
    </trn:split>
    <trn:split>
      <split:id type="guid">8590110af430b5e6dd50962a22f1bf94</split:id>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>-63711/100</split:value>
      <split:quantity>-63711/100</split:quantity>
      <split:account type="guid">03e14d95276fcefdb3d688c802580df0</split:account>
<!-- FMDCX Income account -->
    </trn:split>
    
    
    

- Transaction Examples:

-- Buy
<gnc:transaction version="2.0.0">
  <trn:id type="guid">a927ba4af9a47c1fc18f5afa61ac3c61</trn:id>
  <trn:currency>
    <cmdty:space>ISO4217</cmdty:space>
    <cmdty:id>USD</cmdty:id>
  </trn:currency>
  <trn:date-posted>
    <ts:date>2013-01-23 05:59:00 -0500</ts:date>
  </trn:date-posted>
  <trn:date-entered>
    <ts:date>2013-01-23 11:11:46 -0500</ts:date>
  </trn:date-entered>
  <trn:description>Bought SCHX</trn:description>
  <trn:splits>
    <trn:split>
      <split:id type="guid">0b84a3783d056aecab2b14bf1b0db0f8</split:id>
      <split:action>Buy</split:action>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>1079474/100</split:value>
      <split:quantity>3040000/10000</split:quantity>
      <split:account type="guid">462047ee6a7692f282dff18b4638105a</split:account>
    </trn:split>
    <trn:split>
      <split:id type="guid">80c28f26178f23d6b4dca181566d72b0</split:id>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>-1079474/100</split:value>
      <split:quantity>-1079474/100</split:quantity>
      <split:account type="guid">570952d1529bd0d45116516dcd53ad4d</split:account>
    </trn:split>
  </trn:splits>
</gnc:transaction>



-- Reinvested Dividend

<gnc:transaction version="2.0.0">
  <trn:id type="guid">a2360b50fdc183bba68b4afe3f6e87c5</trn:id>
  <trn:currency>
    <cmdty:space>ISO4217</cmdty:space>
    <cmdty:id>USD</cmdty:id>
  </trn:currency>
  <trn:date-posted>
    <ts:date>2013-03-25 06:59:00 -0400</ts:date>
  </trn:date-posted>
  <trn:date-entered>
    <ts:date>2013-05-07 21:42:32 -0400</ts:date>
  </trn:date-entered>
  <trn:description>Reinvested Dividend</trn:description>
  <trn:slots>
    <slot>
      <slot:key>date-posted</slot:key>
      <slot:value type="gdate">
        <gdate>2013-03-25</gdate>
      </slot:value>
    </slot>
  </trn:slots>
  <trn:splits>
    <trn:split>
      <split:id type="guid">cdd4419644ba068f081873634df8ff98</split:id>
      <split:action>Buy</split:action>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>4560/100</split:value>
      <split:quantity>12235/10000</split:quantity>
      <split:account type="guid">462047ee6a7692f282dff18b4638105a</split:account>
    </trn:split>
    <trn:split>
      <split:id type="guid">2ff7afaef67a343345f6e5cc709ed8e2</split:id>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>-4560/100</split:value>
      <split:quantity>-4560/100</split:quantity>
      <split:account type="guid">4fc1800bb940f99b684f2d50f648257b</split:account>
    </trn:split>
  </trn:splits>
</gnc:transaction>

Split 0: Stock account SCHX US Large Cap ETF
Split 1: Income accounty SCHX


-- Sell Transaction, no commission

<gnc:transaction version="2.0.0">
  <trn:id type="guid">3d550ae48bbf5fb6f5ba4d4dcacc8ba9</trn:id>
  <trn:currency>
    <cmdty:space>ISO4217</cmdty:space>
    <cmdty:id>USD</cmdty:id>
  </trn:currency>
  <trn:date-posted>
    <ts:date>2013-05-23 06:59:00 -0400</ts:date>
  </trn:date-posted>
  <trn:date-entered>
    <ts:date>2013-05-23 22:12:13 -0400</ts:date>
  </trn:date-entered>
  <trn:description>Sold SCHX</trn:description>
  <trn:splits>
    <trn:split>
      <split:id type="guid">16310ff6d8755fd91633972c9b20146e</split:id>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>878/100</split:value>
      <split:quantity>878/100</split:quantity>
      <split:account type="guid">570952d1529bd0d45116516dcd53ad4d</split:account>
    </trn:split>
    <trn:split>
      <split:id type="guid">04ead16306f6cd3eddc98eb00382ada9</split:id>
      <split:action>Sell</split:action>
      <split:reconciled-state>n</split:reconciled-state>
      <split:value>-878/100</split:value>
      <split:quantity>-2235/10000</split:quantity>
      <split:account type="guid">462047ee6a7692f282dff18b4638105a</split:account>
    </trn:split>
  </trn:splits>
</gnc:transaction>



- Problems
8e133224897beedaad0eae55aa70a562 - Opening Balances
e5d09d21db283970f4dbf2862b52b15e - LEXMX

Message.Warning.SplitNoSupportedAccounts
    2003-08-28  Sold 404 shares
        2542.20 - EQUITY Opening Balances
        -2542.20 - MUTUAL LEXMX

    1999-08-13  Bought $2500
        2500.00 - MUTUAL LEXMX
        -2500.00 - EQUITY Opening Balances
 	  
    2006-01-27  Sold 200 shares
        2038.00 - EQUITY Opening Balances
        -2038.00 - MUTUAL LEXMX


Skipping Investment
    2001-01-12 
        0 - MSFT
        0 - MSFT
            <gnc:transaction version="2.0.0">
              <trn:id type="guid">f9aac06875637b888ac38a5adc1020b8</trn:id>
              <trn:currency>
                <cmdty:space>ISO4217</cmdty:space>
                <cmdty:id>USD</cmdty:id>
              </trn:currency>
              <trn:date-posted>
                <ts:date>2000-01-12 05:59:00 -0500</ts:date>
              </trn:date-posted>
              <trn:date-entered>
                <ts:date>2012-12-31 12:25:07 -0500</ts:date>
              </trn:date-entered>
              <trn:description></trn:description>
              <trn:slots>
                <slot>
                  <slot:key>date-posted</slot:key>
                  <slot:value type="gdate">
                    <gdate>2000-01-12</gdate>
                  </slot:value>
                </slot>
              </trn:slots>
              <trn:splits>
                <trn:split>
                  <split:id type="guid">330dbf1ddde820e658e869ba37d8d198</split:id>
                  <split:reconciled-state>y</split:reconciled-state>
                  <split:value>0/100</split:value>
                  <split:quantity>5850000/100000</split:quantity>
                  <split:account type="guid">155b292f5b1f102bff446ae3d37fe8c7</split:account>
                </trn:split>
                <trn:split>
                  <split:id type="guid">21192ce89de9f4998b1da5e508d26e38</split:id>
                  <split:reconciled-state>n</split:reconciled-state>
                  <split:value>0/100</split:value>
                  <split:quantity>-13000000/100000</split:quantity>
                  <split:account type="guid">155b292f5b1f102bff446ae3d37fe8c7</split:account>
                </trn:split>
              </trn:splits>
            </gnc:transaction>
        

    2003-02-14
        0 - MSFT
        0 - MSFT
            <gnc:transaction version="2.0.0">
              <trn:id type="guid">06dec748d4a4910f8e23ced12e209f0d</trn:id>
              <trn:currency>
                <cmdty:space>ISO4217</cmdty:space>
                <cmdty:id>USD</cmdty:id>
              </trn:currency>
              <trn:date-posted>
                <ts:date>2003-02-14 05:59:00 -0500</ts:date>
              </trn:date-posted>
              <trn:date-entered>
                <ts:date>2012-12-31 12:25:07 -0500</ts:date>
              </trn:date-entered>
              <trn:description></trn:description>
              <trn:slots>
                <slot>
                  <slot:key>date-posted</slot:key>
                  <slot:value type="gdate">
                    <gdate>2003-02-14</gdate>
                  </slot:value>
                </slot>
              </trn:slots>
              <trn:splits>
                <trn:split>
                  <split:id type="guid">d5004e709003d92bc30082e1fd8e1800</split:id>
                  <split:reconciled-state>y</split:reconciled-state>
                  <split:value>0/100</split:value>
                  <split:quantity>11600000/100000</split:quantity>
                  <split:account type="guid">155b292f5b1f102bff446ae3d37fe8c7</split:account>
                </trn:split>
                <trn:split>
                  <split:id type="guid">2834cac311cf28df544f3f2c9e1292d4</split:id>
                  <split:reconciled-state>n</split:reconciled-state>
                  <split:value>0/100</split:value>
                  <split:quantity>-5800000/100000</split:quantity>
                  <split:account type="guid">155b292f5b1f102bff446ae3d37fe8c7</split:account>
                </trn:split>
              </trn:splits>
            </gnc:transaction>

            
    2010-03-01
        0 - ANLY
        0 - ANLY
            <gnc:transaction version="2.0.0">
              <trn:id type="guid">780bf63b0b9fa95366436a76d87b8281</trn:id>
              <trn:currency>
                <cmdty:space>ISO4217</cmdty:space>
                <cmdty:id>USD</cmdty:id>
              </trn:currency>
              <trn:date-posted>
                <ts:date>2010-03-01 05:59:00 -0500</ts:date>
              </trn:date-posted>
              <trn:date-entered>
                <ts:date>2012-12-31 11:25:10 -0500</ts:date>
              </trn:date-entered>
              <trn:description>from data feed</trn:description>
              <trn:slots>
                <slot>
                  <slot:key>date-posted</slot:key>
                  <slot:value type="gdate">
                    <gdate>2010-03-01</gdate>
                  </slot:value>
                </slot>
              </trn:slots>
              <trn:splits>
                <trn:split>
                  <split:id type="guid">7c83f60f449fad61165842a1f1f31dfa</split:id>
                  <split:reconciled-state>n</split:reconciled-state>
                  <split:value>0/100</split:value>
                  <split:quantity>316956/10000</split:quantity>
                  <split:account type="guid">1d5bf83497519f467940d997b1f74c48</split:account>
                </trn:split>
                <trn:split>
                  <split:id type="guid">369b6f4c1b91582a000c2f1edcf930c6</split:id>
                  <split:reconciled-state>n</split:reconciled-state>
                  <split:value>0/100</split:value>
                  <split:quantity>-1584781/10000</split:quantity>
                  <split:account type="guid">1d5bf83497519f467940d997b1f74c48</split:account>
                </trn:split>
              </trn:splits>
            </gnc:transaction>
            

