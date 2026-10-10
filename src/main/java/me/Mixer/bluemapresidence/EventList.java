/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bekvon.bukkit.residence.event.ResidenceAreaAddEvent
 *  com.bekvon.bukkit.residence.event.ResidenceAreaDeleteEvent
 *  com.bekvon.bukkit.residence.event.ResidenceCreationEvent
 *  com.bekvon.bukkit.residence.event.ResidenceDeleteEvent
 *  com.bekvon.bukkit.residence.event.ResidenceFlagChangeEvent
 *  com.bekvon.bukkit.residence.event.ResidenceOwnerChangeEvent
 *  com.bekvon.bukkit.residence.event.ResidenceRenameEvent
 *  com.bekvon.bukkit.residence.event.ResidenceSizeChangeEvent
 *  com.bekvon.bukkit.residence.event.ResidenceSubzoneCreationEvent
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 */
package me.Mixer.bluemapresidence;

import com.bekvon.bukkit.residence.event.ResidenceAreaAddEvent;
import com.bekvon.bukkit.residence.event.ResidenceAreaDeleteEvent;
import com.bekvon.bukkit.residence.event.ResidenceCreationEvent;
import com.bekvon.bukkit.residence.event.ResidenceDeleteEvent;
import com.bekvon.bukkit.residence.event.ResidenceFlagChangeEvent;
import com.bekvon.bukkit.residence.event.ResidenceOwnerChangeEvent;
import com.bekvon.bukkit.residence.event.ResidenceRenameEvent;
import com.bekvon.bukkit.residence.event.ResidenceSizeChangeEvent;
import com.bekvon.bukkit.residence.event.ResidenceSubzoneCreationEvent;
import me.Mixer.bluemapresidence.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EventList
implements Listener {
    static Main plugin;

    public EventList(Main instance) {
        plugin = instance;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceCreate(ResidenceCreationEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceFlagChange(ResidenceFlagChangeEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceDelete(ResidenceDeleteEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceOwnerChange(ResidenceOwnerChangeEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceRename(ResidenceRenameEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceAreaAdd(ResidenceAreaAddEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceAreaDelete(ResidenceAreaDeleteEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceSizeChange(ResidenceSizeChangeEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceSubzoneCreate(ResidenceSubzoneCreationEvent event) {
        plugin.refreshPl();
    }
}

