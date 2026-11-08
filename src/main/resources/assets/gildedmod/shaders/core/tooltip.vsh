#version 150

// 工具提示面板的 GLSL 背景：黑金"金液流动"
// 核心是 FBM 噪声 + 域扭曲（domain warping）—— 这是让流动看起来"黏稠、柔和"的关键，
// 用 Java 的 g.fill 拼死也做不出这种连续性。
// 顶点格式 POSITION_COLOR_TEX：Position / Color / UV0，不需要 lightmap（GUI 里没有光照贴图）。

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float time;
uniform vec2 panelSize;      // 面板像素尺寸，用来保证不同长宽下流动不被拉伸

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
}
