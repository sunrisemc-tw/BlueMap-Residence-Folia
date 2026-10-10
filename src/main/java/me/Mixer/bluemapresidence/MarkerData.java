package me.Mixer.bluemapresidence;

import de.bluecolored.bluemap.api.math.Color;

import java.util.UUID;

/**
 * Immutable-ish snapshot of everything needed to build a single BlueMap marker for one residence area.
 * <p>
 * It is produced on the global region thread (where Residence / Bukkit / config / PlaceholderAPI reads
 * are safe) and then consumed on the async scheduler, where the heavy geometry tessellation
 * ({@link de.bluecolored.bluemap.api.math.Shape}), marker construction and BlueMap marker-set writes
 * happen. Holding only primitives + already-resolved strings/colors means the async phase never has to
 * touch Residence, Bukkit or PlaceholderAPI again.
 */
public class MarkerData {

    public enum Kind {
        POINT, CIRCLE, ELLIPSE, RECTANGLE2D, CIRCLE2D, ELLIPSE2D, RECTANGLE
    }

    public UUID worldUid;
    public boolean targetSubzones;
    public String key;
    public Kind kind;

    public String detail;

    public Color line;
    public Color fill;
    public int lineWidth;
    public boolean depthTest;

    public double[] x;
    public float[] y;
    public double[] z;
    public double centX;
    public double centZ;
    public double dist;
    public double distX;
    public double distZ;

    public int points;
    public int yHeight;
    public double pointY;

    public String icon;
    public int anchorX;
    public int anchorY;
}
