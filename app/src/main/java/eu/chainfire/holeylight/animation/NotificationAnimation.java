/*
 * Copyright (C) 2019 Jorrit "Chainfire" Jongma
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
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;

import androidx.core.view.WindowInsetsCompat;
import eu.chainfire.holeylight.misc.CameraCutout;
import eu.chainfire.holeylight.misc.Settings;

@SuppressWarnings("unused")
public class NotificationAnimation implements Settings.OnSettingsChangedListener {
    private static CameraCutout.Cutout OVERRIDE_CUTOUT = null; //CameraCutout.CUTOUT_S10PLUS;
    private static String OVERRIDE_DEVICE = null; //"beyond2";

    public interface OnNotificationAnimationListener {
        void onDimensionsApplied(SpritePlayer view);
        boolean onAnimationComplete(SpritePlayer view);
    }

    // From SystemUI: assets/face_unlocking_cutout_ic_bX.json
    private static final String jsonBeyond0 = "{\"v\":\"5.1.20\",\"fr\":60,\"ip\":0,\"op\":61,\"w\":132,\"h\":132,\"nm\":\"beyond_punch_cut_ani_B0\",\"ddd\":0,\"assets\":[],\"layers\":[{\"ddd\":0,\"ind\":1,\"ty\":1,\"nm\":\"L\",\"td\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":45,\"ix\":10},\"p\":{\"a\":1,\"k\":[{\"i\":{\"x\":0.1,\"y\":1},\"o\":{\"x\":0.33,\"y\":0},\"n\":\"0p1_1_0p33_0\",\"t\":0,\"s\":[-41,66.548,0],\"e\":[170,66.548,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"t\":60}],\"ix\":2},\"a\":{\"a\":0,\"k\":[24,125,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"sw\":48,\"sh\":250,\"sc\":\"#ffffff\",\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":2,\"ty\":4,\"nm\":\"cue_02\",\"tt\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[66,66,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[27.614,0],[0,-27.616],[-27.614,0],[0,27.616]],\"o\":[[-27.614,0],[0,27.616],[27.614,0],[0,-27.616]],\"v\":[[0,-50],[-50,0.004],[0,50.008],[50,0.004]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":3,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[100,100],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":2,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Ellipse 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":3,\"ty\":4,\"nm\":\"cue_01\",\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":30,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[66,66,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[27.614,0],[0,-27.616],[-27.614,0],[0,27.616]],\"o\":[[-27.614,0],[0,27.616],[27.614,0],[0,-27.616]],\"v\":[[0,-50],[-50,0.004],[0,50.008],[50,0.004]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":3,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[100,100],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":2,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Ellipse 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0}],\"markers\":[]}";
    private static final String jsonBeyond1 = "{\"v\":\"5.1.20\",\"fr\":60,\"ip\":0,\"op\":61,\"w\":138,\"h\":138,\"nm\":\"beyond_punch_cut_ani_B1\",\"ddd\":0,\"assets\":[],\"layers\":[{\"ddd\":0,\"ind\":1,\"ty\":1,\"nm\":\"L\",\"parent\":2,\"td\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":45,\"ix\":10},\"p\":{\"a\":1,\"k\":[{\"i\":{\"x\":0.1,\"y\":1},\"o\":{\"x\":0.33,\"y\":0},\"n\":\"0p1_1_0p33_0\",\"t\":0,\"s\":[-107.5,0.548,0],\"e\":[108,0.548,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"t\":60}],\"ix\":2},\"a\":{\"a\":0,\"k\":[24,125,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"sw\":48,\"sh\":250,\"sc\":\"#ffffff\",\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":2,\"ty\":4,\"nm\":\"cue_02\",\"tt\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[69,69,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[28.719,0],[0,-28.721],[-28.719,0],[0,28.721]],\"o\":[[-28.719,0],[0,28.721],[28.719,0],[0,-28.721]],\"v\":[[0,-52.008],[-52,-0.004],[0,52],[52,-0.004]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":3,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[104,104],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":2,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Ellipse 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":3,\"ty\":4,\"nm\":\"cue_01\",\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":30,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[69,69,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[28.719,0],[0,-28.721],[-28.719,0],[0,28.721]],\"o\":[[-28.719,0],[0,28.721],[28.719,0],[0,-28.721]],\"v\":[[0,-52.008],[-52,-0.004],[0,52],[52,-0.004]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":3,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[104,104],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":2,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Ellipse 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0}],\"markers\":[]}";
    private static final String jsonBeyond2 = "{\"v\":\"5.1.20\",\"fr\":60,\"ip\":0,\"op\":61,\"w\":258,\"h\":132,\"nm\":\"beyond_punch_cut_ani_B2\",\"ddd\":0,\"assets\":[],\"layers\":[{\"ddd\":0,\"ind\":1,\"ty\":1,\"nm\":\"L\",\"td\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":45,\"ix\":10},\"p\":{\"a\":1,\"k\":[{\"i\":{\"x\":0.1,\"y\":1},\"o\":{\"x\":0.33,\"y\":0},\"n\":\"0p1_1_0p33_0\",\"t\":0,\"s\":[-40,70.548,0],\"e\":[288,70.548,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"t\":60}],\"ix\":2},\"a\":{\"a\":0,\"k\":[24,125,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"sw\":48,\"sh\":250,\"sc\":\"#ffffff\",\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":2,\"ty\":4,\"nm\":\"cue_02\",\"tt\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[129,66,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[0,-27.062],[27.062,0],[0,0],[0,27.062],[-27.062,0],[0,0]],\"o\":[[0,27.062],[0,0],[-27.062,0],[0,-27.062],[0,0],[27.062,0]],\"v\":[[110,0],[61,49],[-61,49],[-110,0],[-61,-49],[61,-49]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"ty\":\"rc\",\"d\":1,\"s\":{\"a\":0,\"k\":[220,98],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"r\":{\"a\":0,\"k\":54,\"ix\":4},\"nm\":\"Rectangle Path 1\",\"mn\":\"ADBE Vector Shape - Rect\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":1,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Rectangle 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":3,\"ty\":4,\"nm\":\"cue_01\",\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":30,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[129,66,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"hasMask\":true,\"masksProperties\":[{\"inv\":false,\"mode\":\"s\",\"pt\":{\"a\":0,\"k\":{\"i\":[[0,-27.062],[27.062,0],[0,0],[0,27.062],[-27.062,0],[0,0]],\"o\":[[0,27.062],[0,0],[-27.062,0],[0,-27.062],[0,0],[27.062,0]],\"v\":[[110,0],[61,49],[-61,49],[-110,0],[-61,-49],[61,-49]],\"c\":true},\"ix\":1},\"o\":{\"a\":0,\"k\":100,\"ix\":3},\"x\":{\"a\":0,\"k\":0,\"ix\":4},\"nm\":\"Mask 1\"}],\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"ty\":\"rc\",\"d\":1,\"s\":{\"a\":0,\"k\":[220,98],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"r\":{\"a\":0,\"k\":54,\"ix\":4},\"nm\":\"Rectangle Path 1\",\"mn\":\"ADBE Vector Shape - Rect\",\"hd\":false},{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[1,1,1,1],\"ix\":3},\"o\":{\"a\":0,\"k\":100,\"ix\":4},\"w\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.5],\"y\":[1]},\"o\":{\"x\":[0.33],\"y\":[0]},\"n\":[\"0p5_1_0p33_0\"],\"t\":0,\"s\":[0],\"e\":[14]},{\"i\":{\"x\":[0.833],\"y\":[0.833]},\"o\":{\"x\":[0.1],\"y\":[0]},\"n\":[\"0p833_0p833_0p1_0\"],\"t\":9,\"s\":[14],\"e\":[0]},{\"t\":53,\"s\":[0],\"h\":1}],\"ix\":5},\"lc\":1,\"lj\":1,\"ml\":4,\"nm\":\"Stroke 1\",\"mn\":\"ADBE Vector Graphic - Stroke\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"Rectangle 1\",\"np\":2,\"cix\":2,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":4000,\"st\":0,\"bm\":0}],\"markers\":[]}";

    private final OnNotificationAnimationListener onNotificationAnimationListener;
    private final Settings settings;
    private final CameraCutout cameraCutout;
    private final SpritePlayer spritePlayer;

    private final String json;
    private final int dpAddScaleBase;
    private final int dpAddScaleHorizontal;
    private final int dpShiftVertical;
    private final int dpShiftHorizontal;

    private volatile LottieComposition lottieComposition;
    private volatile boolean play = false;

    private volatile int[] colors = new int[] { Color.WHITE, Color.GREEN, Color.RED };
    private volatile int colorIndex = 0;

    private volatile int[] colorsNext = null;
    private volatile boolean playNext = false;

    public NotificationAnimation(Context context, SpritePlayer spritePlayer, OnNotificationAnimationListener onNotificationAnimationListener) {
        this.onNotificationAnimationListener = onNotificationAnimationListener;
        settings = Settings.getInstance(context);
        cameraCutout = new CameraCutout(context);
        this.spritePlayer = spritePlayer;

        String device = OVERRIDE_DEVICE != null ? OVERRIDE_DEVICE : Build.DEVICE;
        if (device.startsWith("beyond0")) { //s10e
            json = jsonBeyond0;
            dpAddScaleBase = 4;
            dpAddScaleHorizontal = 0;
            dpShiftVertical = 0;
            dpShiftHorizontal = 0;
        } else if (device.startsWith("beyond1")) { // s10
            json = jsonBeyond1;
            dpAddScaleBase = 4;
            dpAddScaleHorizontal = 0;
            dpShiftVertical = 0;
            dpShiftHorizontal = 0;
        } else if (device.startsWith("beyond2")) { // s10+
            json = jsonBeyond2;
            dpAddScaleBase = 4;
            dpAddScaleHorizontal = 1;
            dpShiftVertical = 0;
            dpShiftHorizontal = -1;
        } else {
            json = null;
            dpAddScaleBase = 0;
            dpAddScaleHorizontal = 0;
            dpShiftVertical = 0;
            dpShiftHorizontal = 0;
        }

        if (!isValid()) return;

        LottieCompositionFactory.fromJsonString(json, null).addListener(result -> {
            lottieComposition = result;
            applyDimensions();
        });

        spritePlayer.setOnAnimationCompleteListener(new SpritePlayer.OnAnimationCompleteListener() {
            @Override
            public boolean onAnimationComplete() {
                boolean again = false;

                synchronized (NotificationAnimation.this) {
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
                }

                return again;
            }
        });

        settings.registerOnSettingsChangedListener(this);
    }

    @Override
    protected void finalize() throws Throwable {
        settings.unregisterOnSettingsChangedListener(this);
        super.finalize();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private synchronized boolean isValid() {
        return (json != null) && (spritePlayer != null);
    }

    public synchronized boolean isDeviceSupported() {
        return (json != null);
    }

    @Override
    public void onSettingsChanged() {
        applyDimensions();
    }

    private synchronized void setColor(int color) {
        spritePlayer.setColor(color);
    }

    public synchronized void updateFromInsets(WindowInsetsCompat insets) {
        cameraCutout.updateFromInsets(insets);
        if (OVERRIDE_CUTOUT != null) cameraCutout.applyCutout(OVERRIDE_CUTOUT);
        if (cameraCutout.isValid()) {
            settings.setCutoutAreaRect(cameraCutout.getCutout().getArea());
        }
        applyDimensions();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public synchronized void applyDimensions() {
        // most of this could just be hardcoded, but whatever

        if (!isValid()) return;

        Rect cutoutRect = settings.getCutoutAreaRect();
        if (cutoutRect.left > -1) {
            cameraCutout.updateFromAreaRect(cutoutRect);
        }

        if (cameraCutout.isValid() && (lottieComposition != null)) {
            int rotation = ((WindowManager) spritePlayer.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

            float realDpToPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, spritePlayer.getContext().getResources().getDisplayMetrics());

            Point resolution = cameraCutout.getCurrentResolution();

            // something weird is going on with Lottie's px->dp if current resolution doesn't match native resolution
            float scale = (float) resolution.x / (float) cameraCutout.getNativeResolution().x;
            float LottieDpToPx = (1.0f / scale) * realDpToPx;

            Rect r = cameraCutout.getCutout().getArea();
            Rect b = lottieComposition.getBounds();

            float height = (b.height() / LottieDpToPx);
            float width = (b.width() / LottieDpToPx);
            float left = r.exactCenterX() - (width / 2.0f) + (getDpShiftHorizontal() * realDpToPx);
            float top = r.exactCenterY() - (height / 2.0f) + (getDpShiftVertical() * realDpToPx);

            // you'd assume as these animations come straight from Samsung's ROMs that they'd work perfectly
            // out of the box, but oh no...
            float addVertical = getDpAddScaleBase() * realDpToPx;
            float addHorizontal = (addVertical * ((float)b.width() / (float)b.height())) + (getDpAddScaleHorizontal() * realDpToPx);
            float scaledWidth = width + addHorizontal;
            float scaledHeight = height + addVertical;
            left -= (scaledWidth - width) / 2.0f;
            top -= (scaledHeight - height) / 2.0f;
            width = scaledWidth;
            height = scaledHeight;

            ViewGroup.LayoutParams params = spritePlayer.getLayoutParams();
            params.width = (int)width;
            params.height = (int)height;
            if (params instanceof WindowManager.LayoutParams) {
                if (rotation == 0) {
                    ((WindowManager.LayoutParams)params).x = (int)left;
                    ((WindowManager.LayoutParams)params).y = (int)top;
                } else if (rotation == 1) {
                    // not possible to reach area to render
                } else if (rotation == 2) {
                    ((WindowManager.LayoutParams)params).x = resolution.x - (int)(left + width);
                    ((WindowManager.LayoutParams)params).y = resolution.y - (int)(top + height);
                } else if (rotation == 3) {
                    // not possible to reach area to render
                }
                spritePlayer.setRotation(rotation * -90);

                // we're only going to allow portrait and reverse-portrait
                spritePlayer.setVisibility((rotation % 2) == 0 ? View.VISIBLE : View.INVISIBLE);
            } else if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams)params).setMargins((int)left, (int)top, 0, 0);
            }
            spritePlayer.setLayoutParams(params);

            spritePlayer.setSpeed(getSpeedFactor());

            spritePlayer.setOnSpriteSheetNeededListener((w, h) -> SpriteSheet.fromLottieComposition(lottieComposition, w, h));

            if (!spritePlayer.isAnimating() && play) {
                spritePlayer.playAnimation();
            }

            if (onNotificationAnimationListener != null) {
                onNotificationAnimationListener.onDimensionsApplied(spritePlayer);
            }
        }
    }

    public synchronized void play(boolean once) {
        play = !once;
        if (!spritePlayer.isAnimating()) {
            if (colors.length > 0) {
                colorIndex = 0;
                setColor(colors[colorIndex]);
                spritePlayer.playAnimation();
            }
        }
    }

    public synchronized void play(int[] colors, boolean once) {
        if ((colors == null) || (colors.length == 0)) {
            play = false;
            stop(true);
            return;
        }
        if (spritePlayer.isAnimating()) {
            this.colorsNext = colors;
            this.playNext = !once;
        } else {
            this.colors = colors;
            play = !once;
            colorIndex = 0;
            setColor(colors[colorIndex]);
            spritePlayer.playAnimation();
        }
    }

    public synchronized void stop(boolean immediately) {
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

    public synchronized boolean isPlaying() {
        return play || spritePlayer.isAnimating();
    }

    public int getDpAddScaleBase() {
        return settings.getDpAddScaleBase(dpAddScaleBase);
    }

    public int getDpAddScaleHorizontal() {
        return settings.getDpAddScaleHorizontal(dpAddScaleHorizontal);
    }

    public int getDpShiftVertical() {
        return settings.getDpShiftVertical(dpShiftVertical);
    }

    public int getDpShiftHorizontal() {
        return settings.getDpShiftHorizontal(dpShiftHorizontal);
    }

    public float getSpeedFactor() {
        return settings.getSpeedFactor();
    }
}
