#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
번역 후처리 스크립트 - 잘못 번역된 기술 용어 복원 + 코드 영역 보호

주요 개선:
1) 치환 순서 안정화: 긴 키부터 치환 (짧은 키가 긴 키를 깨뜨리는 문제 방지)
2) 코드 보호:
   - ``` code block ``` 보호
   - `inline code` 보호
   - 코드로 보이는 라인 보호 (def/for/if/{}/; 등)
3) retranslate_missing() 내 time.sleep 사용 버그 수정
"""

import csv
import re
from typing import List, Tuple

# 잘못된 번역 -> 원래 용어 매핑
WRONG_TO_CORRECT = {
    # 데이터 타입 (단독 또는 문맥에서)
    "챠 ": "char ",
    "챠의": "char의",
    "챠가": "char가",
    "불 크기": "bool 크기",
    "불의": "bool의",
    "짧은 크기": "short 크기",
    "짧은의": "short의",
    "긴 크기": "long 크기",
    "긴의": "long의",
    "긴 길이": "long long",

    # 소프트웨어/서비스명
    "동물원 사육사": "ZooKeeper",
    "동물원사육사": "ZooKeeper",
    "주키퍼": "ZooKeeper",
    "오징어": "Squid",
    "스퀴드": "Squid",
    "래빗MQ": "RabbitMQ",
    "래빗 MQ": "RabbitMQ",
    "토끼MQ": "RabbitMQ",
    "카산드라": "Cassandra",
    "카싼드라": "Cassandra",
    "몽고DB": "MongoDB",
    "몽고 DB": "MongoDB",
    "레디스": "Redis",
    "카프카": "Kafka",
    "도커": "Docker",
    "쿠버네티스": "Kubernetes",
    "엘라스틱서치": "Elasticsearch",
    "일래스틱서치": "Elasticsearch",
    "하둡": "Hadoop",
    "스파크": "Spark",
    "메모리캐시드": "Memcached",
    "멤캐시드": "Memcached",
    "엔진엑스": "nginx",
    "엔진X": "nginx",
    "아파치": "Apache",
    "제플린": "Zeppelin",
    "카우치DB": "CouchDB",
    "카우치 DB": "CouchDB",
    "H베이스": "HBase",
    "에이치베이스": "HBase",
    "빅테이블": "BigTable",
    "빅 테이블": "BigTable",
    "다이나모DB": "DynamoDB",
    "다이나모 DB": "DynamoDB",
    "아마존 다이나모": "Amazon Dynamo",
    "마리아DB": "MariaDB",
    "마리아 DB": "MariaDB",
    "포스트그레SQL": "PostgreSQL",
    "포스트그레스": "PostgreSQL",
    "마이SQL": "MySQL",
    "마이 SQL": "MySQL",

    # 알고리즘/자료구조
    "해시맵": "HashMap",
    "해시 맵": "HashMap",
    "해시셋": "HashSet",
    "해시 셋": "HashSet",
    "링크드리스트": "LinkedList",
    "링크드 리스트": "LinkedList",
    "연결 리스트": "Linked List",
    "이진 검색 트리": "Binary Search Tree",
    "레드블랙 트리": "Red-Black Tree",
    "레드-블랙 트리": "Red-Black Tree",
    "AVL 트리": "AVL Tree",
    "에이브이엘 트리": "AVL Tree",
    "B 트리": "B-Tree",
    "비 트리": "B-Tree",
    "힙 정렬": "Heap Sort",
    "퀵 정렬": "Quick Sort",
    "퀵정렬": "QuickSort",
    "병합 정렬": "Merge Sort",
    "병합정렬": "MergeSort",
    "버블 정렬": "Bubble Sort",
    "삽입 정렬": "Insertion Sort",
    "선택 정렬": "Selection Sort",

    # 네트워크/프로토콜
    "에이치에이프록시": "HAProxy",
    "바니시": "Varnish",

    # 메시지 브로커/작업 큐
    "셀러리란": "Celery란",
    "셀러리는": "Celery는",
    "셀러리": "Celery",
    "기어맨": "Gearman",
    "호넷큐": "HornetQ",
    "조람": "JORAM",
    "콩나무": "BeanstalkD",
    "액티브MQ": "ActiveMQ",

    # 데이터베이스/캐시
    "레디스": "Redis",
    "레디는": "Redis는",
    "레디": "Redis",
    "볼드모트": "Voldemort",
    "리아크": "Riak",
    "하이퍼테이블": "Hypertable",
    "인Memcache": "in-memory cache",
    "인메모리": "in-memory",
    "구글 BigTable": "Google BigTable",
    "아마존 다이너모DB": "Amazon DynamoDB",

    # 회사/브랜드명
    "빨간 모자": "Red Hat",
    "레드햇": "Red Hat",

    # 기타 IT 용어
    "빅 오": "Big O",
    "빅오": "Big-O",

    # 복잡도/CS
    "빅 오 표기법": "Big-O notation",
    "시간 복잡도": "time complexity",
    "공간 복잡도": "space complexity",
    "점근적": "asymptotic",

    # 시스템/네트워크
    "소켓": "socket",
    "포트 번호": "port number",
    "교착 상태": "deadlock",

    # 분산/DB
    "최종 일관성": "eventual consistency",
    "정족수": "quorum",
    "리더 선출": "leader election",
    "샤딩": "sharding",
    "파티션": "partition",
    "스냅샷": "snapshot",

    # 자주 틀리는 약어/표기
    "에이피아이": "API",
    "레스트": "REST",
    "유알엘": "URL",
    "제이슨": "JSON",
    "제이더블유티": "JWT",

    # 프로그래밍 키워드 (번역하면 안 됨)
    "널 값": "null 값",
    "널값": "null값",
    "널을": "null을",
    "널이": "null이",
    "널로": "null로",
    "널과": "null과",

    # true/false
    "트루": "true",
    "폴스": "false",
    "참을 반환": "true를 반환",
    "거짓을 반환": "false를 반환",
    "참 또는 거짓": "true 또는 false",
    "참/거짓": "true/false",

    # undefined
    "정의되지 않은 값": "undefined 값",
    "정의되지 않음": "undefined",

    # 자주 어색해지는 표현(가벼운 보정)
    "만족스러운": "만족하는",
    "꼭짓점": "정점",

    # 그래프 용어 (안전한 패턴만)
    "그래프의 변": "그래프의 간선",
    "의 변과": "의 간선과",
    "변의 수": "간선의 수",
    "변을 ": "간선을 ",
    "변이 ": "간선이 ",
}

# 보존해야 할 기술 용어 (번역되면 안 되는 것들) - 정규식 패턴용 (추후 확장용)
TECH_TERMS_PATTERN = [
    r'\bchar\b', r'\bbool\b', r'\bshort\b', r'\bint\b', r'\blong\b',
    r'\bfloat\b', r'\bdouble\b', r'\bwchar_t\b', r'\bvoid\b',
    r'\bsize_t\b', r'\bNULL\b', r'\bnullptr\b',
]


# -----------------------------
# Code protection helpers
# -----------------------------
CODE_FENCE_RE = re.compile(r"```.*?```", re.DOTALL)
INLINE_CODE_RE = re.compile(r"`[^`\n]+`")  # single-line inline code


def looks_like_code(text: str) -> bool:
    """텍스트 전체가 '코드/의사코드'로 보이는지 대략 판별"""
    if not text:
        return False

    # 코드펜스가 있으면 거의 확정
    if "```" in text:
        return True

    # 줄 단위로 코드스코어 계산
    lines = text.splitlines()
    code_like_lines = 0
    total_lines = 0

    keyword_or_symbols = re.compile(
        r"\b(def|class|for|while|if|else|elif|return|try|except|switch|case|break|continue|public|private|protected|static|void|int|long|double|float|new)\b"
        r"|[{}();=\[\]]"
        r"|==|!=|<=|>=|->|=>"
    )

    for ln in lines:
        s = ln.strip()
        if not s:
            continue
        total_lines += 1

        # 들여쓰기가 있고 기호가 많으면 코드일 확률↑
        if (ln.startswith("    ") or ln.startswith("\t")) and keyword_or_symbols.search(ln):
            code_like_lines += 1
            continue

        if keyword_or_symbols.search(ln):
            code_like_lines += 1

    # 줄이 적어도 코드성 라인이 많으면 코드로 간주
    if total_lines >= 3 and (code_like_lines / max(total_lines, 1)) >= 0.5:
        return True

    # 단일 라인이라도 세미콜론/중괄호/할당이 있으면 코드일 가능성↑
    if total_lines == 1 and keyword_or_symbols.search(text):
        return True

    return False


def _extract_protected_spans(text: str) -> Tuple[str, List[str]]:
    """
    ```code block``` 과 `inline code` 를 보호 토큰으로 치환해 둔 뒤,
    최종에 다시 복원할 수 있게 리스트로 보관.
    """
    protected: List[str] = []

    def _replace(match: re.Match) -> str:
        protected.append(match.group(0))
        return f"__PROTECTED_{len(protected)-1}__"

    # 먼저 큰 덩어리(펜스)부터 보호
    text = CODE_FENCE_RE.sub(_replace, text)
    # 그 다음 인라인 코드
    text = INLINE_CODE_RE.sub(_replace, text)

    return text, protected


def _restore_protected_spans(text: str, protected: List[str]) -> str:
    for i, chunk in enumerate(protected):
        text = text.replace(f"__PROTECTED_{i}__", chunk)
    return text


# -----------------------------
# 번역된 코드 감지 및 롤백
# -----------------------------
# 코드가 한글로 번역된 경우 감지하는 패턴
TRANSLATED_CODE_KEYWORDS = [
    "만약 ", "만약(", "그렇지 않으면", "반환 ", "반환(",
    "범위(", "범위 (", "의 범위", "행 =", "열 =",
    "참:", "거짓:", "없음:", "없음)", "없음,",
    "동안 ", "동안(", "위해 ", "클래스 ", "함수 ",
    "가져오기 ", "에서 ", "시도:", "예외:",
    "길이(", "인쇄(", "입력(",
]

def is_translated_code(text: str) -> bool:
    """코드가 한글로 번역되었는지 감지"""
    if not text:
        return False

    # 코드 문법 기호가 있어야 함
    has_code_syntax = any(c in text for c in ['()', '{}', '[]', '= ', ';', '->'])
    if not has_code_syntax:
        return False

    # 번역된 코드 키워드가 있는지 확인
    for keyword in TRANSLATED_CODE_KEYWORDS:
        if keyword in text:
            return True

    return False


# -----------------------------
# Core translation fix
# -----------------------------
def fix_translation(text: str) -> str:
    """잘못된 번역을 수정 + 코드/마크다운 코드영역 보호"""
    if not text:
        return text

    # 코드 전체 같으면 손대지 않음 (코드가 번역돼 깨진 경우, 여기서 '치환'만이라도 막음)
    # ※ 이미 번역돼버린 코드(만약/범위 등)는 '재번역' 단계에서 원문코드 보존이 필요함
    if looks_like_code(text):
        return text

    # 부분 코드 보호(인라인/펜스)
    working, protected = _extract_protected_spans(text)

    # 긴 키부터 치환(충돌 방지)
    for wrong, correct in sorted(WRONG_TO_CORRECT.items(), key=lambda x: -len(x[0])):
        working = working.replace(wrong, correct)

    # 보호 영역 복원
    working = _restore_protected_spans(working, protected)
    return working


def process_csv(input_file: str, output_file: str):
    """CSV 파일 후처리"""

    rows = []
    rollback_count = 0

    with open(input_file, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        if not fieldnames:
            raise ValueError("CSV 헤더를 찾을 수 없습니다.")

        # 필드 존재 확인(없으면 KeyError 방지)
        required = ["question_ko", "answer_ko"]
        for col in required:
            if col not in fieldnames:
                raise ValueError(f"CSV에 '{col}' 컬럼이 없습니다. 현재 컬럼: {fieldnames}")

        for row in reader:
            # 코드가 번역된 경우 원본(영어)으로 롤백
            answer_ko = row.get("answer_ko", "")
            answer_en = row.get("answer_en", "")

            if is_translated_code(answer_ko) and answer_en:
                row["answer_ko"] = answer_en
                rollback_count += 1
            else:
                row["answer_ko"] = fix_translation(answer_ko)

            row["question_ko"] = fix_translation(row.get("question_ko", ""))
            rows.append(row)

    with open(output_file, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"완료! {len(rows)}개 행 처리됨 (코드 롤백: {rollback_count}개)")
    print(f"저장: {output_file}")


def show_diff(input_file: str, limit: int = 20):
    """수정 전후 비교 (미리보기)"""

    count = 0
    with open(input_file, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)

        for row in reader:
            q0 = row.get("question_ko", "")
            a0 = row.get("answer_ko", "")
            q_fixed = fix_translation(q0)
            a_fixed = fix_translation(a0)

            if q_fixed != q0 or a_fixed != a0:
                count += 1
                if count <= limit:
                    print(f"\n=== 변경 {count} ===")
                    if q_fixed != q0:
                        print(f"Q 전: {q0[:120]}")
                        print(f"Q 후: {q_fixed[:120]}")
                    if a_fixed != a0:
                        print(f"A 전: {a0[:120]}")
                        print(f"A 후: {a_fixed[:120]}")

    print(f"\n총 {count}개 행이 수정됩니다.")


def is_english_heavy(text: str) -> bool:
    """영어가 대부분인 텍스트인지 확인"""
    if not text or len(text) < 10:
        return False
    english = len(re.findall(r"[a-zA-Z]", text))
    total = len(text.replace(" ", ""))
    if total == 0:
        return False
    ratio = english / total
    return ratio > 0.7 and len(text) > 20


def retranslate_missing(input_file: str, output_file: str, delay: float = 0.3):
    """
    번역 누락된 항목만 재번역

    전제:
    - import_cards.py에 PapagoTranslator, extract_and_translate, is_code_content가 있어야 함
    - 코드 내용(answer_en)이 코드면 재번역하지 않음
    """
    import time  # time.sleep 사용을 위해 함수 내부에서 import
    from import_cards import PapagoTranslator, extract_and_translate, is_code_content

    translator = PapagoTranslator(source="en", target="ko")
    print("파파고 번역기 초기화 완료")

    rows = []
    retranslated_count = 0

    with open(input_file, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        if not fieldnames:
            raise ValueError("CSV 헤더를 찾을 수 없습니다.")
        all_rows = list(reader)

    total = len(all_rows)
    for i, row in enumerate(all_rows):
        q_en = row.get("question_en", "")
        q_ko = row.get("question_ko", "")
        a_en = row.get("answer_en", "")
        a_ko = row.get("answer_ko", "")

        need_retranslate = False

        # question_ko가 영어 그대로인 경우
        if q_en and is_english_heavy(q_ko) and q_ko == q_en:
            try:
                print(f"[{i+1}/{total}] Q 재번역: {q_en[:60].encode('ascii', 'ignore').decode()}...")
            except Exception:
                print(f"[{i+1}/{total}] Q 재번역...")
            try:
                row["question_ko"] = extract_and_translate(q_en, translator)
                need_retranslate = True
                time.sleep(delay)
            except Exception as e:
                print(f"  오류(Q): {e}")

        # answer_ko가 영어 그대로인 경우 (코드 제외)
        if a_en and is_english_heavy(a_ko) and a_ko == a_en and not is_code_content(a_en):
            try:
                print(f"[{i+1}/{total}] A 재번역: {a_en[:60].encode('ascii', 'ignore').decode()}...")
            except Exception:
                print(f"[{i+1}/{total}] A 재번역...")
            try:
                row["answer_ko"] = extract_and_translate(a_en, translator)
                need_retranslate = True
                time.sleep(delay)
            except Exception as e:
                print(f"  오류(A): {e}")

        if need_retranslate:
            retranslated_count += 1

        # 용어 후처리 적용(코드 보호 포함)
        row["question_ko"] = fix_translation(row.get("question_ko", ""))
        row["answer_ko"] = fix_translation(row.get("answer_ko", ""))

        rows.append(row)

    with open(output_file, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"\n완료! {retranslated_count}개 항목 재번역됨")
    print(f"저장: {output_file}")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="번역 후처리")
    parser.add_argument("--input", "-i", required=True, help="입력 CSV")
    parser.add_argument("--output", "-o", help="출력 CSV (미지정시 덮어쓰기)")
    parser.add_argument("--preview", "-p", action="store_true", help="미리보기만")
    parser.add_argument("--limit", "-l", type=int, default=20, help="미리보기 개수")
    parser.add_argument("--retranslate", "-r", action="store_true", help="누락된 번역 재시도")
    parser.add_argument("--delay", "-d", type=float, default=0.3, help="API 딜레이 (기본: 0.3)")

    args = parser.parse_args()

    if args.preview:
        show_diff(args.input, args.limit)
    elif args.retranslate:
        output = args.output or args.input
        retranslate_missing(args.input, output, args.delay)
    else:
        output = args.output or args.input
        process_csv(args.input, output)
