#version 150

#define M_PI 3.1415926535897932384626433832795

#moj_import <fog.glsl>

const int cosmiccount = 10;
const int cosmicoutof = 101;
const float lightmix = 0.2f;

const vec4 cosmicuvs[10] = vec4[10](
vec4(0.0, 0.0, 1.0, 0.1),
vec4(0.0, 0.1, 1.0, 0.2),
vec4(0.0, 0.2, 1.0, 0.3),
vec4(0.0, 0.3, 1.0, 0.4),
vec4(0.0, 0.4, 1.0, 0.5),
vec4(0.0, 0.5, 1.0, 0.6),
vec4(0.0, 0.6, 1.0, 0.7),
vec4(0.0, 0.7, 1.0, 0.8),
vec4(0.0, 0.8, 1.0, 0.9),
vec4(0.0, 0.9, 1.0, 1.0)
);

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

uniform float time;
uniform float yaw;
uniform float pitch;
uniform float externalScale;
uniform float opacity;
uniform float armorMode;

// ★ 星空"风格开关"：
//   0 = 默认（暗金底 + 金色渐变星，黑金剑/鎏金锭/盔甲用）
//   1 = 终极宇宙版（纯黑底 + 图集元素彩色渐变星，终极宇宙鎏金升级模板专用）
uniform float starMode;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 fPos;

out vec4 fragColor;

mat4 rotationMatrix(vec3 axis, float angle)
{
axis = normalize(axis);
float s = sin(angle);
float c = cos(angle);
float oc = 1.0 - c;

return mat4(oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,  0.0,
oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,  0.0,
oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c,           0.0,
0.0,                                0.0,                                0.0,                                1.0);
}

vec3 rainbowColor(float h)
{
vec3 p = abs(fract(vec3(h) + vec3(1.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0);
return clamp(p - 1.0, 0.0, 1.0);
}

void main (void)
{

vec4 mask = texture(Sampler0, texCoord0.xy);

float oneOverExternalScale = 1.0 / max(externalScale, 0.001);

int uvtiles = 16;

// 背景色：默认暗金；starMode = 1 时纯黑（"里面是背景黑"）
vec4 col = (starMode > 0.5) ? vec4(0.0, 0.0, 0.0, 1.0) : vec4(0.10, 0.07, 0.02, 1.0);

float pulse = mod(time, 400.0) / 400.0;
col.g *= 1.0 + sin(pulse * M_PI * 2.0) * 0.06;
col.b *= 1.0 + cos(pulse * M_PI * 2.0) * 0.08;

vec4 dir;
if (armorMode > 0.5) {
dir = normalize(vec4(normalize(fPos), 0.0));
} else {
const float SKY_FOV = 0.62;
vec2 win = vec2(texCoord0.x - 0.5, 0.5 - texCoord0.y) * SKY_FOV;
vec3 fwd   = vec3(-sin(yaw) * cos(pitch), -sin(pitch), cos(yaw) * cos(pitch));
vec3 right = vec3(-cos(yaw), 0.0, -sin(yaw));
vec3 up    = cross(right, fwd);
dir = normalize(vec4(fwd + right * win.x + up * win.y, 0.0));
}

vec4 ray;

for (int i = 0; i < 8; i++) {
int mult = 16 - i * 2;

int j = i + 7;
float rand1 = (j * j * 4321 + j * 8) * 2.0F;
int k = j + 1;
float rand2 = (k * k * k * 239 + k * 37) * 3.6F;
float rand3 = rand1 * 347.4 + rand2 * 63.4;

vec3 axis = normalize(vec3(sin(rand1), sin(rand2), cos(rand3)));
ray = dir * rotationMatrix(axis, mod(rand3, 2.0 * M_PI));

float rawu = 0.5 + (atan(ray.z, ray.x) / (2.0 * M_PI));
float rawv = 0.5 + (asin(ray.y) / M_PI);

float scale = mult * 0.5 + 2.75;
float u = rawu * scale * externalScale;
float v = (rawv + time * 0.0002 * oneOverExternalScale) * scale * 0.6 * externalScale;

int tu = int(mod(floor(u * uvtiles), uvtiles));
int tv = int(mod(floor(v * uvtiles), uvtiles));

int position = ((171 * tu) + (489 * tv) + (303 * (i + 31)) + 17209) ^ 10;
float twinkle = 0.55 + 0.45 * sin(time * 2.2 + float(mod(position, 997)) * 0.0063);
int symbol = int(mod(position, cosmicoutof));
int rotation = int(mod(pow(tu, float(tv)) + tu + 3 + tv * i, 8));
bool flip = false;
if (rotation >= 4) {
rotation -= 4;
flip = true;
}

if (symbol >= 0 && symbol < cosmiccount) {
vec2 cosmictex = vec2(1.0, 1.0);
vec4 tcol = vec4(1.0, 0.0, 0.0, 1.0);

float ru = clamp(mod(u, 1.0) * uvtiles - tu, 0.0, 1.0);
float rv = clamp(mod(v, 1.0) * uvtiles - tv, 0.0, 1.0);

if (flip) ru = 1.0 - ru;

float oru = ru;
float orv = rv;

if (rotation == 1) {
oru = 1.0 - rv; orv = ru;
} else if (rotation == 2) {
oru = 1.0 - ru; orv = 1.0 - rv;
} else if (rotation == 3) {
oru = rv; orv = 1.0 - ru;
}

float umin = cosmicuvs[symbol].x;
float vmin = cosmicuvs[symbol].y;
float umax = cosmicuvs[symbol].z;
float vmax = cosmicuvs[symbol].w;

cosmictex.x = umin * (1.0 - oru) + umax * oru;
cosmictex.y = vmin * (1.0 - orv) + vmax * orv;

tcol = texture(Sampler1, cosmictex);

float brightness = max(tcol.r, max(tcol.g, tcol.b));
float gradient = 0.5 + 0.5 * sin(time * 1.6 + float(mod(position, 101)) * 0.06);
vec3 starTint;
if (starMode > 0.5) {
    // 彩虹渐变：色相跟着【物品表面位置】走 —— 斜着扫过整张精灵图正好一整圈彩虹，
    // 所以能一眼看出红→橙→黄→绿→青→蓝→紫的过渡，而不是"所有星星同一个颜色"。
    // 再叠一点点每个图集元素自身的红通道（让它更碎更活），以及时间让彩虹缓慢流动。
    float hue = fract(texCoord0.x * 0.65 + texCoord0.y * 0.35 + time * 0.06 + tcol.r * 0.18);
    starTint = clamp(rainbowColor(hue) * 1.35, 0.0, 1.0);   // 纯黑底上提饱和/亮度
} else {
    starTint = mix(vec3(1.0, 0.94, 0.72), vec3(1.0, 0.80, 0.05), gradient);
}
tcol = vec4(starTint * brightness, tcol.a);
float a = brightness * (0.5 + (1.0 / mult)) * (1.0 - smoothstep(0.15, 0.48, abs(rawv - 0.5))) * twinkle;

float r = (mod(rand1, 29.0) / 29.0) * 0.3 + 0.4;
float g = (mod(rand2, 35.0) / 35.0) * 0.4 + 0.6;
float b = (mod(rand1, 17.0) / 17.0) * 0.3 + 0.7;

col = col + tcol * a;
}
}

float molten = 1.0 + 0.25 * sin(texCoord0.y * 4.5 - time * 0.35
                                + sin(texCoord0.x * 2.5 + time * 0.17) * 0.9);
col.rgb *= molten;

vec3 shade = vertexColor.rgb * (lightmix * 0.25) + vec3(1.0 - lightmix * 0.25);
col.rgb *= shade;

col.a *= smoothstep(0.3, 0.7, mask.r) * opacity;
col = clamp(col, 0.0, 1.0);
fragColor = linear_fog(col * ColorModulator, vertexDistance,
        max(FogStart, 1.0e30), max(FogEnd, 1.0e31), FogColor);
}