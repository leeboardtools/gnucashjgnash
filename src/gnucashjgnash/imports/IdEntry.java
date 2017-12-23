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

import org.xml.sax.Attributes;


/**
 * Represents a parsed GnuCash id (currently only "guid"/GUID) from <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>
 * @author albert
 *
 */
public class IdEntry {
    String type;
    String id;


    public static class IdStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final IdEntry idEntry;

        IdStateHandler(final IdEntry idEntry, GnuCashToJGnashContentHandler contentHandler,
                       GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName);
            this.idEntry = idEntry;
        }

        @Override
        protected void endState() {
            super.endState();
            this.idEntry.id = this.characters;
        }

        @Override
        public void handleStateAttributes(Attributes atts) {
            super.handleStateAttributes(atts);
            this.idEntry.type = atts.getValue("type");
        }
    }

    boolean validateGUIDParse(GnuCashToJGnashContentHandler.StateHandler stateHandler, String qName) {
        if (this.type == null) {
            stateHandler.recordWarning("IdTypeMissing_" + qName, "Message.Parse.XMLIdMissingType", qName, "type");
            return false;
        }
        if (!"guid".equals(this.type)) {
            stateHandler.recordWarning("IdTypeInvalid_" + qName, "Message.Parse.XMLIdTypeNotGUID", qName, "type", "guid");
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IdEntry)) {
            return false;
        }

        IdEntry other = (IdEntry)obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        }
        if (this.type == null) {
            if (other.type != null) {
                return false;
            }
        }

        return this.id.equals(other.id) && this.type.equals(other.type);
    }

    @Override
    public String toString() {
        return id;
    }
}
