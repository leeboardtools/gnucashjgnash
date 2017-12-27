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

import gnucashjgnash.NoticeTree;
import gnucashjgnash.NoticeTree.Source;

/**
 * The base class for all GnuCash parsed entries, primarily provides support for managing tracking warnings.
 * @author albert
 *
 */
public abstract class ParsedEntry implements NoticeTree.Source {
	final GnuCashToJGnashContentHandler contentHandler;
	protected Source parentSource;
	int lineNumber;
	int columnNumber;
	String title;
	String description;
	
	protected ParsedEntry(GnuCashToJGnashContentHandler contentHandler) {
		this.contentHandler = contentHandler;
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
	

	/* (non-Javadoc)
	 * @see gnucashjgnash.NoticeTree.Source#getParentSource()
	 */
	@Override
	public Source getParentSource() {
		return this.parentSource;
	}

	/* (non-Javadoc)
	 * @see gnucashjgnash.NoticeTree.Source#getSourceTitle()
	 */
	@Override
	public String getSourceTitle() {
		if (this.title == null ) {
			this.title = getIndentifyingText(this.contentHandler);
			if (this.title == null) {
				this.title = toString();
			}
		}
		return this.title;
	}

	/* (non-Javadoc)
	 * @see gnucashjgnash.NoticeTree.Source#getSourceDescription()
	 */
	@Override
	public String getSourceDescription() {
		if (this.description == null) {
			String uniqueId = this.getUniqueId();
			if ((uniqueId != null) && !uniqueId.isEmpty()) {
				this.description = uniqueId;
			}
		}
		return this.description;
	}
}
