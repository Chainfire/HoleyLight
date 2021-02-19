/*
 * Copyright (C) 2019-2021 Jorrit "Chainfire" Jongma
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package eu.chainfire.holeylight.misc;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {
    public static Locale getLocale(Context context) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        try {
            Configuration config = context.getResources().getConfiguration();

            String lang = Settings.getInstance(context).getLocale();
            if (!"".equals(lang) && !config.getLocales().get(0).getLanguage().replace('-', '_').equals(lang.replace('-', '_'))) {
                if (lang.contains("_")) {
                    locale = new Locale(lang.substring(0, lang.indexOf("_")), lang.substring(lang.indexOf("_") + 1));
                } else {
                    locale = new Locale(lang);
                }
                if ((locale != null) && !Locale.getDefault().equals(locale)) {
                    Locale.setDefault(locale);
                    config.setLocale(locale);
                    context.getResources().updateConfiguration(config, null);
                }
            }
        } catch (Exception ignored) {
            // occasional NullPointerException for reasons unknown
        }
        return locale;
    }

    public static Configuration getConfiguration(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(getLocale(context));
        configuration.setLayoutDirection(configuration.getLocales().get(0));
        return configuration;
    }

    public static Context getContext(Context context) {
        Context ret = context.createConfigurationContext(getConfiguration(context));
        updateResources(ret);
        updateResources(context);
        return ret;
    }

    public static void updateResources(Context context) {
        context.getResources().updateConfiguration(LocaleHelper.getConfiguration(context), null);
    }
}
