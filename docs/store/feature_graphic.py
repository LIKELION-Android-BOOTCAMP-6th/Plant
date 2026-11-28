# -*- coding: utf-8 -*-
# Play 스토어 그래픽 이미지(1024x500) 생성.
# 사용: python -I feature_graphic.py <repo> <out.png> <tagline_json>
# 화분 그림은 앱의 성장 단계 이미지(ic_pot_growth_lv*.png)를 그대로 씀
import json, sys
from PIL import Image, ImageDraw, ImageFont

repo, out, tag_path = sys.argv[1], sys.argv[2], sys.argv[3]
with open(tag_path, encoding="utf-8-sig") as f:
    tagline = json.load(f)["tagline"]

W, H = 1024, 500
BG = (0xEE, 0xF5, 0xE4)      # splash_background
TRACK = (0xD9, 0xE8, 0xC6)   # splash_logo_track
ACCENT = (0x3F, 0x7F, 0x55)  # splash_wordmark

img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

# 바닥 띠 (가장자리 배경 요소)
GROUND = 418
d.rectangle([0, GROUND, W, H], fill=TRACK)

# 글자 (왼쪽)
font_dir = repo + r"\app\src\main\res\font"
title_font = ImageFont.truetype(font_dir + r"\bold.ttf", 112)
tag_font = ImageFont.truetype(font_dir + r"\medium.ttf", 36)
tx = 96
d.text((tx, 118), "Plant", font=title_font, fill=ACCENT)
d.text((tx + 4, 268), tagline, font=tag_font, fill=ACCENT)

# 오른쪽: 앱의 성장 단계 이미지 (같은 배율, 화분 바닥을 GROUND에 맞춤)
res = repo + r"\app\src\main\res\drawable-xhdpi\ic_pot_growth_lv%d.png"
levels = [1, 3, 6]
scale = 0.56
x = 584
for lv in levels:
    im = Image.open(res % lv).convert("RGBA")
    bbox = im.getbbox()  # 투명 여백 제거 (세로 위치는 bbox 기준으로 다시 맞춤)
    im = im.crop(bbox)
    im = im.resize((int(im.width * scale), int(im.height * scale)), Image.LANCZOS)
    img.paste(im, (x, GROUND + 8 - im.height), im)
    x += im.width + 22

img.save(out, "PNG")
print(out, img.size, img.mode, "right edge", x - 22)
