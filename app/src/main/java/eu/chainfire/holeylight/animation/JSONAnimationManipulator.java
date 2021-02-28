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

package eu.chainfire.holeylight.animation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import eu.chainfire.holeylight.misc.Slog;

public class JSONAnimationManipulator {
    private final double thickness;
    private String output;

    private boolean process(String key, Object obj, int level, String path) throws JSONException {
        String value;
        if (obj instanceof JSONObject) {
            value = "{}";
        } else if (obj instanceof JSONArray) {
            value = "[]";
        } else {
            value = obj.toString();
        }
        Slog.d("JSON", "%s/%s %s", path, key, value);

        boolean ret = (!(obj instanceof JSONObject) && !(obj instanceof JSONArray) && (path.contains("/pt/k/v")));

        if (obj instanceof JSONObject) {
            walk((JSONObject)obj, level + 1, path + "/" + key);
        } else if (obj instanceof JSONArray) {
            walk((JSONArray)obj, level + 1, path + "/" + key);
        }

        return ret;
    }

    private Double patch(Object obj) {
       double d = 0;
       if (obj instanceof Double) {
           d = (Double)obj;
       } else if (obj instanceof Float) {
           d = (double)(Float)obj;
       } else if (obj instanceof Integer) {
           d = (double)(Integer)obj;
       }
       double e = Math.abs(d) < 1f ? d : d < 0 ? d + thickness : d - thickness;
       Slog.d("JSON", "%.5f --> %.5f", (float)d, (float)e);
       return e;
    }

    private void walk(JSONArray arr, int level, String path) throws JSONException {
        for (int i = 0; i < arr.length(); i++) {
            if (process("[" + i + "]", arr.get(i), level + 1, path)) {
                arr.put(i, patch(arr.get(i)));
            }
        }
    }

    private void walk(JSONObject obj, int level, String path) throws JSONException {
        for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
            String key = it.next();
            if (process(key, obj.get(key), level, path)) {
                obj.put(key, patch(obj.get(key)));
            }
        }
    }

    private JSONAnimationManipulator(String input, float thickness) {
        this.thickness = thickness;
        output = input;
        if (Math.abs(thickness) > 0.125) {
            try {
                JSONObject obj = new JSONObject(input);
                walk(obj, 0, "");
                output = obj.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static String modify(String input, float thickness) {
        return new JSONAnimationManipulator(input, thickness).output;
    }
}
