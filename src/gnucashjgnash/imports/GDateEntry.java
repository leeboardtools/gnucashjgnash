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
import java.time.format.DateTimeParseException;

import gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler;
import gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler;

/**
 * @author albert
 *
 */
public class GDateEntry {
    LocalDate localDate = LocalDate.now();
    String parseError;
    
    
    public static class GDateStateHandler extends AbstractStateHandler {
        final GDateEntry gDateEntry;
        
        GDateStateHandler(GDateEntry gDateEntry, GnuCashToJGnashContentHandler contentHandler, StateHandler parentStateHandler,
                String elementName) {
            super(contentHandler, parentStateHandler, elementName);
            this.gDateEntry = gDateEntry;
        }

        /* (non-Javadoc)
         * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.AbstractStateHandler#endState()
         */
        @Override
        protected void endState() {
            super.endState();
            
            try {
                this.gDateEntry.parseError = null;
                this.gDateEntry.localDate = LocalDate.parse(this.characters);
            }
            catch (DateTimeParseException e) {
                this.gDateEntry.parseError = e.getLocalizedMessage();
            }
        }
        
    }
}
