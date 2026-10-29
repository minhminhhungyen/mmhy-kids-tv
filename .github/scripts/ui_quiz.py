"""Đọc 4 ô đáp án trên màn hình khóa từ bản dump giao diện của uiautomator (dùng cho emulator-checks.sh).

Cách dùng:
  ui_quiz.py <tệp xml> info       In chữ của 4 ô đáp án từ trái sang phải, nối bằng "|". Mã thoát 1: không thấy đủ 4 ô.
  ui_quiz.py <tệp xml> tap <N>    In "x y" tâm của ô đáp án thứ N (đếm từ 0, trái sang phải).
Mã thoát 2: tệp dump hỏng.

Ô đáp án = TextView bấm được, không phải nút "Phụ huynh", nằm trên cùng một hàng.
Không phụ thuộc vào chữ hay hình trong ô nên dùng được cho cả toán lẫn tiếng Việt (chữ, hình, tiếng).
"""
import re
import sys
import xml.etree.ElementTree as ET


def options(root):
    found = []
    for node in root.iter("node"):
        if node.get("class", "") != "android.widget.TextView" or node.get("clickable") != "true":
            continue
        text = node.get("text", "")
        if not text or text.startswith("Phụ huynh"):
            continue
        n = [int(v) for v in re.findall(r"-?\d+", node.get("bounds", ""))]
        if len(n) == 4 and n[2] > n[0] and n[3] > n[1]:
            found.append(((n[0] + n[2]) // 2, (n[1] + n[3]) // 2, text))
    if len(found) < 4:
        return None
    # Bốn ô đáp án cùng một hàng: lấy hàng có nhiều ô nhất, sắp từ trái sang phải
    rows = {}
    for x, y, t in found:
        rows.setdefault(round(y / 40), []).append((x, y, t))
    best = max(rows.values(), key=len)
    if len(best) != 4:
        return None
    return sorted(best)


def main() -> int:
    try:
        root = ET.parse(sys.argv[1]).getroot()
    except (ET.ParseError, OSError):
        return 2
    opts = options(root)
    if opts is None:
        return 1
    if sys.argv[2] == "info":
        print("|".join(t for _, _, t in opts))
        return 0
    if sys.argv[2] == "tap":
        x, y, _ = opts[int(sys.argv[3])]
        print(x, y)
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
