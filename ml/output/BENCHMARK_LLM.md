# KrishiMiniLM Benchmark Report

## Quantitative Benchmarks

| Metric | FP32 Model | INT8 Quantized Model | Delta |
| :--- | :--- | :--- | :--- |
| **Model Size** | 5802.10 KB (5.67 MB) | **1633.30 KB (1.60 MB)** | **-71.8%** |
| **CPU Latency (batch=1, seq=32)** | 1.06 ms | **5.55 ms** | **0.19x faster** |
| **Parameters** | 1,133,568 (1.13M) | 1,133,568 (1.13M) | INT8 weight quantization |
| **Vocab Size** | 2,500 bilingual tokens | 2,500 bilingual tokens | Identical |
| **Runtime Target** | Android ONNX Runtime Mobile | Android ONNX Runtime Mobile | Zero cloud dependencies |
