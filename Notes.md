# Other Ideas for jGnash Plugins
- Accounts that displays number of shares.
- Graph that lets you see account balances at an interactive date slider. Watch your income/debt grow over time...


# Next Up
- Add display of warnings.

Warnings displayed in a tree view.
Tree items represent ParsedEntry objects, except leafs, which are the actual message.
The ParseEntry objects need to be able to figure out what to display.
Need a unique id for each ParsedEntry object so we can track them in a master list.
When a message is recorded, look for the first ParsedEntry object going up the parent chain.
Then walk back down adding TreeItems and adding the ParsedEntry objects to the master list.
Finally get to the message, add that.

To use TreeItem, will need a class that handles both the ParsedEntry and the final message.