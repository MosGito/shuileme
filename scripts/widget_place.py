"""Pixel Launcher 组件放置脚本（adb uiautomator 驱动，稳健版）。
流程: 长按桌面 → Widgets → 循环滚动找到 SleepShift → 点开预览 → 轮播切尺寸 → 拖拽/Add。
用法: python scripts/widget_place.py <small|medium> [--drag]
"""
import re
import subprocess
import sys
import time

ADB = "adb -s emulator-5554"


def sh(cmd):
    return subprocess.run(cmd, capture_output=True, shell=True, text=True, encoding="utf-8").stdout


def tap(x, y):
    sh(f"{ADB} shell input tap {x} {y}")
    time.sleep(2)


def swipe(x1, y1, x2, y2, ms=500):
    sh(f"{ADB} shell input swipe {x1} {y1} {x2} {y2} {ms}")
    time.sleep(2)


def dump():
    sh(f"{ADB} shell uiautomator dump /sdcard/ui.xml")
    sh(f"{ADB} pull /sdcard/ui.xml /tmp/ui.xml")
    return open("/tmp/ui.xml", encoding="utf-8").read()


def nodes(xml, attr="text"):
    out = []
    for m in re.finditer(rf'{attr}="([^"]{{1,60}})"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        t, x1, y1, x2, y2 = m.group(1), *map(int, m.group(2, 3, 4, 5))
        if t.strip():
            out.append((t, (x1 + x2) // 2, (y1 + y2) // 2))
    return out


def find(xml, needle, attr="text"):
    for t, cx, cy in nodes(xml, attr):
        if needle in t:
            return cx, cy
    return None


def open_widget_sheet():
    swipe(800, 1300, 800, 1300, 900)
    for _ in range(3):
        xml = dump()
        p = find(xml, "Widgets", "text")
        if p:
            tap(*p)
            return True
        time.sleep(1)
    return False


target = sys.argv[1]  # small | medium
use_drag = "--drag" in sys.argv

if not open_widget_sheet():
    print("FAIL: cannot open widget sheet"); sys.exit(1)

# 滚动直到找到 SleepShift
for _ in range(8):
    xml = dump()
    p = find(xml, "SleepShift", "text")
    if p:
        tap(*p)
        break
    swipe(540, 1600, 540, 500, 600)
else:
    print("FAIL: SleepShift not found"); sys.exit(1)

# 预览轮播 → 切到目标尺寸
time.sleep(2)
xml = dump()
want = "wide by 2 high" if target == "medium" else "wide by 1 high"
for _ in range(3):
    p = find(xml, want, "content-desc")
    if p:
        break
    swipe(900, 1700, 200, 1700, 500)
    xml = dump()
else:
    print(f"FAIL: {target} preview not found"); sys.exit(1)

print(f"FOUND {target} preview at {p}")

if use_drag:
    # 拖拽: DOWN -> 停顿 -> 验证提起 -> MOVE -> UP
    cx, cy = p
    sh(f"{ADB} shell input motionevent DOWN {cx} {cy}")
    time.sleep(2)
    xml = dump()
    lifted = ("Drop" in xml) or ("Add to Home" in xml) or ("add to" in xml)
    print(f"LIFT check: {'yes' if lifted else 'no'}")
    for i, (x, y) in enumerate([(cx, cy - 200), (cx, cy - 450), (cx, cy - 700), (cx, 800)]):
        sh(f"{ADB} shell input motionevent MOVE {x} {y}")
        time.sleep(0.3)
    sh(f"{ADB} shell input motionevent UP 540 800")
    time.sleep(3)
    print(sh(f"{ADB} shell dumpsys appwidget | grep -c 'provider=.*sleepshift'"))
else:
    xml = dump()
    p = find(xml, "Add SleepShift widget", "content-desc")
    if not p:
        print("FAIL: no Add button"); sys.exit(1)
    tap(*p)
    time.sleep(3)
    print("OK: tapped Add")
    print(sh(f"{ADB} shell dumpsys appwidget | grep -c 'provider=.*sleepshift'"))
