#version 150

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

uniform vec3 CameraPos;
uniform vec3 LightPos;
uniform vec2 FadeParams;

void main() {
    vec2 uv = texCoord * 2.0 - 1.0;
    float dist = length(uv);

    if (dist > 1.0) discard;

    float coreRadius = 0.0015;
    float core = 1.0 - smoothstep(coreRadius * 0.7, coreRadius, dist);

    float bloomDist = max(dist - coreRadius, 0.0);
    float bloom = exp(-bloomDist * 4.0);

    float glow = max(core, bloom * 0.85);

    float distToCam = distance(CameraPos, LightPos);
    float nearFade = 1.0 - smoothstep(FadeParams.x, FadeParams.y, distToCam);

    float intensity = 1.8;
    float alpha = glow * nearFade * vertexColor.a * intensity;

    fragColor = vec4(vertexColor.rgb * alpha, alpha);
}
