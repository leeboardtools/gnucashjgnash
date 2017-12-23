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
import java.math.RoundingMode;

/**
 * Represents a parsed GnuCash GncNumeric from <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>
 * @author albert
 *
 */
public class NumericEntry {
    BigInteger numerator = null;
    BigInteger denominator = null;
    int scale;
    
    
    public void fromRealString(String valueText, BigInteger denominator) {
    	float value = Float.parseFloat(valueText) * denominator.floatValue();
    	this.numerator = BigInteger.valueOf(Math.round(value));
    	this.denominator = denominator;
    }
    

    public static class NumericStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final NumericEntry numericEntry;
        NumericStateHandler(final NumericEntry numericEntry, GnuCashToJGnashContentHandler contentHandler,
                          GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName);
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
                this.numericEntry.scale = (int)Math.round(Math.log10(this.numericEntry.denominator.intValue()));
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
    
    public BigDecimal divide(NumericEntry divisor) {
        int scale = Math.max(this.scale, divisor.scale);
        BigDecimal bdNumerator = new BigDecimal(this.numerator).multiply(new BigDecimal(divisor.denominator)).setScale(scale);
        BigDecimal bdDenominator = new BigDecimal(this.denominator).multiply(new BigDecimal(divisor.numerator)).setScale(scale);
        return bdNumerator.divide(bdDenominator, RoundingMode.HALF_EVEN);
    }
}
