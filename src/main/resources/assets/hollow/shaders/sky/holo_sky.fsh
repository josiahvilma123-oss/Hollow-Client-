#version 330

// Holo Sky Studio: a layered procedural sky drawn in a single full-screen pass.
// Layers, back to front: gradient, texture layer, nebula, stars, planets, sun/moon/eclipse,
// celestial ring, horizon silhouettes. All artwork is generated here; nothing is sampled
// except the optional resource-pack texture layer.

uniform sampler2D SkyTexture;

layout(std140) uniform HoloSky {
    mat4 InvViewProj;
    vec4 Zenith;        // rgb, a = brightness
    vec4 Horizon;       // rgb, a = gradient curve
    vec4 Ground;        // rgb, a = saturation (intensity)
    vec4 Glow;          // rgb, a = strength
    vec4 SunDir;        // xyz, w = angular radius
    vec4 SunColor;      // rgb, a = glow
    vec4 MoonDir;       // xyz, w = angular radius
    vec4 MoonColor;     // rgb, a = glow
    vec4 Styles;        // x sun style, y moon style, z moon phase (-1..1), w celestial visibility
    vec4 Stars;         // x density, y size, z brightness, w twinkle
    vec4 StarColor;     // rgb, a = colour variance
    vec4 NebulaA;       // rgb, a = intensity (0 = off)
    vec4 NebulaB;       // rgb, a = scale
    vec4 PlanetA;       // xyz dir (star space), w = angular radius (0 = off)
    vec4 PlanetAColor;  // rgb, a = ring
    vec4 PlanetB;
    vec4 PlanetBColor;
    vec4 Ring;          // x radius, y ornaments, z width, w enabled
    vec4 RingColor;     // rgb
    vec4 Silhouette;    // rgb, a = style (0 none, 1 pyramids, 2 obelisks, 3 spires)
    vec4 Misc;          // x time (s), y star rotation angle, z detail (0..2), w texture opacity
};

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i);
    float n100 = hash13(i + vec3(1, 0, 0));
    float n010 = hash13(i + vec3(0, 1, 0));
    float n110 = hash13(i + vec3(1, 1, 0));
    float n001 = hash13(i + vec3(0, 0, 1));
    float n101 = hash13(i + vec3(1, 0, 1));
    float n011 = hash13(i + vec3(0, 1, 1));
    float n111 = hash13(i + vec3(1, 1, 1));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float fbm(vec3 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        if (i >= octaves) break;
        sum += amp * valueNoise(p);
        p = p * 2.03 + vec3(17.1, 9.2, 3.7);
        amp *= 0.5;
    }
    return sum;
}

// Rotation of the star field around the celestial axis (world X, as in vanilla).
vec3 toStarSpace(vec3 d, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec3(d.x, c * d.y + s * d.z, -s * d.y + c * d.z);
}

vec3 starLayer(vec3 d, float scale, float threshold, float radius, float time) {
    vec3 p = d * scale;
    vec3 cell = floor(p);
    vec3 h = hash33(cell);
    if (h.x > threshold) return vec3(0.0);
    vec3 centre = normalize(cell + 0.2 + 0.6 * hash33(cell + 7.7));
    float dist = length(d - centre) * scale;
    float r = radius * (0.45 + h.y);
    float core = 1.0 - smoothstep(0.0, r, dist);
    float halo = exp(-dist / (r * 2.5)) * 0.25;
    float twinkle = 1.0 - Stars.w * 0.5 * (0.5 + 0.5 * sin(time * (1.5 + h.z * 4.0) + h.y * 40.0));
    vec3 warm = vec3(1.0, 0.82, 0.62);
    vec3 cool = vec3(0.68, 0.8, 1.0);
    vec3 tint = mix(StarColor.rgb, h.z > 0.5 ? warm : cool, StarColor.a * h.y);
    // Most stars are faint; only a few are bright, as in a real night sky.
    return tint * (core + halo) * twinkle * (0.25 + 1.6 * h.z * h.z * h.z);
}

void basis(vec3 n, out vec3 u, out vec3 v) {
    vec3 up = abs(n.y) < 0.95 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    u = normalize(cross(up, n));
    v = cross(n, u);
}

// Returns rgb + alpha for a celestial body of the given style.
vec4 body(vec3 d, vec3 dir, float radius, vec3 color, float glow, float style, float phase, float time) {
    if (style < 0.5 || radius <= 0.0) return vec4(0.0);
    // Skip pixels beyond the reach of the body's glow (halo rays reach furthest).
    float reach = radius * (style > 1.5 && style < 2.5 ? 26.0 : 10.0);
    float cosD = dot(d, dir);
    if (reach < PI && cosD < cos(reach)) return vec4(0.0);
    float a = acos(clamp(cosD, -1.0, 1.0));
    float disc = 1.0 - smoothstep(radius * 0.94, radius, a);
    vec3 u;
    vec3 v;
    basis(dir, u, v);
    vec2 q = vec2(dot(d, u), dot(d, v)) / max(radius, 1e-4);
    float glowAmount = glow * exp(-max(a - radius, 0.0) / (radius * 1.4)) * 0.8;

    if (style > 3.5) {
        // Eclipse: a dark disc with a ragged gold-and-violet corona.
        float phi = atan(q.y, q.x + 1e-6);
        float rays = 0.55 + 0.45 * valueNoise(vec3(phi * 3.0, time * 0.15, 1.0));
        float corona = exp(-max(a - radius, 0.0) / (radius * (0.35 + 0.5 * rays))) * (1.0 - disc);
        vec3 coronaColor = mix(color, vec3(0.55, 0.3, 0.95), 0.35 + 0.35 * sin(phi * 2.0 + time * 0.1));
        float rim = (1.0 - smoothstep(radius, radius * 1.06, a)) * smoothstep(radius * 0.96, radius, a);
        vec3 rgb = coronaColor * (corona * (0.9 + glow) + rim * 2.0) + vec3(0.004, 0.002, 0.006) * disc;
        return vec4(rgb, max(disc, clamp(corona + rim, 0.0, 1.0)));
    }
    vec3 rgb = color;
    if (style > 2.5) {
        // Crescent: |phase| is the lit fraction (0 new, 1 full); its sign picks the lit side.
        float k = abs(phase);
        float side = phase >= 0.0 ? 1.0 : -1.0;
        float terminator = 1.0 - 2.0 * k;
        float lit = smoothstep(terminator - 0.12, terminator + 0.12, q.x * side);
        float surface = 0.85 + 0.15 * valueNoise(vec3(q * 6.0, 3.0));
        rgb = color * mix(0.06, 1.0, lit) * surface;
    } else if (style > 1.5) {
        // Halo: soft disc with long, faint rays.
        float phi = atan(q.y, q.x + 1e-6);
        glowAmount += glow * 0.35 * exp(-a / (radius * 4.0)) * (0.6 + 0.4 * sin(phi * 12.0));
    } else {
        rgb = color * (0.92 + 0.08 * valueNoise(vec3(q * 5.0, 1.0)));
    }
    return vec4(rgb * disc + color * glowAmount, clamp(disc + glowAmount, 0.0, 1.0));
}

vec4 planet(vec3 d, vec4 info, vec4 colorInfo, vec3 sunDir) {
    if (info.w <= 0.0) return vec4(0.0);
    vec3 dir = normalize(info.xyz);
    // The ring reaches 2.25 radii; nothing is drawn beyond that.
    if (dot(d, dir) < cos(min(info.w * 2.4, 1.5))) return vec4(0.0);
    vec3 u;
    vec3 v;
    basis(dir, u, v);
    vec2 q = vec2(dot(d, u), dot(d, v)) / info.w;
    float r2 = dot(q, q);
    vec4 result = vec4(0.0);
    // Ring behind the planet.
    if (colorInfo.a > 0.5) {
        vec2 rq = mat2(0.94, -0.34, 0.34, 0.94) * q;
        float e = length(vec2(rq.x, rq.y / 0.28));
        float band = smoothstep(1.35, 1.45, e) * (1.0 - smoothstep(2.1, 2.25, e)) * (0.6 + 0.4 * sin(e * 30.0));
        if (rq.y > 0.0 || r2 > 1.0) {
            result = vec4(colorInfo.rgb * 0.9, band * 0.7);
        }
    }
    if (r2 < 1.0) {
        vec3 n = normalize(u * q.x + v * q.y + dir * sqrt(1.0 - r2));
        float light = 0.12 + 0.88 * max(dot(n, normalize(sunDir + dir * 0.3)), 0.0);
        float bands = 0.85 + 0.15 * sin(q.y * 14.0 + valueNoise(vec3(q * 3.0, 2.0)) * 3.0);
        float edge = 1.0 - smoothstep(0.96, 1.0, r2);
        vec3 surface = colorInfo.rgb * light * bands;
        result = vec4(mix(result.rgb * result.a, surface, edge), max(result.a, edge));
        if (colorInfo.a > 0.5) {
            vec2 rq = mat2(0.94, -0.34, 0.34, 0.94) * q;
            float e = length(vec2(rq.x, rq.y / 0.28));
            float band = smoothstep(1.35, 1.45, e) * (1.0 - smoothstep(2.1, 2.25, e)) * (0.6 + 0.4 * sin(e * 30.0));
            if (rq.y < 0.0) {
                result.rgb = mix(result.rgb, colorInfo.rgb * 0.9, band * 0.7);
            }
        }
    }
    return result;
}

float silhouetteHeight(float az, float style) {
    float h = 0.0;
    if (style < 1.5) {
        // Pyramids: a few large and small triangles along the horizon.
        for (int i = 0; i < 4; i++) {
            float fi = float(i);
            float freq = 1.6 + fi * 1.3;
            float cell = floor(az * freq / (2.0 * PI) * 3.0);
            float local = fract(az * freq / (2.0 * PI) * 3.0);
            float rnd = hash13(vec3(cell, fi, 4.2));
            if (rnd < 0.45) continue;
            float height = (0.05 + 0.08 * rnd) / (1.0 + fi * 0.5);
            h = max(h, height * max(0.0, 1.0 - abs(local - 0.5) * 2.6));
        }
    } else if (style < 2.5) {
        // Obelisks and temple roofs.
        float cell = floor(az * 18.0);
        float local = fract(az * 18.0);
        float rnd = hash13(vec3(cell, 2.0, 9.1));
        if (rnd > 0.7) {
            float width = 0.08 + 0.04 * rnd;
            float column = step(abs(local - 0.5), width);
            float tip = max(0.0, 1.0 - abs(local - 0.5) / width) * 0.02;
            h = column * (0.07 + 0.06 * rnd) + column * tip;
        } else if (rnd > 0.45) {
            float roof = step(0.1, local) * step(local, 0.9);
            h = roof * (0.025 + 0.02 * rnd);
        }
    } else {
        // Ice spires: thin, sharp peaks.
        for (int i = 0; i < 3; i++) {
            float fi = float(i);
            float freq = 9.0 + fi * 7.0;
            float cell = floor(az * freq);
            float local = fract(az * freq);
            float rnd = hash13(vec3(cell, fi, 7.3));
            float height = (0.03 + 0.09 * rnd * rnd) / (1.0 + fi * 0.7);
            h = max(h, height * max(0.0, 1.0 - abs(local - 0.5) * 5.0));
        }
    }
    return h;
}

void main() {
    vec4 clip = InvViewProj * vec4(texCoord * 2.0 - 1.0, 1.0, 1.0);
    vec3 d = normalize(clip.xyz / clip.w);
    float time = Misc.x;
    int octaves = int(Misc.z) + 2;
    float visibility = Styles.w;

    // 1. Gradient.
    float up = clamp(d.y, 0.0, 1.0);
    vec3 color = mix(Horizon.rgb, Zenith.rgb, pow(max(up, 1e-6), Horizon.a));
    color = mix(color, Ground.rgb, 1.0 - smoothstep(-0.25, 0.0, d.y));
    vec2 flatDir = normalize(d.xz + 1e-5);
    vec2 sunFlat = normalize(SunDir.xz + 1e-5);
    float toward = 0.45 + 0.55 * max(dot(flatDir, sunFlat), 0.0);
    color += Glow.rgb * Glow.a * exp(-abs(d.y) * 7.0) * toward;

    vec3 sd = toStarSpace(d, Misc.y);

    // 2. Texture layer (equirectangular, from a resource pack).
    if (Misc.w > 0.0) {
        float az = atan(sd.z, sd.x + 1e-6);
        vec2 uv = vec2(az / (2.0 * PI) + 0.5, 0.5 - asin(clamp(sd.y, -1.0, 1.0)) / PI);
        vec4 tex = texture(SkyTexture, uv);
        color = mix(color, color + tex.rgb, tex.a * Misc.w);
    }

    // 3. Nebula, concentrated in a band like a galactic plane.
    if (NebulaA.a > 0.0 && octaves > 2) {
        vec3 p = sd * (2.2 * NebulaB.a);
        float plane = dot(sd, normalize(vec3(0.3, 1.0, 0.45))) * 2.2;
        float band = exp(-plane * plane);
        if (band > 0.01) {
            float n = fbm(p + vec3(0.0, time * 0.003, 0.0), octaves);
            float density = pow(clamp(n * 1.35 - 0.25, 0.0, 1.0), 2.2) * band;
            if (density > 0.002) {
                // The colour mix is low-frequency, so one noise octave is enough, and only where there is nebula.
                float n2 = valueNoise(p * 1.7 + 11.0);
                color += mix(NebulaA.rgb, NebulaB.rgb, clamp(n2 * 1.6 - 0.3, 0.0, 1.0)) * density * NebulaA.a * visibility;
            }
        }
    }

    // 4. Stars (two layers; the fine layer is skipped on low detail).
    if (Stars.x > 0.0 && Stars.z > 0.0 && visibility > 0.0) {
        vec3 stars = starLayer(sd, 90.0, Stars.x * 0.16, 0.12 * Stars.y, time);
        if (octaves > 2) {
            stars += starLayer(sd, 210.0, Stars.x * 0.07, 0.14 * Stars.y, time + 3.0) * 0.5;
        }
        float horizonFade = smoothstep(-0.02, 0.12, d.y);
        color += stars * Stars.z * visibility * horizonFade;
    }

    // 5. Planets.
    vec4 pa = planet(sd, PlanetA, PlanetAColor, SunDir.xyz);
    color = mix(color, pa.rgb, pa.a * visibility);
    vec4 pb = planet(sd, PlanetB, PlanetBColor, SunDir.xyz);
    color = mix(color, pb.rgb, pb.a * visibility);

    // 6. Sun and moon.
    vec4 sun = body(d, SunDir.xyz, SunDir.w, SunColor.rgb, SunColor.a, Styles.x, 1.0, time);
    color = color * (1.0 - sun.a * 0.9) + sun.rgb;
    vec4 moon = body(d, MoonDir.xyz, MoonDir.w, MoonColor.rgb, MoonColor.a, Styles.y, Styles.z, time);
    color = color * (1.0 - moon.a * 0.9) + moon.rgb * max(visibility, 0.35);

    // 7. Celestial ring with glyph ornaments around the moon.
    if (Ring.w > 0.5 && dot(d, MoonDir.xyz) > cos(min(Ring.x * 1.25 + Ring.z * 4.0, 3.0))) {
        float a = acos(clamp(dot(d, MoonDir.xyz), -1.0, 1.0));
        float width = Ring.z;
        float line = 1.0 - smoothstep(0.0, width, abs(a - Ring.x));
        float outer = (1.0 - smoothstep(0.0, width * 0.6, abs(a - Ring.x * 1.12))) * 0.5;
        float ornaments = 0.0;
        if (Ring.y > 0.5) {
            vec3 u;
            vec3 v;
            basis(MoonDir.xyz, u, v);
            float phi = atan(dot(d, v), dot(d, u)) + time * 0.02;
            float seg = fract(phi / (2.0 * PI) * Ring.y);
            float radial = abs(a - Ring.x * 1.06);
            float glyph = step(abs(seg - 0.5), 0.07) * step(radial, width * 3.5);
            float eye = 1.0 - smoothstep(0.0, width * 2.0, length(vec2((seg - 0.5) * 0.6, radial * 1.5 - width)));
            ornaments = max(glyph * 0.8, eye);
        }
        color += RingColor.rgb * (line + outer + ornaments) * 0.8 * max(visibility, 0.4);
    }

    // 8. Distant silhouettes on the horizon.
    // Silhouettes never rise above ~0.14 rad, so higher pixels skip the trig.
    if (Silhouette.a > 0.5 && d.y < 0.16 && d.y > -0.3) {
        float az = atan(d.z, d.x + 1e-6);
        float elevation = asin(clamp(d.y, -1.0, 1.0));
        float h = silhouetteHeight(az, Silhouette.a);
        if (elevation < h && elevation > -0.3) {
            float rim = smoothstep(h - 0.004, h, elevation);
            color = Silhouette.rgb + Glow.rgb * rim * 0.35 * Glow.a;
        }
    }

    // Brightness and saturation.
    // Never let an undefined value (driver-dependent maths at singular points) reach the screen.
    if (any(isnan(color)) || any(isinf(color))) {
        color = Horizon.rgb;
    }
    color *= Zenith.a;
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(luma), color, Ground.a);
    fragColor = vec4(max(color, 0.0), 1.0);
}
