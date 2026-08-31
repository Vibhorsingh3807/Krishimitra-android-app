"""
train_nlp.py
Trains an agriculture NLP intent classifier and builds a portable offline
knowledge retrieval index for low-end Android mobile deployment (<200KB).
Evaluates accuracy on a held-out test split and exports JSON weights for Kotlin and Python.
"""

import json
import os
import re
import math
from collections import defaultdict
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, accuracy_score

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(ROOT_DIR, "data")
OUTPUT_DIR = os.path.join(ROOT_DIR, "ml_pipeline", "output")

def clean_text(text):
    text = text.lower().strip()
    for ch in ['?', '!', '.', ',', ';', ':', "'", '"', '(', ')', '[', ']', '{', '}']:
        text = text.replace(ch, " ")
    tokens = [t for t in text.split() if t]
    return " ".join(tokens)

def train_and_export():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    dataset_path = os.path.join(DATA_DIR, "generated_training_questions.json")
    with open(dataset_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    print(f"Loaded {len(data)} training examples.")

    texts = [clean_text(d["question"]) for d in data]
    labels = [d["intent"] for d in data]

    # Train / Test split with stratification
    X_train, X_test, y_train, y_test, data_train, data_test = train_test_split(
        texts, labels, data, test_size=0.15, random_state=42, stratify=labels
    )

    print(f"Train size: {len(X_train)}, Test size: {len(X_test)}")

    # TF-IDF Vectorizer with unigram and bigram features
    vectorizer = TfidfVectorizer(ngram_range=(1, 2), min_df=2, max_features=3000)
    X_train_vec = vectorizer.fit_transform(X_train)
    X_test_vec = vectorizer.transform(X_test)

    # Train multiclass regularized logistic regression
    clf = LogisticRegression(C=5.0, max_iter=800, random_state=42)
    clf.fit(X_train_vec, y_train)

    # Evaluate
    y_pred = clf.predict(X_test_vec)
    acc = accuracy_score(y_test, y_pred)
    print(f"\n==========================================")
    print(f"Held-out Test Set Accuracy: {acc * 100:.2f}%")
    print(f"==========================================\n")
    print(classification_report(y_test, y_pred))

    # Export model to lightweight JSON for zero-dependency mobile inference in Kotlin
    classes = list(clf.classes_)
    vocab = {k: int(v) for k, v in vectorizer.vocabulary_.items()}
    idf = [float(x) for x in vectorizer.idf_]
    coef = [[float(val) for val in row] for row in clf.coef_]
    intercept = [float(val) for val in clf.intercept_]

    mobile_nlp_model = {
        "model_type": "tfidf_logistic_regression",
        "ngram_range": [1, 2],
        "classes": classes,
        "vocabulary": vocab,
        "idf": idf,
        "coefficients": coef,
        "intercept": intercept,
        "accuracy": round(acc, 4)
    }

    model_path = os.path.join(OUTPUT_DIR, "mobile_nlp_intent_model.json")
    with open(model_path, "w", encoding="utf-8") as f:
        json.dump(mobile_nlp_model, f, ensure_ascii=False)
    print(f"Mobile Intent Model exported to: {model_path} ({os.path.getsize(model_path) / 1024:.1f} KB)")

    # Build deduplicated knowledge retrieval database index for offline semantic matching
    knowledge_entries = []
    seen = set()
    for item in data:
        key = (item["intent"], item.get("crop_id"), item["answer_en"])
        if key in seen:
            continue
        seen.add(key)
        
        keywords = []
        if item.get("crop_id"):
            keywords.append(item["crop_id"])
        keywords.append(item["intent"])

        knowledge_entries.append({
            "intent": item["intent"],
            "crop_id": item.get("crop_id"),
            "keywords": keywords,
            "sample_question": item["question"],
            "answer_en": item["answer_en"],
            "answer_hi": item["answer_hi"],
            "source": item["source"]
        })

    index_path = os.path.join(OUTPUT_DIR, "mobile_knowledge_index.json")
    with open(index_path, "w", encoding="utf-8") as f:
        json.dump(knowledge_entries, f, ensure_ascii=False, indent=2)

    print(f"Knowledge Retrieval Index exported ({len(knowledge_entries)} verified items) to: {index_path} ({os.path.getsize(index_path) / 1024:.1f} KB)")

if __name__ == "__main__":
    train_and_export()
