package tizio.dev.lle.api;

import tizio.dev.lle.core.client.renderer.LightDataTexture;
import tizio.dev.lle.api.data.SpotlightInstance;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpotlightAPI {

    private static final Set<SpotlightInstance> ACTIVE_SPOTLIGHTS = ConcurrentHashMap.newKeySet();

    /**
     * with it you can register a new spotlight source.
     */
    public static SpotlightInstance registerLight(SpotlightInstance light) {
        if((ACTIVE_SPOTLIGHTS.size() < LightDataTexture.MAX_LIGHTS)) {
            ACTIVE_SPOTLIGHTS.add(light);
        }
        return light;
    }

    /**
     * remove a registered source
     */
    public static void unregisterLight(SpotlightInstance light) {
        ACTIVE_SPOTLIGHTS.remove(light);
    }

    /**
     * gah dayum cleam them up!
     */
    public static void clearAll() {
        ACTIVE_SPOTLIGHTS.clear();
    }

    public static Set<SpotlightInstance> getActiveSpotlights() {
        return Collections.unmodifiableSet(ACTIVE_SPOTLIGHTS);
    }
}

