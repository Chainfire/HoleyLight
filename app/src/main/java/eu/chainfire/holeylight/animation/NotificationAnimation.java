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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.android.systemui.VIDirector;

import java.util.Locale;

import androidx.core.view.WindowInsetsCompat;
import eu.chainfire.holeylight.misc.CameraCutout;
import eu.chainfire.holeylight.misc.Fold;
import eu.chainfire.holeylight.misc.Manufacturer;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;

@SuppressWarnings({ "unused", "WeakerAccess" })
public class NotificationAnimation implements Settings.OnSettingsChangedListener {
    public static volatile Boolean test_lastHideAOD = null;

    private static final CameraCutout.Cutout OVERRIDE_CUTOUT = null; //CameraCutout.CUTOUT_S10PLUS;
    private static final String OVERRIDE_DEVICE = null; //"beyond2";

    public interface OnNotificationAnimationListener {
        void onDimensionsApplied(SpritePlayer view);
        boolean onAnimationFrameStart(SpritePlayer view, boolean draw);
        void onAnimationFrameEnd(SpritePlayer view, boolean draw);
        boolean onAnimationComplete(SpritePlayer view);
    }

    // From SystemUI: assets/face_unlocking_cutout_ic_XXX.json, or assets/punch_hole_ic_XXX.json
    private static final String jsonBeyond0 = "{\"v\":\"5.1.20\",\"fr\":60,\"ip\":0,\"op\":61,\"w\":132,\"h\":132,\"nm\":\"beyond_punch_cut_ani_B0\",\"ddd\":0,\"assets\":[],\"layers\":[{\"ddd\":0,\"ind\":1,\"ty\":1,\"nm\":\"L\",\"td\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":45,\"ix\":10},\"p\":{\"a\":1,\"k\":[{\"i\":{\"x\":0.1,\"y\":1},\"o\":{\"x\":0.33,\"y\":0},\"n\":\"0p1_1_0p33_0\",\"t\":0,\"s\":[-41,66.548,0],\"e\":[170,66.548,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"t\":60}],\"ix\":2},\"a\":{\"a\":0,\"k\":[24,125,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"sw\":48,\"sh\":250,\"sc\":\"#ffffff\",\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":2,\"ty\":4,\"nm\":\"cue_02\",\"tt\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[66,66,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[27.614,0],[0,-27.616],[-27.614,0],[0,27.616]],\"o\":[[-27.614,0],[0,27.616],[27.614,0],[0,-27.616]],\"v\":[[0,-50],[-50,0.004],[0,50.008],[50,0.004]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":3,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[100,100],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":2,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Ellipse 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":3,\"ty\":4,\"nm\":\"cue_01\",\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":30,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[66,66,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[27.614,0],[0,-27.616],[-27.614,0],[0,27.616]],\"o\":[[-27.614,0],[0,27.616],[27.614,0],[0,-27.616]],\"v\":[[0,-50],[-50,0.004],[0,50.008],[50,0.004]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":3,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[100,100],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":2,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Ellipse 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0}],\"markers\":[]}";
    private static final String jsonBeyond1 = "{\"v\":\"5.1.20\",\"fr\":60,\"ip\":0,\"op\":61,\"w\":138,\"h\":138,\"nm\":\"beyond_punch_cut_ani_B1\",\"ddd\":0,\"assets\":[],\"layers\":[{\"ddd\":0,\"ind\":1,\"ty\":1,\"nm\":\"L\",\"parent\":2,\"td\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":45,\"ix\":10},\"p\":{\"a\":1,\"k\":[{\"i\":{\"x\":0.1,\"y\":1},\"o\":{\"x\":0.33,\"y\":0},\"n\":\"0p1_1_0p33_0\",\"t\":0,\"s\":[-107.5,0.548,0],\"e\":[108,0.548,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"t\":60}],\"ix\":2},\"a\":{\"a\":0,\"k\":[24,125,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"sw\":48,\"sh\":250,\"sc\":\"#ffffff\",\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":2,\"ty\":4,\"nm\":\"cue_02\",\"tt\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[69,69,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[28.719,0],[0,-28.721],[-28.719,0],[0,28.721]],\"o\":[[-28.719,0],[0,28.721],[28.719,0],[0,-28.721]],\"v\":[[0,-52.008],[-52,-0.004],[0,52],[52,-0.004]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":3,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[104,104],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":2,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Ellipse 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":3,\"ty\":4,\"nm\":\"cue_01\",\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":30,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[69,69,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[28.719,0],[0,-28.721],[-28.719,0],[0,28.721]],\"o\":[[-28.719,0],[0,28.721],[28.719,0],[0,-28.721]],\"v\":[[0,-52.008],[-52,-0.004],[0,52],[52,-0.004]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":3,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[104,104],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":2,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Ellipse 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0}],\"markers\":[]}";
    private static final String jsonBeyond2 = "{\"v\":\"5.1.20\",\"fr\":60,\"ip\":0,\"op\":61,\"w\":258,\"h\":132,\"nm\":\"beyond_punch_cut_ani_B2\",\"ddd\":0,\"assets\":[],\"layers\":[{\"ddd\":0,\"ind\":1,\"ty\":1,\"nm\":\"L\",\"td\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":45,\"ix\":10},\"p\":{\"a\":1,\"k\":[{\"i\":{\"x\":0.1,\"y\":1},\"o\":{\"x\":0.33,\"y\":0},\"n\":\"0p1_1_0p33_0\",\"t\":0,\"s\":[-40,70.548,0],\"e\":[288,70.548,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"t\":60}],\"ix\":2},\"a\":{\"a\":0,\"k\":[24,125,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"sw\":48,\"sh\":250,\"sc\":\"#ffffff\",\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":2,\"ty\":4,\"nm\":\"cue_02\",\"tt\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[129,66,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[0,-27.062],[27.062,0],[0,0],[0,27.062],[-27.062,0],[0,0]],\"o\":[[0,27.062],[0,0],[-27.062,0],[0,-27.062],[0,0],[27.062,0]],\"v\":[[110,0],[61,49],[-61,49],[-110,0],[-61,-49],[61,-49]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"ty\":\"rc\",\"d\":1,\"s\":{\"a\":0,\"k\":[220,98],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"r\":{\"a\":0,\"k\":54,\"ix\":4},\"nm\":\"Rectangle Path 1\",\"mn\":\"ADBE Vector Shape - Rect\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":1,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Rectangle 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":3,\"ty\":4,\"nm\":\"cue_01\",\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":30,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[129,66,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[0,-27.062],[27.062,0],[0,0],[0,27.062],[-27.062,0],[0,0]],\"o\":[[0,27.062],[0,0],[-27.062,0],[0,-27.062],[0,0],[27.062,0]],\"v\":[[110,0],[61,49],[-61,49],[-110,0],[-61,-49],[61,-49]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"ty\":\"rc\",\"d\":1,\"s\":{\"a\":0,\"k\":[220,98],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"r\":{\"a\":0,\"k\":54,\"ix\":4},\"nm\":\"Rectangle Path 1\",\"mn\":\"ADBE Vector Shape - Rect\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":1,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Rectangle 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0}],\"markers\":[]}";
    private static final String jsonDefault = jsonBeyond1;

    private static class DeviceSpecs {
        public final String device;
        public final boolean exact;
        public String json;
        public final float dpAddScaleBase;
        public final float dpAddScaleHorizontal;
        public final float dpShiftVertical;
        public final float dpShiftHorizontal;
        public final float dpAddDoze;
        public final boolean supported;
        public final boolean officiallySupported;

        public DeviceSpecs(String device, boolean exact, String json, float dpAddScaleBase, float dpAddScaleHorizontal, float dpShiftVertical, float dpShiftHorizontal, float dpAddDoze, boolean supported, boolean officiallySupported) {
            this.device = device.toLowerCase(Locale.ENGLISH);
            this.exact = exact;
            this.json = json;
            this.dpAddScaleBase = dpAddScaleBase;
            this.dpAddScaleHorizontal = dpAddScaleHorizontal;
            this.dpShiftVertical = dpShiftVertical;
            this.dpShiftHorizontal = dpShiftHorizontal;
            this.dpAddDoze = dpAddDoze;
            this.supported = supported;
            this.officiallySupported = officiallySupported;
        }
    }

    private static final DeviceSpecs[] deviceSpecs = new DeviceSpecs[] {
            /* Samsung viDirector */ new DeviceSpecs("_viDirector", true, null, 0, 0, 0, 0, 1f, true, true),

            /* Samsung S10e       */ new DeviceSpecs("beyond0", false, jsonBeyond0, 4, 0, 0, 0, 1f, true, true),
            /* Samsung S10        */ new DeviceSpecs("beyond1", false, jsonBeyond1, 4, 0, 0, 0, 1f, true, true),
            /* Samsung S10+       */ new DeviceSpecs("beyond2", false, jsonBeyond2, 5, 1, 0.25f, -1.75f, 1f, true, true),

            /* Google Pixel 4a    */ new DeviceSpecs("sunfish", true, jsonDefault, -7, 0, 5.5f, 5.5f, 3f, true, true),
            /* Google Pixel 4a 5G */ new DeviceSpecs("bramble", true, jsonDefault, -7, 0, 3.5f, 3.5f, 3f, true, true),
            /* Google Pixel 5     */ new DeviceSpecs("redfin", true, jsonDefault, -11.75f, 0, 3.25f, 4.75f, 3f, true, true),

            /* Samsung Generic    */ new DeviceSpecs("_samsung", true, jsonDefault, 0, 0, 0, 0, 1f, true, false),
            /* Google Generic     */ new DeviceSpecs("_google", true, jsonDefault, 0, 0, 0, 0, 3f, true, false),

            /* Unsupported        */ new DeviceSpecs("_unsupported", true, null, 0, 0, 0, 0, 3f, false, false)
    };

    private static DeviceSpecs findDevice(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        for (DeviceSpecs spec : deviceSpecs) {
            if (
                    (spec.exact && name.equals(spec.device)) ||
                    (!spec.exact && name.startsWith(spec.device))
            ) {
                return spec;
            }
        }
        return null;
    }

    private final OnNotificationAnimationListener onNotificationAnimationListener;
    private final Settings settings;
    private final CameraCutout cameraCutout;
    private final SpritePlayer spritePlayer;
    private final float densityMultiplier;

    private final DeviceSpecs spec;
    private final VIDirector viDirector;
    private final VIDirector viDirector2;

    private volatile LottieComposition lottieComposition;
    private volatile boolean play = false;

    private volatile int[] colors = new int[] { Color.WHITE, Color.GREEN, Color.RED };
    private volatile int colorIndex = 0;

    private volatile int[] colorsNext = null;
    private volatile boolean playNext = false;

    private volatile boolean inDoze = false;
    private volatile boolean hideAOD = false;
    private volatile boolean hideAODFully = false;
    private volatile boolean showAODClock = false;
    private volatile boolean positionAODClock = false;
    private volatile SpritePlayer.Mode mode = SpritePlayer.Mode.SWIRL;
    private volatile boolean blackFill = false;
    private final Rect tspRect = new Rect(0, 0, 0, 0);
    private final Rect clockRect = new Rect(0, 0, 0, 0);
    private volatile int tspOverlayBottom = 0;
    private volatile float currentDpAddThickness = 0;

    public NotificationAnimation(Context context, SpritePlayer spritePlayer, float densityMultiplier, OnNotificationAnimationListener onNotificationAnimationListener) {
        this.onNotificationAnimationListener = onNotificationAnimationListener;
        settings = Settings.getInstance(context);
        cameraCutout = new CameraCutout(context);
        this.spritePlayer = spritePlayer;
        this.densityMultiplier = densityMultiplier > 0.1f ? densityMultiplier : 1.0f;

        String viJson = null;
        VIDirector viDirector = null;
        VIDirector viDirector2 = null;
        if (OVERRIDE_DEVICE == null && Manufacturer.isSamsung()) {
            if (Fold.isFold()) {
                try {
                    viDirector = VIDirector.create(context, true);
                    viDirector2 = VIDirector.create(context, false);
                    if ((viDirector != null) && (viDirector2 != null)) {
                        Rect r = viDirector.getVIViewLocation();
                        Rect r2 = viDirector2.getVIViewLocation();
                        if (r.width() > 0 && r2.width() > 0) {
                            viJson = viDirector.getFaceRecognitionJson();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    viDirector = VIDirector.create(context, null);
                    if (viDirector != null) {
                        Rect r = viDirector.getVIViewLocation();
                        if (r.width() > 0) {
                            viJson = viDirector.getFaceRecognitionJson();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (viJson != null) {
            this.viDirector = viDirector;
            this.viDirector2 = viDirector2;
            spec = findDevice("_viDirector");
            spec.json = viJson;
            settings.setUsingVIDirector(true);
        } else {
            this.viDirector = null;
            this.viDirector2 = null;
            settings.setUsingVIDirector(false);

            String device = OVERRIDE_DEVICE != null ? OVERRIDE_DEVICE : Build.DEVICE;
            DeviceSpecs spec = findDevice(device);
            if (spec == null && Manufacturer.isSamsung()) spec = findDevice("_samsung");
            if (spec == null && Manufacturer.isGoogle()) spec = findDevice("_google");
            if (spec == null) spec = findDevice("_unsupported");
            this.spec = spec;
        }

        if (!isValid()) return;

        loadJson();

        spritePlayer.setOnAnimationListener(new SpritePlayer.OnAnimationListener() {
            @Override
            public boolean onAnimationFrameStart(boolean draw) {
                if (onNotificationAnimationListener != null) {
                    return onNotificationAnimationListener.onAnimationFrameStart(NotificationAnimation.this.spritePlayer, draw);
                }
                return true;
            }

            @Override
            public void onAnimationFrameEnd(boolean draw) {
                if (onNotificationAnimationListener != null) {
                    onNotificationAnimationListener.onAnimationFrameEnd(NotificationAnimation.this.spritePlayer, draw);
                }
            }

            @SuppressWarnings("NonAtomicOperationOnVolatileField") // is called locked
            @Override
            public boolean onAnimationComplete() {
                boolean again = false;

                boolean newColors = false;
                colorIndex++;
                if (colorIndex >= colors.length) {
                    colorIndex = 0;
                    if (colorsNext != null) {
                        colors = colorsNext;
                        colorsNext = null;
                        newColors = true;
                    }
                }
                if (colors.length > 0) {
                    setColor(colors[colorIndex]);
                    if (play || newColors || (colorIndex > 0)) {
                        again = true;
                    } else {
                        if (onNotificationAnimationListener != null) {
                            again = onNotificationAnimationListener.onAnimationComplete(NotificationAnimation.this.spritePlayer);
                        }
                    }
                    if (newColors) {
                        play = playNext;
                    }
                } else {
                    if (onNotificationAnimationListener != null) {
                        again = onNotificationAnimationListener.onAnimationComplete(NotificationAnimation.this.spritePlayer);
                    }
                }

                return again;
            }
        });

        settings.registerOnSettingsChangedListener(this);

        test_lastHideAOD = false;
    }

    private void loadJson() {
        // not adjusted for density but shouldn't be important here
        float dpToPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, spritePlayer.getContext().getResources().getDisplayMetrics());

        currentDpAddThickness = getDpAddThickness();
        LottieCompositionFactory.fromJsonString(JSONAnimationManipulator.modify(spec.json, currentDpAddThickness * dpToPx), null).addListener(result -> {
            lottieComposition = result;
            spritePlayer.forceReload();
            applyDimensions();
        });
    }

    @Override
    protected void finalize() throws Throwable {
        settings.unregisterOnSettingsChangedListener(this);
        super.finalize();
    }

    private Object getSynchronizer() {
        if (spritePlayer != null) return spritePlayer.getSynchronizer();
        return this;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isValid() {
        synchronized (getSynchronizer()) {
            return (isDeviceSupported() && (spec.json != null) && (spritePlayer != null));
        }
    }

    public boolean isDeviceSupported() {
        return spec.supported;
    }

    public boolean isDeviceOfficiallySupported() {
        return spec.officiallySupported;
    }

    @Override
    public void onSettingsChanged() {
        if (getDpAddThickness() != currentDpAddThickness) {
            loadJson();
        } else {
            applyDimensions();
        }
    }

    private void setColor(int color) {
        synchronized (getSynchronizer()) {
            if (spritePlayer.isMultiColorMode(mode)) {
                spritePlayer.setColors(colors);
            } else {
                spritePlayer.setColors(new int[] { color });
            }
        }
    }

    public void updateFromInsets(WindowInsetsCompat insets) {
        synchronized (getSynchronizer()) {
            cameraCutout.updateFromInsets(insets);
            if (OVERRIDE_CUTOUT != null) cameraCutout.applyCutout(OVERRIDE_CUTOUT);
            if (cameraCutout.isValid()) {
                settings.setCutoutAreaRect(cameraCutout.getCutout().getAreaF());
            }
            applyDimensions();
        }
    }

    @SuppressWarnings({"StatementWithEmptyBody", "WeakerAccess"})
    public void applyDimensions() {
        synchronized (getSynchronizer()) {
            if (!isValid()) return;

            // most of this could just be hardcoded, but whatever

            RectF cutoutRect = settings.getCutoutAreaRect();
            if (cutoutRect.left > -1) {
                cameraCutout.updateFromAreaRect(cutoutRect);
            }

            if ((cameraCutout.isValid() || viDirector != null) && (lottieComposition != null)) {
                int rotation = ((WindowManager) spritePlayer.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                float realDpToPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, spritePlayer.getContext().getResources().getDisplayMetrics());

                Point resolution = cameraCutout.getCurrentResolution();

                float left;
                float top;
                float width;
                float height;

                if (mode == SpritePlayer.Mode.TSP) {
                    top = tspRect.top;
                    if (positionAODClock && clockRect.height() > 0) {
                        top = clockRect.bottom + clockRect.height()/2f;
                    }

                    float centerX = tspRect.exactCenterX();
                    float centerY = top + (tspRect.bottom - top)/2f;

                    // TSP saves the entire width
                    width = resolution.x;
                    height = tspRect.bottom - top;

                    // Limit render square size to 60% of width (resolution)
                    int squareSize = (int)Math.min(resolution.x * 0.6f, Math.min(width, height));

                    // Apply
                    left = centerX - (squareSize / 2f);
                    width = squareSize;
                    height = squareSize;
                    if (left < 0) left = 0;
                    if (left + width >= resolution.x) left = resolution.x - width;
                } else {
                    if (viDirector != null) {
                        Rect r;
                        boolean is2 = false;
                        if ((viDirector2 != null) && Fold.isFolded(viDirector.getScreenWidth(), viDirector.getScreenHeight())) {
                            r = viDirector2.getVIViewLocation();
                        } else {
                            r = viDirector.getVIViewLocation();
                        }
                        left = r.left;
                        top = r.top;
                        width = r.width();
                        height = r.height();

                        // fix aspect ratio
                        Rect b = lottieComposition.getBounds();
                        float fx = width / b.width();
                        float fy = height / b.height();
                        if (fx > fy) {
                            float old = width;
                            width = fx * b.width();
                            left += Math.round((old - width) / 2.0f);
                        } else if (fy > fx) {
                            float old = height;
                            height = fx * b.height();
                            top += Math.round((old - height) / 2.0f);
                        }

                        // tuning
                        realDpToPx *= densityMultiplier;
                        left += getDpShiftHorizontal() * realDpToPx;
                        top += getDpShiftVertical() * realDpToPx;

                        float addVertical = (getDpAddScaleBase() + getDpAdd()) * realDpToPx;
                        float addHorizontal = (addVertical * ((float)b.width() / (float)b.height())) + (getDpAddScaleHorizontal() * realDpToPx);
                        float scaledWidth = width + addHorizontal;
                        float scaledHeight = height + addVertical;
                        left -= (scaledWidth - width) / 2.0f;
                        top -= (scaledHeight - height) / 2.0f;
                        width = scaledWidth;
                        height = scaledHeight;
                    } else {
                        // something weird is going on with Lottie's px->dp if current resolution doesn't match native resolution on Samsung devices
                        // if we ever update the used Lottie library, triple check this all still works out
                        float scale = (float) resolution.x / (float) cameraCutout.getNativeResolution().x;
                        float lottieDpToPx = (1.0f / scale) * realDpToPx;

                        // on Google (but not Samsung) devices this is needed to stabilize tuning parameters between DPI changes
                        // we specifically don't have to apply it to lottieDpToPx; implying these two issues are mutually exclusive
                        realDpToPx *= densityMultiplier;

                        if (Build.VERSION.SDK_INT >= 30) {
                            RectF r = cameraCutout.getCutout().getAreaF();
                            Rect b = lottieComposition.getBounds();

                            r.right = r.left + r.height() * ((float)b.width()/(float)b.height());

                            height = (b.height() / lottieDpToPx);
                            width = (b.width() / lottieDpToPx);
                            left = r.centerX() - (width / 2.0f) + (getDpShiftHorizontal() * realDpToPx);
                            top = r.centerY() - (height / 2.0f) + (getDpShiftVertical() * realDpToPx);

                            float addVertical = (getDpAddScaleBase() + getDpAdd()) * realDpToPx;
                            float addHorizontal = (addVertical * ((float)b.width() / (float)b.height())) + (getDpAddScaleHorizontal() * realDpToPx);
                            float scaledWidth = width + addHorizontal;
                            float scaledHeight = height + addVertical;
                            left -= (scaledWidth - width) / 2.0f;
                            top -= (scaledHeight - height) / 2.0f;
                            width = scaledWidth;
                            height = scaledHeight;

                            float right = left + width;
                            float bottom = top + height;
                            left = (float)Math.floor(left);
                            top = (float)Math.floor(top);
                            width = Math.round(right - left);
                            height = Math.round(bottom - top);
                        } else {
                            Rect r = cameraCutout.getCutout().getArea();
                            Rect b = lottieComposition.getBounds();

                            height = (b.height() / lottieDpToPx);
                            width = (b.width() / lottieDpToPx);
                            left = r.exactCenterX() - (width / 2.0f) + (getDpShiftHorizontal() * realDpToPx);
                            top = r.exactCenterY() - (height / 2.0f) + (getDpShiftVertical() * realDpToPx);

                            // you'd assume as these animations come straight from Samsung's ROMs that
                            // they'd work perfectly out of the box, but oh no...
                            float addVertical = (getDpAddScaleBase() + getDpAdd()) * realDpToPx;
                            float addHorizontal = (addVertical * ((float)b.width() / (float)b.height())) + (getDpAddScaleHorizontal() * realDpToPx);
                            float scaledWidth = width + addHorizontal;
                            float scaledHeight = height + addVertical;
                            left -= (scaledWidth - width) / 2.0f;
                            top -= (scaledHeight - height) / 2.0f;
                            width = scaledWidth;
                            height = scaledHeight;
                        }

                        if (rotation == 2) { // upside down
                            left = resolution.x - (int)(left + width);
                            top = resolution.y - (int)(top + height);
                        }
                    }
                }

                // we're only going to allow portrait and reverse-portrait
                spritePlayer.setVisibility((rotation % 2) == 0 ? View.VISIBLE : View.INVISIBLE);

                Rect update = new Rect();

                // Update internal views first
                if (hideAOD) {
                    update.set((int)left, (int)top, (int)(left + width), (int)(top + height));
                } else {
                    update.set(0, 0, (int)width, (int)height);
                }

                if (spritePlayer.isTSPMode(mode) && showAODClock) {
                    Slog.d("Anim", "Apply/Clock %s [%s]", clockRect.toString(), mode.toString());
                    spritePlayer.updateTransparentArea(clockRect);
                } else {
                    spritePlayer.updateTransparentArea(null);
                }
                Slog.d("Anim", "Apply/View %s [%s]", update.toString(), mode.toString());
                spritePlayer.updateDisplayArea(update);

                // Update parent view
                WindowManager.LayoutParams params = (WindowManager.LayoutParams)spritePlayer.getLayoutParams();
                if (hideAOD) {
                    // If not hideAODFully, less than 100% height, to leave room for the fully charged notification and the fingerprint animation
                    int bottom = resolution.y;
                    if (!hideAODFully) {
                        bottom = (int)(resolution.y * 0.75f);
                        if (Build.VERSION.SDK_INT >= 30) {
                            if (tspOverlayBottom > 0) {
                                bottom = Math.max(tspRect.bottom, tspOverlayBottom);
                            } else if (tspRect.bottom > 0) {
                                bottom = tspRect.bottom;
                            }
                        }
                    }
                    update.set(0, 0, resolution.x, bottom);
                    spritePlayer.setBackgroundColor(Settings.DEBUG_OVERLAY ? 0x40FF0000 : Color.BLACK);
                } else {
                    update.set((int)left, (int)top, (int)(left + width), (int)(top + height));
                    spritePlayer.setBackgroundColor(Color.TRANSPARENT);
                }
                params.x = update.left;
                params.y = update.top;
                params.width = update.width();
                params.height = update.height();
                Slog.d("Anim", "Apply/Container %s [%s]", update.toString(), mode.toString());
                spritePlayer.setLayoutParams(params);
                spritePlayer.setDrawBackground(hideAOD);

                // Get going
                spritePlayer.setSpeed(getSpeedFactor());
                spritePlayer.setOnSpriteSheetNeededListener((w, h, m) -> SpriteSheet.fromLottieComposition(lottieComposition, w, h, m));
                if (!spritePlayer.isAnimating() && play) {
                    spritePlayer.playAnimation();
                }
                spritePlayer.invalidateDisplayArea();
                if (onNotificationAnimationListener != null) {
                    onNotificationAnimationListener.onDimensionsApplied(spritePlayer);
                }
            }
        }
    }

    public void play(int[] colors, Drawable[] icons, boolean once, boolean immediately) {
        synchronized (getSynchronizer()) {
            if ((colors == null) || (colors.length == 0)) {
                stop(true);
                return;
            }
            spritePlayer.setIcons(icons);
            if (spritePlayer.isMultiColorMode(mode)) {
                immediately = true;
            }
            if (spritePlayer.isAnimating() && !immediately) {
                this.colorsNext = colors;
                this.playNext = !once;
            } else {
                if (immediately && !spritePlayer.isTSPMode()) spritePlayer.cancelAnimation();
                this.colors = colors;
                play = !once;
                colorIndex = 0;
                setColor(colors[colorIndex]);
                spritePlayer.playAnimation();
            }
        }
    }

    public void stop(boolean immediately) {
        synchronized (getSynchronizer()) {
            playNext = false;
            colorsNext = null;
            play = false;
            if (isPlaying()) {
                if (immediately) {
                    spritePlayer.cancelAnimation();
                }
            } else {
                if (onNotificationAnimationListener != null) {
                    onNotificationAnimationListener.onAnimationComplete(spritePlayer);
                }
            }
        }
    }

    public boolean isPlaying() {
        synchronized (getSynchronizer()) {
            return play || spritePlayer.isAnimating();
        }
    }

    public float getDpAddScaleBase() {
        return settings.getDpAddScaleBase(spec.dpAddScaleBase);
    }

    public float getDpAddScaleHorizontal() {
        return settings.getDpAddScaleHorizontal(spec.dpAddScaleHorizontal);
    }

    public float getDpShiftVertical() {
        return settings.getDpShiftVertical(spec.dpShiftVertical);
    }

    public float getDpShiftHorizontal() {
        return settings.getDpShiftHorizontal(spec.dpShiftHorizontal);
    }

    public float getDpAddThickness() {
        return settings.getDpAddThickness(0);
    }

    public float getSpeedFactor() {
        return settings.getSpeedFactor();
    }

    public boolean getDoze() {
        return inDoze;
    }

    public void setDoze(boolean doze) {
        synchronized (getSynchronizer()) {
            if (this.inDoze != doze) {
                this.inDoze = doze;
                applyDimensions();
            }
        }
    }

    public float getDpAdd() {
        return inDoze ? spec.dpAddDoze : 0;
    }

    public boolean getHideAOD() {
        return hideAOD;
    }

    public void setHideAOD(boolean hide) {
        setHideAOD(hide, false);
    }

    public void setHideAOD(boolean hide, boolean fully) {
        synchronized (getSynchronizer()) {
            if ((this.hideAOD != hide) || (this.hideAODFully != fully)) {
                this.hideAOD = hide;
                this.hideAODFully = fully;
                applyDimensions();
            }
            test_lastHideAOD = hide;
        }
    }

    public boolean getShowAODClock() {
        return showAODClock;
    }

    public void setShowAODClock(boolean show, boolean position) {
        synchronized (getSynchronizer()) {
            if (this.showAODClock != show || this.positionAODClock != (show || position)) {
                this.showAODClock = show;
                this.positionAODClock = show || position;
                applyDimensions();
            }
        }
    }

    public void setMode(SpritePlayer.Mode mode, boolean blackFill) {
        if (mode != this.mode) {
            boolean apply = spritePlayer.isTSPMode(mode) || spritePlayer.isTSPMode(this.mode);
            this.mode = mode;
            spritePlayer.setMode(mode, blackFill);
            if (apply) {
                applyDimensions();
            }
        } else if (blackFill != this.blackFill) {
            this.blackFill = blackFill;
            spritePlayer.setMode(mode, blackFill);
        }
    }

    public void updateTSPRect(Rect rect, Rect clockRect, int overlayBottom) {
        boolean apply = !rect.equals(tspRect) || !this.clockRect.equals(clockRect) || ((overlayBottom > 0) && (overlayBottom != tspOverlayBottom));
        Slog.d("AOD_TSP", "Anim " + rect.toString() + " clock " + clockRect + " bottom:" + overlayBottom + " apply:" + apply);
        if (apply) {
            tspRect.set(rect);
            if (clockRect != null)  {
                this.clockRect.set(clockRect);
            }  else {
                this.clockRect.set(0, 0, 0, 0);
            }
            if (overlayBottom > 0) tspOverlayBottom = overlayBottom;
            spritePlayer.setTSPBlank(rect.height() == 0);
            applyDimensions();
        }
    }
}
