#version 150

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

uniform vec3 CameraPos;
uniform vec3 LightPos;
uniform vec3 LightDir;
uniform vec2 FadeParams;

void main() {
    float x = texCoord.x * 2.0 - 1.0;
    float y = texCoord.y;

    float coneEdge = mix(0.015, 1.0, y);
    float r = abs(x) / coneEdge;

    float radial = exp(-r * r * 1.3);

    if (radial < 0.004) discard;

    float distAtten = exp(-y * 1.3);
    float bottomFade = 1.0 - smoothstep(0.35, 1.0, y);
    float coneBody = radial * distAtten * bottomFade;

    float glowCore = exp(-y * 13.0) * exp(-r * r * 0.5);
    float glowAura = exp(-y * 3.8) * exp(-r * r * 0.16);
    float sourceGlow = glowCore * 0.8 + glowAura * 0.4;

    vec3 finalColor = vertexColor.rgb;

    vec3 lightToCam = normalize(CameraPos - LightPos);
    vec3 dirNorm = normalize(LightDir);
    float viewAlign = dot(lightToCam, dirNorm);
    float dirFade = smoothstep(-0.05, 0.55, viewAlign);

    float distToCam = distance(CameraPos, LightPos);
    float nearFade = 1.0 - smoothstep(FadeParams.x, FadeParams.y, distToCam);

    float alpha = (sourceGlow * 0.0 + coneBody * 0.45) * vertexColor.a * nearFade * dirFade;

    fragColor = vec4(finalColor, clamp(alpha, 0.0, 1.0));
}
