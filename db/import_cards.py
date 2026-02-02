#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
카드 데이터 추출 및 번역 스크립트

사용법:
    python import_cards.py --input cards-jwasham.db --output cards_translated.csv
    python import_cards.py --input cards-jwasham.db --output cards_translated.csv --translate
    python import_cards.py --input cards-jwasham.db --output cards_translated.csv --translate --translator papago
"""

import sqlite3
import csv
import argparse
import time
import re
import uuid
import requests
from typing import Optional


class PapagoTranslator:
    """비공식 파파고 번역기"""

    def __init__(self, source='en', target='ko'):
        self.source = source
        self.target = target
        self.url = "https://papago.naver.com/apis/n2mt/translate"
        self.headers = {
            "Content-Type": "application/x-www-form-urlencoded",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "x-apigw-partnerid": "papago"
        }
        self.device_id = str(uuid.uuid4())

    def translate(self, text: str) -> str:
        if not text or not text.strip():
            return text

        data = {
            "source": self.source,
            "target": self.target,
            "text": text,
            "deviceId": self.device_id,
        }

        response = requests.post(self.url, headers=self.headers, data=data, timeout=30)
        response.raise_for_status()
        return response.json()["translatedText"]


def is_code_content(text: str) -> bool:
    """코드가 포함된 텍스트인지 확인 (더 정교한 판단)"""
    # 확실한 코드 문법 (하나만 있어도 코드)
    strong_indicators = ['{', '}', '();', '();', '->', '=>', '==', '!=', '&&', '||']

    # 약한 지표 (2개 이상 있어야 코드로 판단)
    weak_indicators = [
        'void ', 'int ', 'return ', 'if (', 'for (', 'while (',
        'def ', 'class ', 'function ', 'const ', 'let ', 'var ',
        'public ', 'private ', 'static ', 'import ', 'from ',
        '= new ', '.get(', '.set(', '.add('
    ]

    # 확실한 코드 문법이 있으면 코드
    for indicator in strong_indicators:
        if indicator in text:
            return True

    # 약한 지표가 2개 이상이면 코드
    weak_count = sum(1 for indicator in weak_indicators if indicator in text)
    return weak_count >= 2

def extract_and_translate(text: str, translator) -> str:
    """텍스트에서 코드 부분은 보존하고 나머지만 번역"""
    if not text or not translator:
        return text

    # 코드가 대부분인 경우 번역하지 않음
    if is_code_content(text) and len(text) < 500:
        # 짧은 코드 스니펫은 번역하지 않음
        lines = text.split('\n')
        code_lines = sum(1 for line in lines if is_code_content(line))
        if code_lines > len(lines) / 2:
            return text

    try:
        # 코드 블록 보존
        code_blocks = []
        def save_code(match):
            code_blocks.append(match.group(0))
            return f"__CODE_BLOCK_{len(code_blocks)-1}__"

        # 백틱 코드 블록 보존
        text_with_placeholders = re.sub(r'```[\s\S]*?```', save_code, text)
        text_with_placeholders = re.sub(r'`[^`]+`', save_code, text_with_placeholders)

        # 번역
        translated = translator.translate(text_with_placeholders)

        # 코드 블록 복원
        for i, code in enumerate(code_blocks):
            translated = translated.replace(f"__CODE_BLOCK_{i}__", code)

        return translated
    except Exception as e:
        print(f"번역 오류: {e}")
        return text

def process_cards(input_file: str, output_file: str, do_translate: bool = False,
                  category: str = "CS", limit: Optional[int] = None,
                  translator_type: str = "papago", delay: float = 0.3):
    """SQLite에서 카드를 읽어 CSV로 저장"""

    translator = None
    if do_translate:
        if translator_type == "papago":
            translator = PapagoTranslator(source='en', target='ko')
            print("파파고 번역기 초기화 완료")
        else:
            try:
                from deep_translator import GoogleTranslator
                translator = GoogleTranslator(source='en', target='ko')
                print("Google 번역기 초기화 완료")
            except ImportError:
                print("deep-translator가 설치되지 않았습니다. pip install deep-translator")
                return

    conn = sqlite3.connect(input_file)
    cursor = conn.cursor()

    query = "SELECT id, type, front, back FROM cards"
    if limit:
        query += f" LIMIT {limit}"

    cursor.execute(query)
    rows = cursor.fetchall()

    print(f"총 {len(rows)}개 카드 처리 시작...")

    with open(output_file, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['question_en', 'question_ko', 'answer_en', 'answer_ko', 'category'])

        for i, row in enumerate(rows):
            card_id, card_type, front, back = row

            question_en = front
            answer_en = back
            question_ko = None
            answer_ko = None

            if do_translate:
                try:
                    print(f"[{i+1}/{len(rows)}] 번역 중: {front[:50]}...")
                except UnicodeEncodeError:
                    print(f"[{i+1}/{len(rows)}] 번역 중...")

                # 질문 번역
                question_ko = extract_and_translate(front, translator)

                # 답변 번역 (코드가 많으면 건너뜀)
                if not is_code_content(back) or len(back) > 200:
                    answer_ko = extract_and_translate(back, translator)
                else:
                    answer_ko = back  # 코드는 그대로

                # API 제한 방지를 위한 딜레이
                time.sleep(delay)

            writer.writerow([question_en, question_ko or '', answer_en, answer_ko or '', category])

            if (i + 1) % 100 == 0:
                print(f"{i+1}개 처리 완료...")

    conn.close()
    print(f"완료! {output_file}에 저장되었습니다.")

def main():
    parser = argparse.ArgumentParser(description='Flash cards SQLite를 CSV로 변환 (번역 옵션)')
    parser.add_argument('--input', '-i', required=True, help='SQLite DB 파일 경로')
    parser.add_argument('--output', '-o', required=True, help='출력 CSV 파일 경로')
    parser.add_argument('--translate', '-t', action='store_true', help='한글 번역 수행')
    parser.add_argument('--translator', default='papago', choices=['papago', 'google'],
                        help='번역기 선택 (기본: papago)')
    parser.add_argument('--delay', '-d', type=float, default=0.3,
                        help='API 요청 간 딜레이 초 (기본: 0.3)')
    parser.add_argument('--category', '-c', default='CS', help='카테고리 (기본: CS)')
    parser.add_argument('--limit', '-l', type=int, help='처리할 카드 수 제한')

    args = parser.parse_args()

    process_cards(
        input_file=args.input,
        output_file=args.output,
        do_translate=args.translate,
        category=args.category,
        limit=args.limit,
        translator_type=args.translator,
        delay=args.delay
    )

if __name__ == '__main__':
    main()
