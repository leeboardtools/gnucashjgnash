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

import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.SimpleDataStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;

public class TimeEntry {
    LocalDate localDate = LocalDate.now();
    OffsetTime offsetTime = OffsetTime.now();
    int zoneOffset;
    String parseError = null;

    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZ");


    public static class TimeStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final TimeEntry timeEntry;

        TimeStateHandler(final TimeEntry timeEntry, GnuCashToJGnashContentHandler contentHandler, GnuCashToJGnashContentHandler.StateHandler parentStateHandler,
                     String elementName) {
            super(contentHandler, parentStateHandler, elementName, null);
            this.timeEntry = timeEntry;
        }

		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#getStateHandlerForElement(java.lang.String)
		 */
		@Override
		protected StateHandler getStateHandlerForElement(String qName) {
			switch (qName) {
			case "ts:date":
				return new SimpleDataStateHandler(this.contentHandler, this, qName, new SimpleDataSetterImpl() {
	                @Override
	                protected void setTimeEntryField(TimeEntry timeEntry, String value) {
	                    try {
	                        timeEntry.parseError = null;
	                        timeEntry.localDate = LocalDate.parse(value, DATE_TIME_FORMATTER);
	                        timeEntry.offsetTime = OffsetTime.parse(value, DATE_TIME_FORMATTER);
	                    }
	                    catch (DateTimeParseException e) {
	                        timeEntry.parseError = e.getLocalizedMessage();
	                    }
	                }
	            });
 
			}

			return super.getStateHandlerForElement(qName);
		}
    }

    static abstract class SimpleDataSetterImpl extends GnuCashToJGnashContentHandler.AbstractSimpleDataSetter {
        @Override
        public void setData(String characters, GnuCashToJGnashContentHandler.SimpleDataStateHandler stateHandler) {
            TimeStateHandler parentStateHandler = (TimeStateHandler)stateHandler.parentStateHandler;
            setTimeEntryField(parentStateHandler.timeEntry, characters);
        }

        protected abstract void setTimeEntryField(TimeEntry timeEntry, String value);
    }


    boolean validateParse(GnuCashToJGnashContentHandler.StateHandler stateHandler, String qName) {
        if (this.parseError != null) {
            stateHandler.recordWarning("TimeParseError_" + qName, "Message.Parse.XMLTimeParseError", qName, this.parseError);
            return false;
        }
        return true;
    }
}
