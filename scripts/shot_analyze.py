"""截图分析辅助：像素校验 + 缩小预览。
用法:
  python scripts/shot_analyze.py <png> [--bars] [--preview]
  --bars    检查顶部状态栏 / 底部导航栏是否为白底
  --preview 生成 50% 宽度 JPG 供查看
"""
import sys
from collections import Counter

from PIL import Image

BAR_H = 24  # 系统栏区域高度（px, 1080 宽下 24~48 像素量级）


def dominant(img, region=None, n=5):
    if region:
        img = img.crop(region)
    img = img.convert("RGB").resize((img.width // 4, img.height // 4))
    cnt = Counter(img.getdata())
    return cnt.most_common(n)


def fmt(c, n):
    return "#%02X%02X%02X" % c[:3]


def main():
    path = sys.argv[1]
    img = Image.open(path).convert("RGB")
    print(f"file={path} size={img.width}x{img.height}")
    avg = tuple(sum(p[i] for p in img.getdata()) // (img.width * img.height) for i in range(3))
    print(f"avg={fmt(avg, 0)} (亮度 {int(sum(avg)/3)})")
    print("dominant:", ", ".join(f"{fmt(c, n)}({n})" for c, n in dominant(img)))

    if "--bars" in sys.argv:
        w, h = img.size
        top = dominant(img, (0, 0, w, BAR_H), 3)
        bot = dominant(img, (0, h - BAR_H, w, h), 3)
        for label, bars in (("TOP(status bar)", top), ("BOTTOM(nav bar)", bot)):
            mx = bars[0][0]
            lum = int(sum(mx) / 3)
            verdict = "WHITE! BAD" if lum > 200 else "dark OK"
            print(f"{label}: {', '.join(fmt(c, n) for c, n in bars)} -> {verdict}")

    if "--preview" in sys.argv:
        w, h = img.size
        out = path.rsplit(".", 1)[0] + "_prev.jpg"
        img.resize((w // 2, h // 2), Image.LANCZOS).save(out, quality=82)
        print(f"preview={out}")


if __name__ == "__main__":
    main()
