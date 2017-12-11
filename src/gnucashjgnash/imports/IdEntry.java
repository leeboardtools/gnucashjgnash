package gnucashjgnash.imports;

import org.xml.sax.Attributes;

public class IdEntry {
    String type;
    String id;


    public static class IdStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final IdEntry idEntry;

        IdStateHandler(final IdEntry idEntry, GnuCashToJGnashContentHandler contentHandler,
                       GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName, null);
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
