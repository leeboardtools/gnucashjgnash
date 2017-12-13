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

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumericEntry {
    BigInteger numerator = null;
    BigInteger denominator = null;

    public static class NumericStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final NumericEntry numericEntry;
        NumericStateHandler(final NumericEntry numericEntry, GnuCashToJGnashContentHandler contentHandler,
                          GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName, null);
            this.numericEntry = numericEntry;
        }

        @Override
        protected void endState() {
            super.endState();

            this.numericEntry.numerator = null;
            this.numericEntry.denominator = null;

            int dividerIndex = this.characters.indexOf('/');
            if (dividerIndex < 0) {
                recordWarning("NumericValueMissingDiv_" + this.elementName, "Message.Parse.XMLNumericDividerMissing", this.elementName);
                return;
            }

            String numeratorText = this.characters.substring(0, dividerIndex);
            String denominatorText = this.characters.substring(dividerIndex + 1);

            try {
                this.numericEntry.numerator = new BigInteger(numeratorText);
                this.numericEntry.denominator = new BigInteger(denominatorText);
            }
            catch (NumberFormatException e) {
                recordWarning("NumericValueInvalid_" + this.elementName, "Message.Parse.XMLNumericValueInvalid", this.elementName);
            }
        }
    }

    boolean validateParse(GnuCashToJGnashContentHandler.StateHandler stateHandler, String qName) {
        return (this.numerator != null) && (this.denominator != null);
    }


    public BigDecimal toBigDecimal() {
        return new BigDecimal(this.numerator).divide(new BigDecimal(this.denominator));
    }
}
