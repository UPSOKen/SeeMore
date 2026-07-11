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
    void usesTheGlobalWorldSettingBeforeTheSelectedGroupsDefaultOverride() {
        DistanceProfile override = new DistanceProfile(
                "admin", "seemore.view-distance.admin", new WorldSettings(20, Map.of())
        );

        ResolvedProfile resolved = PermissionProfileResolver.resolve(
                List.of(override), "seemore.view-distance.admin"::equals, defaults, "world_nether"
        );
        ResolvedProfile newWorld = PermissionProfileResolver.resolve(
                List.of(override), "seemore.view-distance.admin"::equals, defaults, "new_world"
        );

        assertEquals("admin", resolved.name());
        assertEquals(8, resolved.maximumViewDistance());
        assertEquals(20, newWorld.maximumViewDistance());
    }

    @Test
    void usesTheSelectedGroupsExactWorldOverrideBeforeTheGlobalWorldSetting() {
        ResolvedProfile resolved = PermissionProfileResolver.resolve(
                List.of(admin), "seemore.view-distance.admin"::equals, defaults, "world_nether"
        );

        assertEquals("admin", resolved.name());
        assertEquals(10, resolved.maximumViewDistance());
    }

    @Test
    void usesGlobalSettingsWhenTheSelectedGroupHasNoApplicableOverride() {
        DistanceProfile override = new DistanceProfile(
                "donor", "seemore.view-distance.donor", new WorldSettings(null, Map.of())
        );

        ResolvedProfile namedWorld = PermissionProfileResolver.resolve(
                List.of(override), "seemore.view-distance.donor"::equals, defaults, "world_nether"
        );
        ResolvedProfile unlistedWorld = PermissionProfileResolver.resolve(
                List.of(override), "seemore.view-distance.donor"::equals, defaults, "new_world"
        );

        assertEquals(8, namedWorld.maximumViewDistance());
        assertEquals(12, unlistedWorld.maximumViewDistance());
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
