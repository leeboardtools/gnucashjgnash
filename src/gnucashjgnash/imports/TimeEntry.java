package gnucashjgnash.imports;

import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

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
            super(contentHandler, parentStateHandler, elementName, _getTimeStateHandlerQNameToStateHandlers());
            this.timeEntry = timeEntry;
        }
    }


    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _TimeStateHandlerQNameToStateHandlers = null;
    static Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> _getTimeStateHandlerQNameToStateHandlers() {
        if (_TimeStateHandlerQNameToStateHandlers == null) {
            Map<String, GnuCashToJGnashContentHandler.StateHandlerCreator> stateHandlers = _TimeStateHandlerQNameToStateHandlers = new HashMap<>();

            GnuCashToJGnashContentHandler.addSimpleDataStateHandler(stateHandlers, "ts:date", new SimpleDataSetterImpl() {
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
        return _TimeStateHandlerQNameToStateHandlers;
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
