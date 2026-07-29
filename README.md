# Light-Lab Engine API Documentation

An API for **Minecraft Forge 1.20.1** (Mod ID: `lle`) that allows mod developers to create, manipulate, and render real-time volumetric spotlights with customizable vectors, colors, lens flares, dynamic block collision raycasting, and texture gobo projections.

---

## 📑 Table of Contents
- [Features](#-features)
- [Quick Start](#-quick-start)
  - [1. Creating & Registering a Spotlight](#1-creating--registering-a-spotlight)
  - [2. Using Gobo Textures (Projected Patterns)](#2-using-gobo-textures-projected-patterns)
  - [3. Unregistering & Cleanup](#3-unregistering--cleanup)
- [Updating Spotlights (Single & Batch)](#-updating-spotlights-single--batch)
  - [1. How Value Updates Work](#1-how-value-updates-work)
  - [2. Updating a Single Spotlight](#2-updating-a-single-spotlight)
  - [3. Updating All Active Spotlights](#3-updating-all-active-spotlights)
  - [4. Complete Animated Spotlight Example](#4-complete-animated-spotlight-example)
  - [5. Recommended Instance Setters](#5-recommended-instance-setters)
- [API Documentation](#-api-documentation)
  - [SpotlightAPI](#spotlightapi)
  - [SpotlightInstance](#spotlightinstance)
  - [GoboTextureManager](#gobotexturemanager)
- [Architecture & Mechanics](#-architecture--mechanics)
- [Best Practices & Limitations](#-best-practices--limitations)

---

## Features

- **Volumetric Beams & Origin Lens Flares:** Renders real-time billboard lens flares and volumetric light beams in screen space and world space.
- **Dynamic Collision Raycasting:** Active beams raycast against solid blocks in real-time (`SpotlightRaytracer`), dynamically shortening the visual beam when hitting obstacles.
- **Gobo Texture Projections:** Supports texture array masks (`GoboTextureManager`) to project logos, shadows, or custom light masks.
- **Shader Pipeline Integration:** Injects light uniforms directly into vanilla Minecraft block, cutout, translucent, and entity shaders via Mixin.
- **High Performance:** Utilizes OpenGL 2D Texture Arrays (`RGBA32F`) to compute up to **2048 active spotlights** concurrently on the GPU.

---

## Quick Start

### 1. Creating & Registering a Spotlight

To spawn a spotlight in the world, construct a `SpotlightInstance` with your target coordinates, vector direction, color, and beam dimensions, then register it using `SpotlightAPI.registerLight()`.

```java
import org.joml.Vector3f;
import org.joml.Vector4f;
import tizio.dev.lwe.api.SpotlightAPI;
import tizio.dev.lwe.api.data.SpotlightInstance;

// 1. Position in world coordinates (X, Y, Z)
Vector3f position = new Vector3f(100.5f, 75.0f, -200.5f);

// 2. Pointing direction (automatically normalized internally)
Vector3f direction = new Vector3f(0.0f, -1.0f, 0.5f); 

// 3. RGBA Color (Values from 0.0f to 1.0f; Alpha acts as light intensity)
Vector4f color = new Vector4f(1.0f, 0.8f, 0.2f, 1.0f); // Warm Golden Yellow

// 4. Dimensions and distances
float flareSize = 1.2f;        // Origin flare size
float minFadeDist = 1.0f;      // Near distance fade start
float maxFadeDist = 32.0f;     // Maximum reach / Raycast distance
float beamLength = 32.0f;      // Default visual beam length
float beamRadius = 3.5f;       // Beam cone spread radius

// Create instance
SpotlightInstance light = new SpotlightInstance(
    position,
    direction,
    color,
    flareSize,
    minFadeDist,
    maxFadeDist,
    beamLength,
    beamRadius
);

// Register the light source
SpotlightAPI.registerLight(light);
```

---

### 2. Using Gobo Textures (Projected Patterns)

A **Gobo** (Go-Between / Optical Mask) allows you to project custom textures (such as searchlight patterns, grate shadows, or symbols) through the spotlight beam.

```java
import net.minecraft.resources.ResourceLocation;
import tizio.dev.lwe.api.SpotlightAPI;
import tizio.dev.lwe.api.data.SpotlightInstance;
import tizio.dev.lwe.core.client.renderer.GoboTextureManager;

// 1. Register your texture (from assets/yourmod/textures/gobo/custom_pattern.png)
ResourceLocation goboTexture = new ResourceLocation("yourmod", "textures/gobo/custom_pattern.png");
int goboIndex = GoboTextureManager.register(goboTexture);

// 2. Pass goboIndex into the SpotlightInstance constructor
SpotlightInstance goboLight = new SpotlightInstance(
    position,
    direction,
    color,
    flareSize,
    minFadeDist,
    maxFadeDist,
    beamLength,
    beamRadius,
    goboIndex // Custom texture layer index
);

SpotlightAPI.registerLight(goboLight);
```

---

### 3. Unregistering & Cleanup

To prevent memory leaks and unnecessary render calculations, always unregister light sources when blocks are destroyed, entities despawn, or chunk/world unloads.

```java
// Unregister a specific light source
SpotlightAPI.unregisterLight(light);

// Clear all active lights (e.g. when leaving a world level)
SpotlightAPI.clearAll();
```

---

## Updating Spotlights (Single & Batch)

### 1. How Value Updates Work

`SpotlightAPI` maintains direct in-memory references to registered `SpotlightInstance` objects inside a `ConcurrentHashMap` set. 

**You do NOT need to unregister and re-register a light to update its properties.** The rendering pipeline (`SpotlightRenderHandler`, `LightDataTexture`, and `SpotlightRaytracer`) reads property values directly from each `SpotlightInstance` on every frame and client tick. Any setter modification takes effect immediately on the next render pass.

---

### 2. Updating a Single Spotlight

Keep a reference to the `SpotlightInstance` returned when registering the light (e.g. inside a `BlockEntity`, Entity, or tick handler), and call the desired setter methods.

```java
// Save reference during registration
SpotlightInstance myLight = SpotlightAPI.registerLight(new SpotlightInstance(...));

// Inside your tick or update loop:
public void updateLightPositionAndDirection(Vector3f newPos, Vector3f newDir) {
    // Updates location in world space
    myLight.setPosition(newPos);
    
    // Updates direction vector (automatically normalized)
    myLight.setDirection(newDir);
    
    // Switch projected pattern dynamically
    myLight.setGoboIndex(newGoboIndex);
}
```

---

### 3. Updating All Active Spotlights

To apply global modifications (such as weather-based intensity adjustments, global color shifts, or strobe effects), iterate through all active spotlights via `SpotlightAPI.getActiveSpotlights()`.

```java
import tizio.dev.lwe.api.SpotlightAPI;
import tizio.dev.lwe.api.data.SpotlightInstance;

public void updateAllSpotlightsGlobally(float time) {
    for (SpotlightInstance light : SpotlightAPI.getActiveSpotlights()) {
        // Example: Apply a subtle swaying motion to every active spotlight
        Vector3f currentDir = light.getDirection();
        float wobbleX = currentDir.x() + (float) Math.sin(time) * 0.01f;
        float wobbleZ = currentDir.z() + (float) Math.cos(time) * 0.01f;
        
        light.setDirection(new Vector3f(wobbleX, currentDir.y(), wobbleZ));
    }
}
```
---

### 4. Recommended Instance Setters

By default, `SpotlightInstance` includes setters for `position`, `direction`, `visualBeamLength`, and `goboIndex`. To enable dynamic adjustments of all parameters (color, size, beam spread), add these additional setters to `SpotlightInstance.java`:

```java
// Add to SpotlightInstance.java for full dynamic control:
public void setColor(Vector4f color) { this.color = color; }
public void setSize(float size) { this.size = size; }
public void setMinFadeDist(float minFadeDist) { this.minFadeDist = minFadeDist; }
public void setMaxFadeDist(float maxFadeDist) { this.maxFadeDist = maxFadeDist; }
public void setBeamLength(float beamLength) { this.beamLength = beamLength; }
public void setBeamRadius(float beamRadius) { this.beamRadius = beamRadius; }
```

---

## API Documentation

### `SpotlightAPI`
`tizio.dev.lwe.api.SpotlightAPI`

Primary entry point for managing light sources.

| Method | Return Type | Description |
| :--- | :--- | :--- |
| `registerLight(SpotlightInstance light)` | `SpotlightInstance` | Registers a spotlight. Returns the instance. Max capacity: `2048`. |
| `unregisterLight(SpotlightInstance light)` | `void` | Removes a registered light source from the active render queue. |
| `clearAll()` | `void` | Clears all registered active spotlights. |
| `getActiveSpotlights()` | `Set<SpotlightInstance>` | Returns an unmodifiable set of all active spotlights. |

---

### `SpotlightInstance`
`tizio.dev.lwe.api.data.SpotlightInstance`

Holds state data for a single spotlight source.

#### Constructors
```java
public SpotlightInstance(Vector3f position, Vector3f direction, Vector4f color, float size, float minFadeDist, float maxFadeDist, float beamLength, float beamRadius)
```
```java
public SpotlightInstance(Vector3f position, Vector3f direction, Vector4f color, float size, float minFadeDist, float maxFadeDist, float beamLength, float beamRadius, int goboIndex)
```

#### Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `position` | `Vector3f` | Absolute world space coordinates `(x, y, z)` of the light origin. |
| `direction` | `Vector3f` | Direction vector. Is normalized automatically upon setting. |
| `color` | `Vector4f` | Color in RGBA `(0.0f - 1.0f)`. `Alpha (w)` acts as light intensity multiplier. |
| `size` | `float` | Scale size of the origin flare billboard quad. |
| `minFadeDist` | `float` | Near camera/surface distance threshold for attenuation fading. |
| `maxFadeDist` | `float` | Maximum beam reach distance used for raytracing block collisions. |
| `beamLength` | `float` | Base projected length of the volumetric cone in shaders. |
| `beamRadius` | `float` | Spread radius at the base cone of the spotlight beam. |
| `goboIndex` | `int` | *(Optional)* Texture layer index from `GoboTextureManager`. `-1` for default/none. |

#### Getters & Setters
- `getPosition()`, `setPosition(Vector3f)`
- `getDirection()`, `setDirection(Vector3f)`
- `getColor()` *(+ optional `setColor(Vector4f)`)*
- `getSize()` *(+ optional `setSize(float)`)*
- `getMinFadeDist()`, `getMaxFadeDist()`
- `getBeamLength()`, `getBeamRadius()`, `getVisualBeamLength()`, `setVisualBeamLength(float)`
- `getGoboIndex()`, `setGoboIndex(int)`

---

### `GoboTextureManager`
`tizio.dev.lwe.core.client.renderer.GoboTextureManager`

Manages 2D Texture Array (`GL_TEXTURE_2D_ARRAY`) registration for light projection patterns.

| Method | Return Type | Description |
| :--- | :--- | :--- |
| `register(ResourceLocation texture)` | `int` | Loads and uploads a PNG texture into the GPU texture array. Returns the assigned layer index (`0` to `2047`). Returns `-1` on failure or when full. |

---

## Architecture & Mechanics

1. **Client-Side Rendering Hook (`SpotlightRenderHandler`):** Listens to Forge event `RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS` to render volumetric quads and flare quads with additive blending (`GL_SRC_ALPHA`, `GL_ONE`).
2. **Raytracing Collision Engine (`SpotlightRaytracer`):** Runs on client tick (`TickEvent.ClientTickEvent`). Executes `level.clip()` raycasts from each light origin along its direction vector to find block collisions (`ClipContext.Block.COLLIDER`). Dynamically updates `visualBeamLength` so beams terminate cleanly against solid structures.
3. **GPU Data Packing (`LightDataTexture`):** Stores position, direction, color, angles, intensity, and gobo indices inside OpenGL floating-point texture buffers (`GL_RGBA32F` on texture slots `10` and `11`).
4. **Shader Dynamic Uniforms (`DynamicLightUniforms` & `ShaderInstanceMixin`):** Injects uniform setters into vanilla block shaders (`rendertype_solid`, `rendertype_cutout`, `rendertype_translucent`, etc.) so surrounding terrain, blocks, and entities are lit dynamically in real-time.

---

## Best Practices & Limitations

- **Maximum Capacity:** The API supports up to **2048 spotlights** concurrently (`LightDataTexture.MAX_LIGHTS`).
- **Client Side Context:** `SpotlightAPI` render routines operate exclusively on the client thread (`Dist.CLIENT`). Ensure calls involving API data structures occur on client logic or are safely guarded by `level.isClientSide()`.
- **Proper Unregistration:** Always unregister light sources during `onChunkUnload`, `setRemoved()` in BlockEntities, or when despawning entities to prevent orphaned render entries.
