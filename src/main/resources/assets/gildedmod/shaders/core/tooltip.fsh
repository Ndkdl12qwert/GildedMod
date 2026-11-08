#version 150

// 黑金「金液流动」面板
// ------------------------------------------------------------------
// 做法：FBM 噪声做两层，第一层当"扰动场"去扭曲第二层的采样坐标
// （domain warping）→ 得到带丝缕感的黏稠流动，而不是一团糊噪点。
// 再按噪声值在黑 → 暗金 → 金 → 亮金 之间做多次 smoothstep 混色。
// 时间项只往 -y 方向推 → 视觉上是"金液缓缓向下淌"。

uniform float time;
uniform vec2 panelSize;

in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);          // 平滑插值，保证连续（不会出现硬边）
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(1.7, 9.2);         // 每层加点偏移，避免格子感
        a *= 0.5;
    }
    return v;
}

void main() {
    // 以像素为单位采样：面板多长多宽，纹理就多长多宽，不会被拉伸
    vec2 uv = texCoord0 * panelSize;
    float t = time * 0.55;

    // ---- 域扭曲：先算一层缓慢流动的扰动场 ----
    vec2 q = vec2(fbm(uv * 0.035 + vec2(0.0, -t * 0.35)),
                  fbm(uv * 0.035 + vec2(4.7, -t * 0.28)));

    // ---- 主体：被扰动过的噪声 ----
    float n = fbm(uv * 0.028 + q * 2.2 + vec2(0.0, -t * 0.5));

    // ---- 黑金调色（黑 → 暗金 → 金 → 亮金，全部用 smoothstep 平滑过渡）----
    vec3 cBlack = vec3(0.030, 0.022, 0.010);
    vec3 cDark  = vec3(0.215, 0.140, 0.026);
    vec3 cGold  = vec3(0.830, 0.590, 0.130);
    vec3 cHi    = vec3(1.000, 0.870, 0.450);

    vec3 col = mix(cBlack, cDark, smoothstep(0.20, 0.58, n));
    col = mix(col, cGold, smoothstep(0.52, 0.78, n));
    col = mix(col, cHi, smoothstep(0.76, 0.96, n) * 0.80);

    // ---- 边缘：内侧一圈渐暗 + 镶边金光，让内容区更聚焦 ----
    vec2 d = min(texCoord0 * panelSize, (1.0 - texCoord0) * panelSize);   // 到四边的像素距离
    float edge = smoothstep(0.0, 12.0, min(d.x, d.y));
    col *= mix(0.45, 1.0, edge);
    col += cGold * (1.0 - edge) * 0.55;

    // 面板整体带一点自发光感（vertexColor 由 Java 侧传白色）
    col *= mix(vec3(1.0), vertexColor.rgb, 0.9);

    fragColor = vec4(col, 0.94);
}
