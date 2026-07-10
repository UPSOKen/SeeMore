package com.froobworld.seemore.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionProfileResolverTest {
    private final WorldSettings defaults = new WorldSettings(12, Map.of("world_nether", 8));
    private final DistanceProfile admin = new DistanceProfile(
            "admin", "seemore.view-distance.admin", new WorldSettings(-1, Map.of("world_nether", 10))
    );
    private final DistanceProfile donor = new DistanceProfile(
            "donor", "seemore.view-distance.donor", new WorldSettings(18, Map.of("world_nether", 10))
    );

    @Test
    void choosesTheFirstMatchingProfileInConfigurationOrder() {
        Set<String> permissions = Set.of("seemore.view-distance.admin", "seemore.view-distance.donor");

        ResolvedProfile resolved = PermissionProfileResolver.resolve(
                List.of(admin, donor), permissions::contains, defaults, "world"
        );

        assertEquals("admin", resolved.name());
        assertEquals(-1, resolved.maximumViewDistance());
    }

    @Test
    void usesTheSelectedProfilesWorldFallback() {
        ResolvedProfile resolved = PermissionProfileResolver.resolve(
                List.of(admin, donor), "seemore.view-distance.donor"::equals, defaults, "unlisted_world"
        );

        assertEquals("donor", resolved.name());
        assertEquals(18, resolved.maximumViewDistance());
    }

    @Test
    void usesGlobalDefaultsWhenNoProfileMatches() {
        ResolvedProfile resolved = PermissionProfileResolver.resolve(
                List.of(admin, donor), permission -> false, defaults, "world_nether"
        );

        assertEquals("default", resolved.name());
        assertEquals(8, resolved.maximumViewDistance());
    }
}
