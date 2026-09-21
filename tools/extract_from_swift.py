#!/usr/bin/env python3
"""iOS 저장소(Swift)의 원본 데이터를 JSON 으로 옮긴다.

손으로 3,400 줄을 베끼면 반드시 어딘가 틀린다. 그리고 틀린 것이 명언 한 글자라면
아무도 눈치채지 못한 채 배포된다. 그래서 기계가 옮기고, 개수를 세어 확인한다.

    python3 tools/extract_from_swift.py <iOS 저장소 경로>

읽는 곳   <iOS>/Shared/Data/*.swift
쓰는 곳   core/src/main/resources/data/*.json
"""
from __future__ import annotations

import json
import pathlib
import re
import sys


# ---------------------------------------------------------------- Swift 파싱

def _fold_multiline(raw: str, closing_indent: str) -> str:
    """Swift 여러 줄 문자열의 알맹이를 한 줄짜리 리터럴로 만든다.

    Swift 규칙 두 가지를 따른다.
    1. 닫는 `\"\"\"` 의 들여쓰기만큼을 모든 줄에서 덜어 낸다.
    2. 줄 끝의 역슬래시는 "여기서 줄을 바꾸지 않는다"는 뜻이다.
    """
    lines = raw.split("\n")
    if lines and not lines[0].strip():
        lines = lines[1:]
    trimmed = [ln[len(closing_indent):] if ln.startswith(closing_indent) else ln.lstrip()
               for ln in lines]

    joined, buffer = [], ""
    for line in trimmed:
        if line.endswith("\\"):
            buffer += line[:-1]
        else:
            joined.append(buffer + line)
            buffer = ""
    if buffer:
        joined.append(buffer)
    # 닫는 구분자 바로 앞의 줄바꿈은 내용이 아니다. Swift 도 그것을 세지 않는다.
    while joined and not joined[-1]:
        joined.pop()
    return "\n".join(joined)


def normalize(source: str) -> str:
    """줄 주석을 없애고, 여러 줄 문자열을 한 줄 리터럴로 접는다.

    두 가지를 한 번에 하는 이유는 서로를 알아야 하기 때문이다. 문자열 안의 `//` 는
    주석이 아니고, 주석 안의 따옴표는 문자열의 시작이 아니다. 따로 돌리면 둘 다 틀린다.
    """
    out, i = [], 0
    while i < len(source):
        if source.startswith('\"\"\"', i):
            end = source.index('\"\"\"', i + 3)
            line_start = source.rfind("\n", 0, end) + 1
            indent = source[line_start:end]
            folded = _fold_multiline(source[i + 3:line_start], indent)
            escaped = (folded.replace("\\", "\\\\")
                             .replace('"', '\\"')
                             .replace("\n", "\\n"))
            out.append(f'"{escaped}"')
            i = end + 3
            continue

        ch = source[i]
        if ch == '"':                                  # 한 줄 문자열은 그대로 옮긴다
            j = i + 1
            while j < len(source):
                if source[j] == "\\":
                    j += 2
                    continue
                if source[j] == '"':
                    break
                j += 1
            out.append(source[i:j + 1])
            i = j + 1
            continue

        if source.startswith("//", i):                 # 줄 주석은 버린다
            while i < len(source) and source[i] != "\n":
                i += 1
            continue

        out.append(ch)
        i += 1
    return "".join(out)


def find_calls(source: str, name: str) -> list[str]:
    """`name(` 으로 시작하는 호출의 괄호 안쪽을 통째로 돌려준다."""
    results = []
    for match in re.finditer(rf"\b{name}\s*\(", source):
        start = match.end()
        depth = 1
        i = start
        in_string = False
        while i < len(source) and depth:
            ch = source[i]
            if in_string:
                if ch == "\\":
                    i += 2
                    continue
                if ch == '"':
                    in_string = False
            elif ch == '"':
                in_string = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        results.append(source[start:i])
    return results


def split_arguments(body: str) -> list[str]:
    """최상위 쉼표로만 자른다. 괄호·대괄호·문자열 안의 쉼표는 무시한다."""
    parts, depth, start, in_string = [], 0, 0, False
    i = 0
    while i < len(body):
        ch = body[i]
        if in_string:
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                in_string = False
        elif ch == '"':
            in_string = True
        elif ch in "([":
            depth += 1
        elif ch in ")]":
            depth -= 1
        elif ch == "," and depth == 0:
            parts.append(body[start:i])
            start = i + 1
        i += 1
    parts.append(body[start:])
    return [p.strip() for p in parts if p.strip()]


SWIFT_ESCAPES = {"n": "\n", "t": "\t", "r": "\r", "0": "\0",
                 "\\": "\\", '"': '"', "'": "'"}


def decode_swift_string(raw: str) -> str:
    """Swift 문자열 리터럴을 푼다.

    `json.loads` 를 쓸 뻔했지만 Swift 는 유니코드를 `\\u{201C}` 로 적고 JSON 은
    `\\u201C` 로 적는다. 실제로 명언 한 편이 곡선 따옴표를 그렇게 쓰고 있었다.
    """
    body, out, i = raw[1:-1], [], 0
    while i < len(body):
        ch = body[i]
        if ch != "\\":
            out.append(ch)
            i += 1
            continue
        nxt = body[i + 1]
        if nxt == "u" and body[i + 2] == "{":
            end = body.index("}", i)
            out.append(chr(int(body[i + 3:end], 16)))
            i = end + 1
            continue
        out.append(SWIFT_ESCAPES.get(nxt, nxt))
        i += 2
    return "".join(out)


def parse_value(raw: str):
    raw = raw.strip()
    if raw == "nil":
        return None
    if raw.startswith('"'):
        return decode_swift_string(raw)
    if raw.startswith("["):
        inner = raw[1:-1].strip()
        return [parse_value(v) for v in split_arguments(inner)] if inner else []
    if raw.startswith("."):
        return raw[1:]                              # .work -> "work"
    if re.fullmatch(r"-?\d+", raw):
        return int(raw)
    if raw in ("true", "false"):
        return raw == "true"
    raise ValueError(f"해석하지 못한 값: {raw!r}")


def parse_call(body: str, positional: list[str] | None = None) -> dict:
    """`label: value` 쌍을 dict 로. 레이블 없는 인자는 positional 순서로 채운다."""
    result, index = {}, 0
    for argument in split_arguments(body):
        match = re.match(r"^([A-Za-z_][A-Za-z0-9_]*)\s*:\s*(.*)$", argument, re.S)
        if match and not match.group(2).startswith(":"):
            result[match.group(1)] = parse_value(match.group(2))
        else:
            if not positional or index >= len(positional):
                raise ValueError(f"레이블 없는 인자를 둘 곳이 없다: {argument!r}")
            result[positional[index]] = parse_value(argument)
            index += 1
    return result


# ---------------------------------------------------------------- 항목별 추출

def read(ios: pathlib.Path, name: str) -> str:
    path = ios / "Shared" / "Data" / name
    if not path.exists():
        raise SystemExit(f"찾을 수 없습니다: {path}")
    return normalize(path.read_text(encoding="utf-8"))


def extract_quotes(ios: pathlib.Path) -> list[dict]:
    source = read(ios, "QuoteLibraryData.swift")
    quotes = []
    for body in find_calls(source, "Quote"):
        fields = parse_call(body)
        quotes.append({
            "slug": fields["slug"],
            "text": fields["text"],
            "originalText": fields.get("originalText"),
            "authorId": fields["authorID"],
            "category": fields["category"],
            "secondaryCategories": fields.get("secondaryCategories", []),
        })
    return quotes


def extract_authors(ios: pathlib.Path) -> list[dict]:
    source = read(ios, "AuthorLibrary.swift")
    authors = []
    for body in find_calls(source, "Author"):
        fields = parse_call(body)
        authors.append({
            "id": fields["id"],
            "name": fields["name"],
            "koreanName": fields.get("koreanName"),
            "birthYear": fields.get("birthYear"),
            "deathYear": fields.get("deathYear"),
            "occupation": fields["occupation"],
            "nationality": fields["nationality"],
            "biography": fields["biography"],
            "achievements": fields.get("achievements", []),
            "era": fields.get("era"),
            "notableWorks": fields.get("notableWorks", []),
        })
    return authors


def extract_stories(ios: pathlib.Path) -> list[dict]:
    source = read(ios, "BehindStoryLibrary.swift")
    stories = []
    for body in find_calls(source, "BehindStory"):
        fields = parse_call(body)
        stories.append({
            "quoteSlug": fields["quoteSlug"],
            "occasion": fields["occasion"],
            "context": fields["context"],
            "takeaway": fields.get("takeaway"),
            "source": fields["source"],
        })
    return stories


def extract_foreign_names(ios: pathlib.Path) -> list[dict]:
    """ForeignName("English", "한국어", "직업", "국적") — 레이블이 없다."""
    source = read(ios, "ForeignNameLibrary.swift")
    names = []
    for body in find_calls(source, "ForeignName"):
        fields = parse_call(body, positional=["english", "korean", "occupation", "nationality"])
        names.append({
            "english": fields["english"],
            "korean": fields["korean"],
            "occupation": fields.get("occupation"),
            "nationality": fields.get("nationality"),
        })
    return names


def extract_aliases(ios: pathlib.Path) -> dict[str, str]:
    """AuthorLibrary.aliases 의 "왼쪽": "오른쪽" 쌍."""
    source = read(ios, "AuthorLibrary.swift")
    match = re.search(r"aliases\s*:\s*\[String\s*:\s*String\]\s*=\s*\[(.*?)\n\s*\]", source, re.S)
    if not match:
        return {}
    pairs = {}
    for chunk in split_arguments(match.group(1)):
        key, _, value = chunk.partition(":")
        pairs[parse_value(key)] = parse_value(value)
    return pairs


def extract_disputed(ios: pathlib.Path) -> list[str]:
    source = read(ios, "DisputedAttribution.swift")
    match = re.search(r"slugs\s*:\s*Set<String>\s*=\s*\[(.*?)\n\s*\]", source, re.S)
    if not match:
        return []
    return sorted({parse_value(v) for v in split_arguments(match.group(1))})


# ---------------------------------------------------------------- 실행

def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    ios = pathlib.Path(sys.argv[1]).expanduser().resolve()
    out = pathlib.Path(__file__).resolve().parent.parent / "core/src/main/resources/data"
    out.mkdir(parents=True, exist_ok=True)

    payloads = {
        "quotes.json": extract_quotes(ios),
        "authors.json": extract_authors(ios),
        "behind_stories.json": extract_stories(ios),
        "foreign_names.json": extract_foreign_names(ios),
        "author_aliases.json": extract_aliases(ios),
        "disputed_slugs.json": extract_disputed(ios),
    }

    for name, payload in payloads.items():
        text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
        (out / name).write_text(text, encoding="utf-8")
        print(f"  {name:24} {len(payload):4}개")

    # 옮기다 흘린 것이 없는지 본다. 이 검사가 이 스크립트의 존재 이유다.
    quotes, authors = payloads["quotes.json"], payloads["authors.json"]
    known = {a["id"] for a in authors}
    slugs = [q["slug"] for q in quotes]

    problems = []
    if len(slugs) != len(set(slugs)):
        problems.append("slug 가 중복된다")
    for quote in quotes:
        if quote["authorId"] not in known:
            problems.append(f"{quote['slug']}: 없는 인물 {quote['authorId']}")
    story_slugs = {s["quoteSlug"] for s in payloads["behind_stories.json"]}
    for missing in sorted(story_slugs - set(slugs)):
        problems.append(f"비하인드 스토리가 없는 명언을 가리킨다: {missing}")
    if any(not q["text"] for q in quotes):
        problems.append("본문이 빈 명언이 있다")

    if problems:
        for problem in problems:
            print(f"  ✗ {problem}")
        raise SystemExit("추출 결과가 어긋납니다.")
    print(f"\n검증 통과 — 명언 {len(quotes)}편 / 인물 {len(authors)}명")


if __name__ == "__main__":
    main()
