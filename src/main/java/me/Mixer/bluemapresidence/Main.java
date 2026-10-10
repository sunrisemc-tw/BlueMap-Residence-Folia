package me.Mixer.bluemapresidence;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import com.bekvon.bukkit.residence.protection.ResidenceManager;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main
extends JavaPlugin {
    public static Logger log;
    public Residence res;
    public ResidenceManager resmgr;
    public config config;
    private static Main instance;
    public BlueMapAPI blueMap;
    public String MarkerSetIdResidence = "residences";
    public String MarkerSetIdSubzones = "res-subzones";
    public String BMResLinkSpigot = "https://www.spigotmc.org/resources/107389/";
    public String MarkerSetLabelResidence = "Residences";
    public String MarkerSetLabelSubzones = "Residences";
    public boolean papiState;
    private Scheduler.Task updateTimer;

    private static final String DEFAULT_ICON = "https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg";

    // Coalescing single-flight guard for refreshes. Because the heavy work now runs on the async
    // scheduler, two triggers (periodic timer + Residence events) could otherwise run concurrently and
    // race on Residence reads and the BlueMap marker sets. We keep at most one refresh in flight and
    // collapse any triggers that arrive while it runs into a single follow-up refresh.
    private final AtomicBoolean refreshRunning = new AtomicBoolean(false);
    private final AtomicBoolean refreshPending = new AtomicBoolean(false);

    public void onLoad() {
        log = this.getLogger();
    }

    public void info(String msg) {
        this.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&7[&9&lBlueMap Residence&7] &f" + msg)));
    }

    public static void severe(String msg) {
        log.log(Level.SEVERE, msg);
    }

    public Main() {
        instance = this;
    }

    public static Main getInstance() {
        return instance;
    }

    public void onEnable() {
        this.registerFiles();
        this.MarkerSetLabelResidence = this.getConfig().getString("marker.name", "Residences");
        this.MarkerSetLabelSubzones = this.getConfig().getString("marker.subzone.name", "Residences");
        new UpdateChecker(this, 107389).getVersion(version -> {
            if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
                this.info("There is a new update available. " + this.BMResLinkSpigot);
            }
        });
        PluginManager pm = this.getServer().getPluginManager();
        Plugin p = pm.getPlugin("Residence");
        if (p == null) {
            Main.severe("Cannot find Residence!");
            return;
        }
        Plugin papi = pm.getPlugin("PlaceholderAPI");
        if (papi == null) {
            this.info("&cPlaceholderAPI not found, working without it");
            this.papiState = false;
        } else {
            this.papiState = true;
        }
        this.res = (Residence)p;
        Metrics metrics = new Metrics(this, 17404);
        metrics.addCustomChart(new Metrics.SimplePie("update_on_change", () -> String.valueOf(this.getConfig().getBoolean("update.onchange"))));
        metrics.addCustomChart(new Metrics.SimplePie("update_on_period", () -> String.valueOf(this.getConfig().getBoolean("update.onperiod"))));
        metrics.addCustomChart(new Metrics.SimplePie("config_auto_update", () -> String.valueOf(this.getConfig().getBoolean("config.auto_update"))));
        metrics.addCustomChart(new Metrics.AdvancedPie("marker_type", () -> {
            HashMap<String, Integer> valueMap = new HashMap<String, Integer>();
            switch (Objects.requireNonNull(this.getConfig().getString("marker.type")).toLowerCase()) {
                case "point": {
                    valueMap.put("point", 1);
                    break;
                }
                case "circle": {
                    valueMap.put("circle", 1);
                    break;
                }
                case "ellipse": {
                    valueMap.put("ellipse", 1);
                    break;
                }
                case "ellipse2d": {
                    valueMap.put("ellipse2D", 1);
                    break;
                }
                case "rectangle2d": {
                    valueMap.put("rectangle2D", 1);
                    break;
                }
                case "circle2d": {
                    valueMap.put("circle2D", 1);
                    break;
                }
                default: {
                    valueMap.put("rectangle", 1);
                }
            }
            return valueMap;
        }));
        pm.registerEvents((Listener)new ServerJoin(this), (Plugin)this);
        BlueMapAPI.onEnable(api -> {
            this.blueMap = api;
            this.resmgr = Residence.getInstance().getResidenceManager();
            if (this.res.isEnabled()) {
                if (this.getConfig().getBoolean("update.onperiod", true)) {
                    long updateInterval = Math.max(1L, this.getConfig().getLong("update.period", 300L));
                    // Periodic refresh now runs on the async scheduler (see Scheduler.runTimerAsync),
                    // so the heavy marker generation no longer ticks on the global region thread.
                    this.updateTimer = Scheduler.runTimerAsync(this::requestRefresh, 0L, 20L * updateInterval);
                }
                if (this.getConfig().getBoolean("update.onchange", true)) {
                    pm.registerEvents((Listener)new EventList(this), (Plugin)this);
                }
                this.info("Version " + this.getDescription().getVersion() + " is activated");
            }
        });
        Objects.requireNonNull(this.getCommand("bluemapresidence")).setExecutor((CommandExecutor)new ReloadCommand(this));
    }

    public void onDisable() {
        BlueMapAPI.onDisable(blueMapAPI -> {
            if (this.updateTimer != null) {
                this.updateTimer.cancel();
            }
            this.blueMap = null;
        });
    }

    public void registerFiles() {
        this.config = new config(this);
        this.saveDefaultConfig();
        new config(this).testCompareConfig();
    }

    // ------------------------------------------------------------------------------------------------
    // Refresh pipeline
    //
    //  requestRefresh()  -> single-flight guard, callable from any thread
    //    phase 1 (global region thread): reloadConfig + collectSnapshots()
    //        reads Residence data, Bukkit Locations, config values and resolves PlaceholderAPI into a
    //        List<MarkerData>. This is the only part that touches Residence / Bukkit / PAPI and is kept
    //        deliberately light.
    //    phase 2 (async scheduler): applyMarkers()
    //        the heavy part — Shape tessellation, marker construction and writes into BlueMap's
    //        (thread-safe) marker sets.
    // ------------------------------------------------------------------------------------------------

    /** Request a marker refresh from any thread. Coalesces concurrent requests into a single run. */
    public void requestRefresh() {
        if (!this.refreshRunning.compareAndSet(false, true)) {
            this.refreshPending.set(true);
            return;
        }
        this.runRefreshCycle();
    }

    private void runRefreshCycle() {
        // Phase 1 — lightweight snapshot on the global region thread.
        Scheduler.run(() -> {
            List<MarkerData> snapshots;
            try {
                this.reloadConfig();
                snapshots = this.collectSnapshots();
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Failed to snapshot residences for BlueMap markers", t);
                this.finishRefreshCycle();
                return;
            }
            // Phase 2 — heavy geometry + BlueMap writes on the async scheduler.
            Scheduler.runAsync(() -> {
                try {
                    this.applyMarkers(snapshots);
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Failed to apply BlueMap markers", t);
                } finally {
                    this.finishRefreshCycle();
                }
            });
        });
    }

    private void finishRefreshCycle() {
        this.refreshRunning.set(false);
        // If any trigger arrived while we were running, run exactly once more.
        if (this.refreshPending.compareAndSet(true, false)) {
            this.requestRefresh();
        }
    }

    /** Phase 1: read every residence into a flat list of marker snapshots. Runs on the global thread. */
    private List<MarkerData> collectSnapshots() {
        List<MarkerData> out = new ArrayList<MarkerData>();
        if (this.resmgr == null) {
            return out;
        }
        for (String resid : this.resmgr.getResidenceList()) {
            ClaimedResidence res = this.resmgr.getByName(resid);
            if (res == null) {
                continue;
            }
            CuboidArea[] areas = res.getAreaArray();
            List<ClaimedResidence> subzones = res.getSubzones();
            this.snapshotMarkers(out, areas, resid, "r", res);
            if (subzones != null) {
                for (ClaimedResidence subzone : subzones) {
                    this.snapshotMarkers(out, subzone.getAreaArray(), resid, "s", subzone);
                }
            }
        }
        return out;
    }

    private static String cfgKey(String prefix, boolean subzone, String leaf) {
        return subzone ? prefix + ".subzone." + leaf : prefix + "." + leaf;
    }

    private static MarkerData.Kind parseKind(String type) {
        if (type == null) {
            return MarkerData.Kind.RECTANGLE;
        }
        switch (type.toLowerCase()) {
            case "point": return MarkerData.Kind.POINT;
            case "circle": return MarkerData.Kind.CIRCLE;
            case "ellipse": return MarkerData.Kind.ELLIPSE;
            case "rectangle2d": return MarkerData.Kind.RECTANGLE2D;
            case "circle2d": return MarkerData.Kind.CIRCLE2D;
            case "ellipse2d": return MarkerData.Kind.ELLIPSE2D;
            default: return MarkerData.Kind.RECTANGLE;
        }
    }

    /**
     * Build one {@link MarkerData} per cuboid area. Mirrors the original generateMarkers() reads but only
     * produces plain data (no Shape tessellation, no marker objects, no BlueMap writes). Config, colours
     * and the PlaceholderAPI-resolved detail string are identical for every area of a residence, so they
     * are resolved once; only the per-area geometry varies.
     */
    private void snapshotMarkers(List<MarkerData> out, CuboidArea[] areas, String resid, String type, ClaimedResidence res) {
        if (areas == null) {
            return;
        }
        boolean isS = Objects.equals(type, "s");
        boolean isSale = res.isForSell();
        boolean isRent = !isSale && res.isForRent();
        // Config section used for most values.
        String prefix = isSale ? "marker-For_Sale" : (isRent ? "marker-For_Rent" : "marker");
        // depth-test / centerPointerMarkerHeight: the original reads these from the For_Sale section for
        // both the "for sale" and the default (not-for-sale, not-for-rent) markers. Preserved verbatim so
        // existing configs render identically.
        String effectsPrefix = isRent ? "marker-For_Rent" : "marker-For_Sale";
        boolean targetSubzones = isS && !this.MarkerSetLabelResidence.equals(this.MarkerSetLabelSubzones);

        // Detail string (PlaceholderAPI resolved here, on the global thread). Note: the original never
        // appended the per-flag text to the detail (it concatenated an always-empty StringBuilder), so the
        // visible detail is just the template with [ResName]/[OwnerName] substituted. Behaviour preserved.
        String detailTemplate = this.getConfig().getString(cfgKey(prefix, isS, "detail"));
        if (detailTemplate == null) {
            detailTemplate = "";
        }
        detailTemplate = detailTemplate.replace("[ResName]", res.getName());
        detailTemplate = detailTemplate.replace("[OwnerName]", res.getRPlayer().getName());
        String detail = this.Placeholder(res.getRPlayer().getPlayer(), detailTemplate);

        Color line = new Color(
                this.getConfig().getInt(cfgKey(prefix, isS, "LineColor.r"), 255),
                this.getConfig().getInt(cfgKey(prefix, isS, "LineColor.g"), 0),
                this.getConfig().getInt(cfgKey(prefix, isS, "LineColor.b"), 0),
                (float)this.getConfig().getDouble(cfgKey(prefix, isS, "LineColor.a"), 1.0));
        Color fill = new Color(
                this.getConfig().getInt(cfgKey(prefix, isS, "FillColor.r"), 200),
                this.getConfig().getInt(cfgKey(prefix, isS, "FillColor.g"), 0),
                this.getConfig().getInt(cfgKey(prefix, isS, "FillColor.b"), 0),
                (float)this.getConfig().getDouble(cfgKey(prefix, isS, "FillColor.a"), 0.3));
        int lineWidth = this.getConfig().getInt(cfgKey(prefix, isS, "LineWidth"), 3);
        int yHeight = this.getConfig().getInt(cfgKey(prefix, isS, "Yheight"), 60);
        boolean depthTest = this.getConfig().getBoolean(cfgKey(effectsPrefix, isS, "depth-test"), true);
        boolean centerPointer = this.getConfig().getBoolean(cfgKey(effectsPrefix, isS, "centerPointerMarkerHeight"), true);
        String icon = this.getConfig().getString(cfgKey(prefix, isS, "icon.url"), DEFAULT_ICON);
        int anchorX = this.getConfig().getInt(cfgKey(prefix, isS, "icon.anchorX"));
        int anchorY = this.getConfig().getInt(cfgKey(prefix, isS, "icon.anchorY"));
        int points = this.getConfig().getInt(cfgKey(prefix, isS, "points"), 100);
        MarkerData.Kind kind = parseKind(this.getConfig().getString(cfgKey(prefix, isS, "type")));

        for (int i = 0; i < areas.length; ++i) {
            double maxZ;
            double minZ;
            double maxX;
            String areaResid = resid + "%" + type + i;
            Location ll = areas[i].getLowLocation();
            Location lh = areas[i].getHighLocation();
            double dist = ll.distance(lh) / 2.0;
            double minX = ll.getX();
            if (minX > (maxX = lh.getX() + 1.0)) {
                minX = lh.getX();
                maxX = ll.getX();
            }
            if ((minZ = ll.getZ()) > (maxZ = lh.getZ() + 1.0)) {
                minZ = lh.getZ();
                maxZ = ll.getZ();
            }
            double distX = (minX - maxX) / 2.0;
            double distZ = (minZ - maxZ) / 2.0;
            double[] x = new double[2];
            float[] y = new float[2];
            double[] z = new double[2];
            x[0] = minX;
            x[1] = maxX;
            y[0] = (float)ll.getY() + 1.0f;
            y[1] = (float)lh.getY() + 1.0f;
            z[0] = minZ;
            z[1] = maxZ;
            double centX = (x[0] + x[1]) / 2.0;
            double centY = (y[0] + y[1]) / 2.0f;
            double centZ = (z[0] + z[1]) / 2.0;
            double pointY = centY;
            if (centerPointer) {
                pointY = yHeight;
            }

            MarkerData md = new MarkerData();
            md.worldUid = areas[i].getWorld().getUID();
            md.targetSubzones = targetSubzones;
            md.key = areaResid;
            md.kind = kind;
            md.detail = detail;
            md.line = line;
            md.fill = fill;
            md.lineWidth = lineWidth;
            md.depthTest = depthTest;
            md.x = x;
            md.y = y;
            md.z = z;
            md.centX = centX;
            md.centZ = centZ;
            md.dist = dist;
            md.distX = distX;
            md.distZ = distZ;
            md.points = points;
            md.yHeight = yHeight;
            md.pointY = pointY;
            md.icon = icon;
            md.anchorX = anchorX;
            md.anchorY = anchorY;
            out.add(md);
        }
    }

    /** Phase 2: build and write all markers. Runs on the async scheduler; BlueMap's marker API is thread-safe. */
    private void applyMarkers(List<MarkerData> snapshots) {
        final BlueMapAPI api = this.blueMap;
        if (api == null) {
            return;
        }
        for (MarkerData md : snapshots) {
            api.getWorld((Object)md.worldUid).ifPresent(blueWorld -> blueWorld.getMaps().forEach(map -> {
                String setId = md.targetSubzones ? this.MarkerSetIdSubzones : this.MarkerSetIdResidence;
                String setLabel = md.targetSubzones ? this.MarkerSetLabelSubzones : this.MarkerSetLabelResidence;
                MarkerSet markerSet = map.getMarkerSets().getOrDefault(setId, MarkerSet.builder().label(setLabel).build());
                markerSet.getMarkers().put(md.key, this.buildMarker(md));
                map.getMarkerSets().put(setId, markerSet);
            }));
        }
    }

    private Marker buildMarker(MarkerData md) {
        switch (md.kind) {
            case POINT:
                return this.generatePointMarker(md.detail, md.centX, md.pointY, md.centZ, md.icon, md.anchorX, md.anchorY);
            case CIRCLE:
                return this.generateCircleExtrudeMarker(md.detail, md.line, md.fill, md.lineWidth, md.depthTest, md.centX, md.y, md.centZ, md.dist, md.points);
            case ELLIPSE:
                return this.generateEllipseExtrudeMarker(md.detail, md.line, md.fill, md.lineWidth, md.depthTest, md.centX, md.y, md.centZ, md.distX, md.distZ, md.points);
            case RECTANGLE2D:
                return this.generateRectangleExtrudeMarker2D(md.detail, md.line, md.fill, md.lineWidth, md.depthTest, md.x, md.yHeight, md.z);
            case CIRCLE2D:
                return this.generateCircleExtrudeMarker2D(md.detail, md.line, md.fill, md.lineWidth, md.depthTest, md.centX, md.yHeight, md.centZ, md.dist, md.points);
            case ELLIPSE2D:
                return this.generateEllipseExtrudeMarker2D(md.detail, md.line, md.fill, md.lineWidth, md.depthTest, md.centX, md.yHeight, md.centZ, md.distX, md.distZ, md.points);
            case RECTANGLE:
            default:
                return this.generateRectangleExtrudeMarker(md.detail, md.line, md.fill, md.lineWidth, md.depthTest, md.x, md.y, md.z);
        }
    }

    private ExtrudeMarker generateRectangleExtrudeMarker(String detail, Color line, Color fill, int width, boolean depthTest, double[] x, float[] y, double[] z) {
        return ((ExtrudeMarker.Builder)((ExtrudeMarker.Builder)ExtrudeMarker.builder().label(this.res.getName())).detail(detail)).shape(Shape.createRect((double)x[0], (double)z[0], (double)x[1], (double)z[1]), y[0], y[1]).centerPosition().lineColor(line).fillColor(fill).lineWidth(width).depthTestEnabled(depthTest).build();
    }

    private ShapeMarker generateRectangleExtrudeMarker2D(String detail, Color line, Color fill, int width, boolean depthTest, double[] x, int yHeight, double[] z) {
        return ((ShapeMarker.Builder)((ShapeMarker.Builder)ShapeMarker.builder().label(this.res.getName())).detail(detail)).shape(Shape.createRect((double)x[0], (double)z[0], (double)x[1], (double)z[1]), (float)yHeight).centerPosition().lineColor(line).fillColor(fill).lineWidth(width).depthTestEnabled(depthTest).build();
    }

    private POIMarker generatePointMarker(String detail, double centX, double pointY, double centZ, String icon, int anchorX, int anchorY) {
        return ((POIMarker.Builder)((POIMarker.Builder)POIMarker.builder().label(this.res.getName())).detail(detail).position(centX, pointY, centZ)).icon(icon, anchorX, anchorY).build();
    }

    private ExtrudeMarker generateCircleExtrudeMarker(String detail, Color line, Color fill, int width, boolean depthTest, double centX, float[] y, double centZ, double dist, int points) {
        return ((ExtrudeMarker.Builder)((ExtrudeMarker.Builder)ExtrudeMarker.builder().label(this.res.getName())).detail(detail)).shape(Shape.createCircle((double)centX, (double)centZ, (double)dist, (int)points), y[0], y[1]).centerPosition().lineColor(line).fillColor(fill).lineWidth(width).depthTestEnabled(depthTest).build();
    }

    private ShapeMarker generateCircleExtrudeMarker2D(String detail, Color line, Color fill, int width, boolean depthTest, double centX, int yHeight, double centZ, double dist, int points) {
        return ((ShapeMarker.Builder)((ShapeMarker.Builder)ShapeMarker.builder().label(this.res.getName())).detail(detail)).shape(Shape.createCircle((double)centX, (double)centZ, (double)dist, (int)points), (float)yHeight).centerPosition().lineColor(line).fillColor(fill).lineWidth(width).depthTestEnabled(depthTest).build();
    }

    private ExtrudeMarker generateEllipseExtrudeMarker(String detail, Color line, Color fill, int width, boolean depthTest, double centX, float[] y, double centZ, double distX, double distZ, int points) {
        return ((ExtrudeMarker.Builder)((ExtrudeMarker.Builder)ExtrudeMarker.builder().label(this.res.getName())).detail(detail)).shape(Shape.createEllipse((double)centX, (double)centZ, (double)distX, (double)distZ, (int)points), y[0], y[1]).centerPosition().lineColor(line).fillColor(fill).lineWidth(width).depthTestEnabled(depthTest).build();
    }

    private ShapeMarker generateEllipseExtrudeMarker2D(String detail, Color line, Color fill, int width, boolean depthTest, double centX, int yHeight, double centZ, double distX, double distZ, int points) {
        return ((ShapeMarker.Builder)((ShapeMarker.Builder)ShapeMarker.builder().label(this.res.getName())).detail(detail)).shape(Shape.createEllipse((double)centX, (double)centZ, (double)distX, (double)distZ, (int)points), (float)yHeight).centerPosition().lineColor(line).fillColor(fill).lineWidth(width).depthTestEnabled(depthTest).build();
    }

    public String Placeholder(Player player, String text) {
        if (this.papiState) {
            return PlaceholderAPI.setPlaceholders((Player)player, (String)text);
        }
        return text;
    }

    /** Request a refresh (used by the reload command and Residence event listener). */
    public void refreshPl() {
        this.requestRefresh();
    }
}
