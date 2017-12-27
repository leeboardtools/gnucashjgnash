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
package gnucashjgnash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Manages textual notices in a tree hierarchy.
 * @author albert
 *
 */
public class NoticeTree {
	private final SourceEntry rootSourceEntry = new SourceEntry(null);
	private final Map<Source, SourceEntry> sourcesToSourceEntries = new HashMap<>();
	
	/**
	 * An individual item in the notice tree.
	 * @author albert
	 *
	 */
	public class SourceEntry {
		final Source source;
		final Set<SourceEntry> children = new HashSet<>();
		
		protected SourceEntry(Source source) {
			this.source = source;
		}
		
		/**
		 * @return	The {@link Source} this represents.
		 */
		public final Source getSource() {
			return this.source;
		}
		
		
		/**
		 * @return	An array of the children {@link SourceEntry} objects. The array
		 * is a copy and can be modified without side effects.
		 */
		public SourceEntry [] getChildren() {
			return this.children.toArray(new SourceEntry[this.children.size()]);
		}
		
		/**
		 * @return	<code>true</code> if the entry has children.
		 */
		public boolean hasChildren() {
			return !this.children.isEmpty();
		}
		
		protected SourceEntry addChildSource(Source source) {
			SourceEntry sourceEntry = new SourceEntry(source);
			this.children.add(sourceEntry);
			return sourceEntry;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return this.source.getSourceTitle();
		}
	}
	
	
	public NoticeTree() {
		
	}
	
	/**
	 * @return	The root source entry of the notice tree.
	 */
	public final SourceEntry getRootSourceEntry() {
		return this.rootSourceEntry;
	}
	
	/**
	 * Removes all notices from the tree.
	 */
	public void clearNotices() {
		this.rootSourceEntry.children.clear();
		this.sourcesToSourceEntries.clear();
	}
	
	
	/**
	 * @return	<code>true</code> if there are any notices.
	 */
	public boolean isNotices() {
		return !this.rootSourceEntry.children.isEmpty();
	}
	
	
	/**
	 * Adds a notice to the tree.
	 * @param source	The source, if <code>null</code> the notice is added to the root.
	 * @param message	The message of the notice.
	 * @param extraContent	Additional information for the notice.
	 */
	public void addNotice(Source source, String message, String extraContent) {
		addNotice(new TextSource(source, message, extraContent));
	}
	
	
	/**
	 * Adds a source to the tree if it is not already in the tree.
	 * @param source	The source to be added.
	 * @return	The {@link SourceEntry} in the tree holding the source.
	 */
	public SourceEntry addNotice(Source source) {
		SourceEntry sourceEntry = this.sourcesToSourceEntries.get(source);
		if (sourceEntry != null) {
			return sourceEntry;
		}
		
		Source parentSource = source.getParentSource();
		SourceEntry parentSourceEntry = (parentSource == null) ? this.rootSourceEntry : addNotice(parentSource);
		sourceEntry = parentSourceEntry.addChildSource(source);
		
		this.sourcesToSourceEntries.put(source, sourceEntry);
		return sourceEntry;
	}
	
	
	/**
	 * Interface used to retrieve information about a particular notice or its parent.
	 * @author albert
	 *
	 */
	public interface Source {
		/**
		 * @return	The optional parent of the source.
		 */
		public Source getParentSource();
		
		/**
		 * @return	The main text of the source.
		 */
		public String getSourceTitle();
		
		/**
		 * @return	Optional extra description for the source
		 */
		public String getSourceDescription();
	}
	
	
	/**
	 * Simple text based implementation of {@link Source}.
	 * @author albert
	 *
	 */
	public static class TextSource implements Source {
		final Source parentSource;
		final String title;
		final String description;
		
		/**
		 * Constructor.
		 * @param parentSource	The parent of the source, may be <code>null</code>
		 * @param title	The title message.
		 * @param description	The description.
		 */
		public TextSource(Source parentSource, String title, String description) {
			this.parentSource = parentSource;
			this.title = title;
			this.description = description;
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.NoticeManager.Source#getParentSource()
		 */
		@Override
		public Source getParentSource() {
			return this.parentSource;
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.NoticeManager.Source#getSourceTitle()
		 */
		@Override
		public String getSourceTitle() {
			return this.title;
		}

		/* (non-Javadoc)
		 * @see gnucashjgnash.NoticeManager.Source#getSourceDescription()
		 */
		@Override
		public String getSourceDescription() {
			return this.description;
		}
	}
}
