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

/*
 * Inspired by AOSP's AnyMotionDetector class
 */

package eu.chainfire.holeylight.misc;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;

@SuppressWarnings({ "WeakerAccess", "unused", "UnusedReturnValue" })
public class MotionSensor {
    private static MotionSensor instance = null;
    public static MotionSensor getInstance(Context context) {
        if (instance == null) {
            instance = new MotionSensor(context);
        }
        return instance;
    }

    public enum MotionState { UNKNOWN, STATIONARY, MOVING }

    public interface OnMotionStateListener {
        boolean onMotionState(MotionState motionState, long for_millis);
    }

    private static final String TAG = "MotionSensor";
    private static final boolean DEBUG = false;

    private static final float THRESHOLD_ENERGY = 50f; // pretty high, but notification vibration can hit 50-ish, prefer angle detection
    private static final float THRESHOLD_ANGLE = 10f;
    private static final long ORIENTATION_MEASUREMENT_DURATION_MILLIS = 200;
    private static final long ACCELEROMETER_DATA_TIMEOUT_MILLIS = 1000;
    private static final int SAMPLING_INTERVAL_MILLIS = 40;
    private static final int ANGLE_AGE_MILLIS = 1000;

    private final Object lock = new Object();
    private final Sensor accelSensor;
    private final SensorManager sensorManager;
    private final RunningSignalStats runningStats;
    private final Handler handler;
    private final List<Vector3> history = new ArrayList<>();
    private final List<OnMotionStateListener> listeners = new ArrayList<>();
    private boolean measurementInProgress;
    private MotionState motionState = MotionState.UNKNOWN;
    private long motionStateStart = 0;

    private MotionSensor(Context context) {
        handler = new Handler(Looper.getMainLooper());
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        measurementInProgress = false;
        runningStats = new RunningSignalStats();
    }

    private final SensorEventListener accelListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (lock) {
                long now = SystemClock.elapsedRealtime();

                if (now - runningStats.getLastReset() >= ACCELEROMETER_DATA_TIMEOUT_MILLIS) {
                    runningStats.reset();
                }

                Vector3 accelDatum = new Vector3(now, event.values[0], event.values[1], event.values[2]);
                runningStats.accumulate(accelDatum);
                if (runningStats.getSampleCount() >= (int) Math.ceil(((double)ORIENTATION_MEASUREMENT_DURATION_MILLIS / SAMPLING_INTERVAL_MILLIS))) {
                    Vector3 average = runningStats.getRunningAverage();
                    float energy = runningStats.getEnergy();
                    float angle = 0.0f;

                    long cutoff = now - ANGLE_AGE_MILLIS;
                    for (int i = history.size() - 1; i >= 0; i--) {
                        if (history.get(i).timeMillisSinceBoot < cutoff) {
                            history.remove(i);
                        }
                    }
                    for (Vector3 historic : history) {
                        angle = Math.max(angle, historic.angleBetween(average));
                    }

                    history.add(average);
                    runningStats.reset();

                    Slog.i(TAG, String.format(Locale.ENGLISH, "energy:%10.8f angle:%4.2f", energy, angle));

                    MotionState nextState = MotionState.STATIONARY;
                    if ((energy >= THRESHOLD_ENERGY) || (angle >= THRESHOLD_ANGLE)) {
                        nextState = MotionState.MOVING;
                    }
                    if ((nextState != motionState) || (motionStateStart == 0)) {
                        motionState = nextState;
                        motionStateStart = now;
                    }
                    for (final OnMotionStateListener listener : listeners) {
                        handler.post(() -> {
                            if (!listener.onMotionState(motionState, now - motionStateStart)) {
                                stop(listener);
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private synchronized void start() {
        if (!measurementInProgress) {
            if (sensorManager.registerListener(accelListener, accelSensor, SAMPLING_INTERVAL_MILLIS * 1000)) {
                measurementInProgress = true;
                history.clear();
                runningStats.reset();
                motionState = MotionState.UNKNOWN;
                motionStateStart = SystemClock.elapsedRealtime();
            }
        }
    }

    private synchronized void stop() {
        if (measurementInProgress) {
            measurementInProgress = false;
            sensorManager.unregisterListener(accelListener);
            runningStats.reset();
        }
    }

    public MotionState start(OnMotionStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            start();
        }
        return motionState;
    }

    public void stop(OnMotionStateListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
            if (listeners.size() == 0) {
                stop();
            }
        }

    }

    public void resetDuration() {
        motionStateStart = 0;
    }

    public void stopAll() {
        listeners.clear();
        stop();
    }

    // From: AnyMotionDetector.Vector3
    /**
     * A timestamped three dimensional vector and some vector operations.
     */
    public static final class Vector3 {
        public long timeMillisSinceBoot;
        public float x;
        public float y;
        public float z;

        public Vector3(long timeMillisSinceBoot, float x, float y, float z) {
            this.timeMillisSinceBoot = timeMillisSinceBoot;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float norm() {
            return (float) Math.sqrt(dotProduct(this));
        }

        public Vector3 normalized() {
            float mag = norm();
            return new Vector3(timeMillisSinceBoot, x / mag, y / mag, z / mag);
        }

        /**
         * Returns the angle between this 3D vector and another given 3D vector.
         * Assumes both have already been normalized.
         *
         * @param other The other Vector3 vector.
         * @return angle between this vector and the other given one.
         */
        public float angleBetween(Vector3 other) {
            Vector3 crossVector = cross(other);
            float degrees = Math.abs((float)Math.toDegrees(
                    Math.atan2(crossVector.norm(), dotProduct(other))));
            if (DEBUG)
                Slog.d(TAG, "angleBetween: this = " + this.toString() +
                    ", other = " + other.toString() + ", degrees = " + degrees);
            return degrees;
        }

        public Vector3 cross(Vector3 v) {
            return new Vector3(
                v.timeMillisSinceBoot,
                y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x);
        }

        @NonNull
        @Override
        public String toString() {
            String msg = "";
            msg += "timeMillisSinceBoot=" + timeMillisSinceBoot;
            msg += " | x=" + x;
            msg += ", y=" + y;
            msg += ", z=" + z;
            return msg;
        }

        public float dotProduct(Vector3 v) {
            return x * v.x + y * v.y + z * v.z;
        }

        public Vector3 times(float val) {
            return new Vector3(timeMillisSinceBoot, x * val, y * val, z * val);
        }

        public Vector3 plus(Vector3 v) {
            return new Vector3(v.timeMillisSinceBoot, x + v.x, y + v.y, z + v.z);
        }

        public Vector3 minus(Vector3 v) {
            return new Vector3(v.timeMillisSinceBoot, x - v.x, y - v.y, z - v.z);
        }
    }

    // From: AnyMotionDetector.RunningSignalStats
    /**
     * Maintains running statistics on the signal revelant to AnyMotion detection, including:
     * <ul>
     *   <li>running average.
     *   <li>running sum-of-squared-errors as the energy of the signal derivative.
     * <ul>
     */
    private static class RunningSignalStats {
        Vector3 previousVector;
        Vector3 currentVector;
        Vector3 runningSum;
        float energy;
        int sampleCount;
        long lastReset;

        public RunningSignalStats() {
            reset();
        }

        public void reset() {
            previousVector = null;
            currentVector = null;
            runningSum = new Vector3(0, 0, 0, 0);
            energy = 0;
            sampleCount = 0;
            lastReset = SystemClock.elapsedRealtime();
        }

        /**
         * Apply a 3D vector v as the next element in the running SSE.
         */
        public void accumulate(Vector3 v) {
            if (v == null) {
                if (DEBUG) Slog.i(TAG, "Cannot accumulate a null vector.");
                return;
            }
            sampleCount++;
            runningSum = runningSum.plus(v);
            previousVector = currentVector;
            currentVector = v;
            if (previousVector != null) {
                Vector3 dv = currentVector.minus(previousVector);
                float incrementalEnergy = dv.x * dv.x + dv.y * dv.y + dv.z * dv.z;
                energy += incrementalEnergy;
                if (DEBUG) Slog.i(TAG, "Accumulated vector " + currentVector.toString() +
                        ", runningSum = " + runningSum.toString() +
                        ", incrementalEnergy = " + incrementalEnergy +
                        ", energy = " + energy);
            }
        }

        public Vector3 getRunningAverage() {
            if (sampleCount > 0) {
              return runningSum.times((1.0f / (float)sampleCount));
            }
            return null;
        }

        public float getEnergy() {
            return energy;
        }

        public int getSampleCount() {
            return sampleCount;
        }

        public long getLastReset() {
            return lastReset;
        }

        @NonNull
        @Override
        public String toString() {
            String msg = "";
            String currentVectorString = (currentVector == null) ?
                "null" : currentVector.toString();
            String previousVectorString = (previousVector == null) ?
                "null" : previousVector.toString();
            msg += "previousVector = " + previousVectorString;
            msg += ", currentVector = " + currentVectorString;
            msg += ", sampleCount = " + sampleCount;
            msg += ", energy = " + energy;
            return msg;
        }
    }
}
