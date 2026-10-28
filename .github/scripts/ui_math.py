"""Đọc bài toán trên màn hình khóa từ bản dump giao diện của uiautomator (dùng cho emulator-checks.sh).

Cách dùng: ui_math.py <tệp xml>
In một dòng: "<câu hỏi>|<đáp án đúng>|<x y đáp án đúng>|<x y một đáp án sai>"
Mã thoát 1: chưa thấy bài toán và đủ 4 nút đáp án; 2: tệp dump hỏng.
"""
import re
import sys
import xml.etree.ElementTree as ET

QUESTION = re.compile(r"^(\d+) ([+−-]) (\d+) = \?$")


def center(node):
    numbers = [int(n) for n in re.findall(r"-?\d+", node.get("bounds", ""))]
    if len(numbers) == 4 and numbers[2] > numbers[0] and numbers[3] > numbers[1]:
        return (numbers[0] + numbers[2]) // 2, (numbers[1] + numbers[3]) // 2
    return None


def main() -> int:
    try:
        root = ET.parse(sys.argv[1]).getroot()
    except (ET.ParseError, OSError):
        return 2
    question = None
    answer = None
    options = []
    for node in root.iter("node"):
        text = node.get("text", "")
        m = QUESTION.match(text)
        if m:
            a, op, b = int(m.group(1)), m.group(2), int(m.group(3))
            answer = a + b if op == "+" else a - b
            question = text
        elif re.fullmatch(r"\d+", text) and node.get("clickable") == "true":
            xy = center(node)
            if xy:
                options.append((int(text), xy))
    if question is None or len(options) < 4:
        return 1
    right = [xy for value, xy in options if value == answer]
    wrong = [xy for value, xy in options if value != answer]
    if not right or not wrong:
        return 1
    print(f"{question}|{answer}|{right[0][0]} {right[0][1]}|{wrong[0][0]} {wrong[0][1]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
