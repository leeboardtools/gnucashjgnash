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

/**
 * The base class for all GnuCash parsed entries, primarily provides support for managing tracking warnings.
 * @author albert
 *
 */
public abstract class ParsedEntry {
	protected ParsedEntry parentEntry;
	int lineNumber;
	int columnNumber;
	
	protected ParsedEntry(GnuCashToJGnashContentHandler contentHandler) {
		if ((contentHandler != null) && (contentHandler.documentLocator != null)) {
			this.lineNumber = contentHandler.documentLocator.getLineNumber();
			this.columnNumber = contentHandler.documentLocator.getColumnNumber();
		}
		else {
			this.lineNumber = -1;
			this.columnNumber = -1;
		}
	}
	
	void updateLocatorInfo(GnuCashToJGnashContentHandler contentHandler) {
		if ((contentHandler != null) && (contentHandler.documentLocator != null)) {
			this.lineNumber = contentHandler.documentLocator.getLineNumber();
			this.columnNumber = contentHandler.documentLocator.getColumnNumber();
		}
		else {
			this.lineNumber = -1;
			this.columnNumber = -1;
		}
	}
	
	
	public ParsedEntry getParentParsedEntry(GnuCashToJGnashContentHandler contentHandler) {
		return parentEntry;
	}
	
	
	
	/**
	 * Retrieves the localized message text identifying the entry, what's displayed in the
	 * warning tree.
	 * @return	The text, <code>null</code> if not supported.
	 */
	public abstract String getIndentifyingText(GnuCashToJGnashContentHandler contentHandler);
	
	/**
	 * Retrieves the optional unique id of the entry.
	 * @return	The unique id string, <code>null</code> if not supported.
	 */
	public String getUniqueId() {
		return null;
	}
}
