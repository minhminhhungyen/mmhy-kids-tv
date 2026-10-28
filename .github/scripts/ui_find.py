"""Tìm tâm một phần tử trong bản dump giao diện của uiautomator (dùng cho emulator-checks.sh).

Cách dùng: ui_find.py <tệp xml> <exact|prefix|contains|class>[-bottom] <giá trị> [thứ tự, mặc định 1]
In ra "x y" của phần tử thứ N khớp theo chữ hiển thị (text) hoặc mô tả (content-desc).
Đuôi "-bottom": chọn phần tử khớp nằm thấp nhất (ví dụ nhãn trên thanh tab ở cuối màn hình).
Mã thoát 1: không thấy; 2: tệp dump hỏng.
"""
import re
import sys
import xml.etree.ElementTree as ET


def main() -> int:
    path, mode, value = sys.argv[1], sys.argv[2], sys.argv[3]
    nth = int(sys.argv[4]) if len(sys.argv) > 4 else 1
    bottom = mode.endswith("-bottom")
    mode = mode.removesuffix("-bottom")
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError):
        return 2

    def matches(text: str) -> bool:
        if mode == "exact":
            return text == value
        if mode == "prefix":
            return text.startswith(value)
        if mode == "contains":
            return value in text
        return False

    hits = []
    for node in root.iter("node"):
        if mode == "class":
            ok = node.get("class", "") == value
        else:
            ok = matches(node.get("text", "")) or matches(node.get("content-desc", ""))
        if not ok:
            continue
        numbers = [int(n) for n in re.findall(r"-?\d+", node.get("bounds", ""))]
        if len(numbers) == 4 and numbers[2] > numbers[0] and numbers[3] > numbers[1]:
            hits.append(((numbers[0] + numbers[2]) // 2, (numbers[1] + numbers[3]) // 2))
    if bottom:
        hits.sort(key=lambda xy: -xy[1])
    if len(hits) < nth:
        return 1
    x, y = hits[nth - 1]
    print(x, y)
    return 0


if __name__ == "__main__":
    sys.exit(main())
