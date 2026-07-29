#version 150

#moj_import <fog.glsl>
#moj_import <lle_dynamic_block_lighting.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec4 normal;
in vec3 lleWorldPos;
in vec3 lleNormal;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    color.rgb = lle_apply_dynamic_lights(color.rgb, lleWorldPos, lleNormal);
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
