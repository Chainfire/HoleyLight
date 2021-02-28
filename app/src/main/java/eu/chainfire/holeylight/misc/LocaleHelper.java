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
        Locale locale = null;
        try {
            Configuration config = context.getResources().getConfiguration();

            String lang = Settings.getInstance(context).getLocale(true);
            if (lang != null && !"".equals(lang) && !config.getLocales().get(0).getLanguage().replace('-', '_').equals(lang.replace('-', '_'))) {
                if (lang.contains("_")) {
                    locale = new Locale(lang.substring(0, lang.indexOf("_")), lang.substring(lang.indexOf("_") + 1));
                } else {
                    locale = new Locale(lang);
                }
                if (locale != null) {
                    if (!Locale.getDefault().getLanguage().equals(locale.getLanguage())) Locale.setDefault(locale);
                    if (!config.getLocales().get(0).getLanguage().equals(locale.getLanguage())) {
                        config.setLocale(locale);
                        context.getResources().updateConfiguration(config, null);
                    }
                }
            }

            if (locale == null) locale = context.getResources().getConfiguration().getLocales().get(0);
        } catch (Exception ignored) {
            // occasional NullPointerException for reasons unknown
        }
        return locale;
    }

    public static Configuration getConfiguration(Context context) {
        return getConfiguration(context, null);
    }

    public static Configuration getConfiguration(Context context, Configuration existingConfiguration) {
        if (existingConfiguration == null) existingConfiguration = context.getResources().getConfiguration();

        Locale locale = getLocale(context);
        if (locale == null || existingConfiguration.getLocales().get(0).getLanguage().equals(locale.getLanguage())) {
            return existingConfiguration;
        }

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(getLocale(context));
        configuration.setLayoutDirection(configuration.getLocales().get(0));
        configuration.uiMode = existingConfiguration.uiMode;
        return configuration;
    }

    public static Context getContext(Context context) {
        Configuration existingConfiguration = context.getResources().getConfiguration();
        Configuration newConfiguration = LocaleHelper.getConfiguration(context, existingConfiguration);

        if (existingConfiguration == newConfiguration) return context;

        Context ret = context.createConfigurationContext(newConfiguration);
        updateResources(ret);
        updateResources(context);
        return ret;
    }

    public static void updateResources(Context context) {
        updateResources(context, null);
    }

    public static void updateResources(Context context, Configuration existingConfiguration) {
        if (existingConfiguration == null) existingConfiguration = context.getResources().getConfiguration();

        Configuration newConfiguration = LocaleHelper.getConfiguration(context, existingConfiguration);

        if (existingConfiguration != newConfiguration) {
            context.getResources().updateConfiguration(newConfiguration, null);
        }
    }
}
