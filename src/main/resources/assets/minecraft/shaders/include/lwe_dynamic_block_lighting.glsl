uniform int LwePointCount;
uniform int LweSpotCount;
uniform sampler2D LwePointData;
uniform sampler2D LweSpotData;
uniform sampler2DArray LweGoboArray;

vec3 lwe_point_light(vec3 worldPos, vec3 normal, vec3 lightPos, vec4 colorRadius, vec4 params) {
    vec3 toLight = lightPos - worldPos;
    float distanceToLight = length(toLight);
    float radius = max(colorRadius.w, 0.001);
    float attenuation = pow(clamp(1.0 - distanceToLight / radius, 0.0, 1.0), max(params.y, 0.01));
    float ndotl = max(dot(normal, normalize(toLight)), 0.18);
    return colorRadius.rgb * attenuation * ndotl * params.x;
}

vec3 lwe_spot_light(vec3 worldPos, vec3 normal, vec3 lightPos, vec3 lightDir, vec4 colorLength, vec4 params) {
    vec3 fromLight = worldPos - lightPos;
    float distanceToLight = length(fromLight);
    float lengthLimit = max(colorLength.w, 0.001);
    vec3 spotDir = normalize(lightDir);
    float cone = dot(normalize(fromLight), spotDir);
    float coneMask = smoothstep(params.y, params.x, cone);
    float attenuation = pow(clamp(1.0 - distanceToLight / lengthLimit, 0.0, 1.0), max(params.w, 0.01));
    float ndotl = max(dot(normal, normalize(-fromLight)), 0.18);
    return colorLength.rgb * coneMask * attenuation * ndotl * params.z;
}

float lwe_gobo_mask(vec3 worldPos, vec3 lightPos, vec3 lightDir, float outerCos, int goboIndex) {
    if (goboIndex < 0) return 1.0;

    vec3 dir = normalize(lightDir);
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    vec3 right = cross(worldUp, dir);
    if (dot(right, right) < 0.0001) {
        right = cross(vec3(1.0, 0.0, 0.0), dir);
    }
    right = normalize(right);
    vec3 up = normalize(cross(dir, right));

    vec3 toFrag = worldPos - lightPos;
    float alongAxis = dot(toFrag, dir);
    if (alongAxis <= 0.0) return 0.0;

    float clampedOuterCos = clamp(outerCos, 0.001, 0.999);
    float outerSin = sqrt(max(1.0 - clampedOuterCos * clampedOuterCos, 0.0001));
    float coneRadius = alongAxis * (outerSin / clampedOuterCos);

    float u = dot(toFrag, right) / max(coneRadius, 0.0001);
    float v = dot(toFrag, up) / max(coneRadius, 0.0001);

    vec2 uv = vec2(u, v) * 0.5 + 0.5;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 0.0;

    return texture(LweGoboArray, vec3(uv, float(goboIndex))).r;
}

vec3 lwe_apply_dynamic_lights(vec3 baseColor, vec3 worldPos, vec3 normal) {
    vec3 n = normalize(normal);
    vec3 added = vec3(0.0);

    for (int i = 0; i < LwePointCount; i++) {
        vec4 data0 = texelFetch(LwePointData, ivec2(i * 4 + 0, 0), 0);
        vec4 data1 = texelFetch(LwePointData, ivec2(i * 4 + 1, 0), 0);
        vec4 data2 = texelFetch(LwePointData, ivec2(i * 4 + 2, 0), 0);

        vec3 pos = data0.xyz;
        vec4 colorRadius = vec4(data1.xyz, data0.w);
        vec4 params = vec4(data1.w, data2.x, 0.0, 0.0);

        added += lwe_point_light(worldPos, n, pos, colorRadius, params);
    }

    for (int i = 0; i < LweSpotCount; i++) {
        vec4 data0 = texelFetch(LweSpotData, ivec2(i * 4 + 0, 0), 0);
        vec4 data1 = texelFetch(LweSpotData, ivec2(i * 4 + 1, 0), 0);
        vec4 data2 = texelFetch(LweSpotData, ivec2(i * 4 + 2, 0), 0);
        vec4 data3 = texelFetch(LweSpotData, ivec2(i * 4 + 3, 0), 0);

        vec3 pos = data0.xyz;
        vec3 dir = data2.xyz;
        vec4 colorLength = vec4(data1.xyz, data0.w);
        vec4 params = vec4(data3.x, data3.y, data1.w, data2.w);

        int goboIndex = int(round(data3.z));
        float goboMask = lwe_gobo_mask(worldPos, pos, dir, data3.y, goboIndex);

        added += lwe_spot_light(worldPos, n, pos, dir, colorLength, params) * goboMask;
    }

    return baseColor + added * baseColor;
}
