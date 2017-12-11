package gnucashjgnash;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GnuCashConvertUtil {

    /**
     * key for locale preference
     */
    private static final String LOCALE = "locale";

    private static final String DEFAULT_RESOURCE_BUNDLE = "resource";

    private static ResourceBundle resourceBundle;


    public synchronized static ResourceBundle getBundle() {
        if (resourceBundle == null) {

            try {
                resourceBundle = ResourceBundle.getBundle(DEFAULT_RESOURCE_BUNDLE);
            } catch (final MissingResourceException e) {
                Logger.getLogger(GnuCashConvertUtil.class.getName()).log(Level.WARNING, "Could not find correct resource bundle", e);
                resourceBundle = ResourceBundle.getBundle(DEFAULT_RESOURCE_BUNDLE, Locale.ENGLISH);
            }
        }

        return resourceBundle;
    }

    /**
     * Gets a localized string with arguments
     *
     * @param key The key for the localized string
     * @param arguments arguments to pass the the message formatter
     * @return The localized string
     */
    public static String getString(final String key, final Object... arguments) {
        try {
            if (arguments.length == 0) {
                return getBundle().getString(key);
            }
            return MessageFormat.format(getBundle().getString(key), arguments);
        } catch (final MissingResourceException mre) {
            Logger.getLogger(GnuCashConvertUtil.class.getName()).log(Level.WARNING, "Missing resource for: " + key, mre);
            return key;
        }
    }

}
