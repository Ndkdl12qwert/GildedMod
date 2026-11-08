# -*- coding: utf-8 -*-
"""
GildedMod 发布图（简化版）
  鎏金/熔金背景 + 只写 GildedMod，不要多余技术文案。

  输出：
    art/GildedMod-banner.png   1600x600
    art/GildedMod-icon.png     1024x1024
"""
import math
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

OUT = r"D:\IDEA\MyMode\forge-1.20.1-47.4.13-mdk\art"
os.makedirs(OUT, exist_ok=True)

# 鎏金色阶
DEEP = (74, 44, 8)       # 暗金（边缘）
MID = (168, 116, 26)     # 中金
HOT = (255, 208, 96)     # 亮金
WHITE_HOT = (255, 240, 200)
INK = (36, 22, 4)        # 字色（深褐，压在金上）


def F(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()


F_TITLE = r"C:\Windows\Fonts\bahnschrift.ttf"
F_CN = r"C:\Windows\Fonts\msyh.ttc"


def make_gold_bg(size, bright=1.0):
    """熔金背景：径向渐变 + 流动感 + 细密颗粒"""
    w, h = size
    # 1) 径向渐变（中心亮 → 边缘暗）
    small = Image.new("RGB", (w // 4, h // 4))
    d = ImageDraw.Draw(small)
    sw, sh = small.size
    cx, cy = sw * 0.5, sh * 0.42
    maxr = math.hypot(sw, sh) * 0.62
    for y in range(sh):
        for x in range(sw):
            t = min(1.0, math.hypot(x - cx, y - cy) / maxr)
            t = t ** 0.85
            col = tuple(
                int((HOT[i] * (1 - t) + DEEP[i] * t) * bright) for i in range(3)
            )
            d.point((x, y), tuple(min(255, c) for c in col))
    bg = small.resize((w, h), Image.BICUBIC)

    # 2) 熔流纹理：几条正弦亮带
    flow = Image.new("RGB", (w, h), (0, 0, 0))
    fd = ImageDraw.Draw(flow)
    for k in range(14):
        amp = 26 + k * 6
        phase = k * 1.7
        base = h * (0.12 + 0.055 * k)
        pts = []
        for x in range(0, w + 8, 8):
            y = base + amp * math.sin(x / (150 + k * 26) + phase)
            pts.append((x, y))
        fd.line(pts, fill=(30 + k * 4, 22 + k * 3, 4), width=2 + (k % 3))
    flow = flow.filter(ImageFilter.GaussianBlur(3))
    bg = ImageChops.add(bg, flow)

    # 3) 高光斑
    glow = Image.new("L", (w, h), 0)
    gd = ImageDraw.Draw(glow)
    for r, v in ((int(h * 0.9), 40), (int(h * 0.6), 55), (int(h * 0.32), 80)):
        gd.ellipse([w * 0.5 - r, h * 0.34 - r * 0.7, w * 0.5 + r, h * 0.34 + r * 0.7], fill=v)
    glow = glow.filter(ImageFilter.GaussianBlur(h * 0.12))
    hot = Image.new("RGB", (w, h), WHITE_HOT)
    bg = ImageChops.add(bg, Image.composite(hot, Image.new("RGB", (w, h), (0, 0, 0)), glow))

    # 4) 颗粒（鎏金不是纯色）
    random.seed(7)
    noise = Image.new("L", (w // 3, h // 3))
    nd = ImageDraw.Draw(noise)
    for _ in range((w // 3) * (h // 3) // 14):
        nd.point((random.randrange(w // 3), random.randrange(h // 3)),
                 fill=random.randrange(30, 100))
    noise = noise.resize((w, h), Image.BILINEAR).filter(ImageFilter.GaussianBlur(1))
    bg = ImageChops.add(bg, Image.composite(Image.new("RGB", (w, h), (26, 18, 4)),
                                            Image.new("RGB", (w, h), (0, 0, 0)), noise))

    # 5) 边缘压暗（暗角）
    vig = Image.new("L", (w, h), 0)
    vd = ImageDraw.Draw(vig)
    vd.ellipse([-w * 0.15, -h * 0.3, w * 1.15, h * 1.3], fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(h * 0.18))
    dark = Image.new("RGB", (w, h), (40, 24, 4))
    bg = Image.composite(bg, dark, vig)
    return bg


def stamp_title(img, text, size_px, center, ink=INK, emboss=True):
    """压印式标题：深色字 + 上方 1px 高光（像刻在金子上）"""
    w, h = img.size
    f = F(F_TITLE, size_px)
    d = ImageDraw.Draw(img)
    x, y = center

    if emboss:
        d.text((x, y - 2), text, font=f, fill=(255, 236, 180), anchor="mm")
        d.text((x, y + 2), text, font=f, fill=(52, 30, 2), anchor="mm")
    d.text((x, y), text, font=f, fill=ink, anchor="mm")
    return img


def make_banner(path, W=1600, H=600):
    img = make_gold_bg((W, H))
    stamp_title(img, "GildedMod", 168, (W * 0.5, H * 0.46))
    d = ImageDraw.Draw(img)
    d.text((W * 0.5, H * 0.745), "Forge 1.20.1", font=F(F_CN, 30),
           fill=(58, 36, 6), anchor="mm")
    # 细刻线
    d.line([(W * 0.5 - 150, H * 0.66), (W * 0.5 + 150, H * 0.66)],
           fill=(72, 46, 10), width=2)
    img.save(path, "PNG", optimize=True)
    print("  banner -> %s  %dx%d" % (path, W, H))


def make_icon(path, S=1024):
    img = make_gold_bg((S, S), bright=1.05)
    stamp_title(img, "GildedMod", 176, (S * 0.5, S * 0.48))
    d = ImageDraw.Draw(img)
    d.line([(S * 0.5 - 170, S * 0.60), (S * 0.5 + 170, S * 0.60)],
           fill=(72, 46, 10), width=3)
    d.text((S * 0.5, S * 0.655), "Forge 1.20.1", font=F(F_CN, 32),
           fill=(58, 36, 6), anchor="mm")
    img.save(path, "PNG", optimize=True)
    print("  icon   -> %s  %dx%d" % (path, S, S))


make_banner(os.path.join(OUT, "GildedMod-banner.png"))
make_icon(os.path.join(OUT, "GildedMod-icon.png"))
