"""
ml/export/export_quantize_llm.py
Quantizes KrishiMiniLM to INT8 for ultra-fast on-device mobile execution (<20ms).
Benchmarks latency and memory against FP32.
Deploys assets directly to android/app/src/main/assets/.
"""

import os
import time
import shutil
import numpy as np
import onnx
import onnxruntime as ort
from onnxruntime.quantization import quantize_dynamic, QuantType

def quantize_and_benchmark(base_dir):
    out_dir = os.path.join(base_dir, "ml", "output")
    fp32_path = os.path.join(out_dir, "krishi_mini_llm.onnx")
    int8_path = os.path.join(out_dir, "krishi_mini_llm_quantized.onnx")
    vocab_path = os.path.join(out_dir, "vocab.json")
    assets_dir = os.path.join(base_dir, "android", "app", "src", "main", "assets")

    if not os.path.exists(fp32_path):
        raise FileNotFoundError(f"FP32 model not found at {fp32_path}")

    fp32_size = os.path.getsize(fp32_path) / 1024
    print(f"FP32 Model Size: {fp32_size:.2f} KB ({fp32_size/1024:.2f} MB)")

    # 1. Dynamic INT8 Quantization
    print("Applying dynamic INT8 quantization...")
    quantize_dynamic(
        model_input=fp32_path,
        model_output=int8_path,
        weight_type=QuantType.QUInt8
    )

    int8_size = os.path.getsize(int8_path) / 1024
    compression_ratio = fp32_size / int8_size
    print(f"INT8 Quantized Model Size: {int8_size:.2f} KB ({int8_size/1024:.2f} MB)")
    print(f"Compression Ratio: {compression_ratio:.2f}x (saved {100*(1 - int8_size/fp32_size):.1f}% disk footprint)")

    # 2. Benchmark on CPU
    dummy_input = np.random.randint(0, 2000, size=(1, 32), dtype=np.int64)

    # FP32 Session
    sess_fp32 = ort.InferenceSession(fp32_path, providers=['CPUExecutionProvider'])
    # Warmup
    for _ in range(5):
        sess_fp32.run(None, {"input_ids": dummy_input})
    t0 = time.perf_counter()
    for _ in range(50):
        sess_fp32.run(None, {"input_ids": dummy_input})
    fp32_lat = (time.perf_counter() - t0) * 1000 / 50.0

    # INT8 Session
    sess_int8 = ort.InferenceSession(int8_path, providers=['CPUExecutionProvider'])
    for _ in range(5):
        sess_int8.run(None, {"input_ids": dummy_input})
    t0 = time.perf_counter()
    for _ in range(50):
        sess_int8.run(None, {"input_ids": dummy_input})
    int8_lat = (time.perf_counter() - t0) * 1000 / 50.0

    print(f"FP32 Inference Latency: {fp32_lat:.2f} ms")
    print(f"INT8 Inference Latency: {int8_lat:.2f} ms")

    # 3. Deploy to Android Assets
    os.makedirs(assets_dir, exist_ok=True)
    target_onnx = os.path.join(assets_dir, "krishi_mini_llm_quantized.onnx")
    target_vocab = os.path.join(assets_dir, "vocab.json")

    shutil.copyfile(int8_path, target_onnx)
    if os.path.exists(vocab_path):
        shutil.copyfile(vocab_path, target_vocab)

    print(f"Deployed quantized model to Android assets: {target_onnx}")
    print(f"Deployed vocabulary to Android assets: {target_vocab}")

    # Generate benchmark summary
    report = f"""# KrishiMiniLM Benchmark Report

## Quantitative Benchmarks

| Metric | FP32 Model | INT8 Quantized Model | Delta |
| :--- | :--- | :--- | :--- |
| **Model Size** | {fp32_size:.2f} KB ({fp32_size/1024:.2f} MB) | **{int8_size:.2f} KB ({int8_size/1024:.2f} MB)** | **-{100*(1 - int8_size/fp32_size):.1f}%** |
| **CPU Latency (batch=1, seq=32)** | {fp32_lat:.2f} ms | **{int8_lat:.2f} ms** | **{fp32_lat/int8_lat:.2f}x faster** |
| **Parameters** | 1,133,568 (1.13M) | 1,133,568 (1.13M) | INT8 weight quantization |
| **Vocab Size** | 2,500 bilingual tokens | 2,500 bilingual tokens | Identical |
| **Runtime Target** | Android ONNX Runtime Mobile | Android ONNX Runtime Mobile | Zero cloud dependencies |
"""
    with open(os.path.join(base_dir, "ml", "output", "BENCHMARK_LLM.md"), "w", encoding="utf-8") as f:
        f.write(report)
    print("Generated BENCHMARK_LLM.md")

if __name__ == "__main__":
    base = "c:/Users/vibho/OneDrive/Desktop/Farmer Android App/Krishimitra-android-app"
    quantize_and_benchmark(base)
