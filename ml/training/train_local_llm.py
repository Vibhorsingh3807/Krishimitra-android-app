"""
ml/training/train_local_llm.py
Trains KrishiMiniLM — a compact, factual, on-device causal transformer language model
conditioned on local RAG context for concise Indian agricultural answers (Hindi & English).
Hardware acceleration: NVIDIA RTX 4060 GPU via PyTorch CUDA.
"""

import json
import os
import math
import time
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader

# Define Architecture
class KrishiMiniLM(nn.Module):
    def __init__(self, vocab_size, d_model=128, nhead=4, num_layers=4, d_ff=512, max_len=160):
        super().__init__()
        self.d_model = d_model
        self.token_embedding = nn.Embedding(vocab_size, d_model)
        self.pos_embedding = nn.Embedding(max_len, d_model)
        
        encoder_layer = nn.TransformerEncoderLayer(
            d_model=d_model,
            nhead=nhead,
            dim_feedforward=d_ff,
            dropout=0.1,
            activation='gelu',
            batch_first=True
        )
        self.transformer = nn.TransformerEncoder(encoder_layer, num_layers=num_layers)
        self.lm_head = nn.Linear(d_model, vocab_size, bias=False)
        # Weight tying
        self.lm_head.weight = self.token_embedding.weight

    def forward(self, input_ids, mask=None):
        B, S = input_ids.shape
        positions = torch.arange(S, device=input_ids.device).unsqueeze(0).expand(B, S)
        x = self.token_embedding(input_ids) * math.sqrt(self.d_model) + self.pos_embedding(positions)
        
        # Causal mask to ensure autoregressive generation
        causal_mask = nn.Transformer.generate_square_subsequent_mask(S, device=input_ids.device)
        out = self.transformer(x, mask=causal_mask, is_causal=True)
        logits = self.lm_head(out)
        return logits

class SimpleBilingualTokenizer:
    def __init__(self):
        self.special_tokens = ["<PAD>", "<UNK>", "<BOS>", "<EOS>", "<QUERY>", "<CONTEXT>", "<ANSWER>"]
        self.token2id = {t: i for i, t in enumerate(self.special_tokens)}
        self.id2token = {i: t for i, t in enumerate(self.special_tokens)}

    def build_vocab(self, texts, max_vocab=2500):
        word_freq = {}
        for text in texts:
            words = text.split()
            for w in words:
                word_freq[w] = word_freq.get(w, 0) + 1
        
        # Add most frequent words
        sorted_words = sorted(word_freq.items(), key=lambda x: x[1], reverse=True)
        for w, _ in sorted_words:
            if len(self.token2id) >= max_vocab:
                break
            if w not in self.token2id:
                idx = len(self.token2id)
                self.token2id[w] = idx
                self.id2token[idx] = w

    def encode(self, text, max_len=160):
        words = text.split()
        tokens = [self.token2id.get(w, self.token2id["<UNK>"]) for w in words]
        if len(tokens) > max_len:
            tokens = tokens[:max_len]
        return tokens

    def decode(self, token_ids):
        words = []
        for tid in token_ids:
            if tid in self.id2token:
                w = self.id2token[tid]
                if w in ["<PAD>", "<BOS>", "<EOS>"]:
                    continue
                words.append(w)
        return " ".join(words)

    def save(self, path):
        with open(path, "w", encoding="utf-8") as f:
            json.dump({"token2id": self.token2id, "id2token": {str(k): v for k, v in self.id2token.items()}}, f, ensure_ascii=False, indent=2)

class RAGDataset(Dataset):
    def __init__(self, triplets, tokenizer, max_len=160):
        self.examples = []
        for item in triplets:
            # Format: <BOS> <QUERY> query <CONTEXT> context <ANSWER> response <EOS>
            seq = f"<BOS> <QUERY> {item['query']} <CONTEXT> {item['context']} <ANSWER> {item['response']} <EOS>"
            tokens = tokenizer.encode(seq, max_len)
            self.examples.append(tokens)
        self.max_len = max_len
        self.pad_id = tokenizer.token2id["<PAD>"]

    def __len__(self):
        return len(self.examples)

    def __getitem__(self, idx):
        tokens = self.examples[idx]
        pad_len = self.max_len - len(tokens)
        input_ids = tokens + [self.pad_id] * pad_len
        labels = tokens + [-100] * pad_len
        return torch.tensor(input_ids, dtype=torch.long), torch.tensor(labels, dtype=torch.long)

def train():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Using compute device: {device} ({torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'CPU'})")

    base_dir = "c:/Users/vibho/OneDrive/Desktop/Farmer Android App/Krishimitra-android-app"
    triplets_path = os.path.join(base_dir, "ml", "data", "rag_llm_training_triplets.json")
    
    with open(triplets_path, "r", encoding="utf-8") as f:
        triplets = json.load(f)

    # Augment data with question variations
    all_texts = []
    for item in triplets:
        all_texts.append(f"<QUERY> {item['query']} <CONTEXT> {item['context']} <ANSWER> {item['response']}")

    tokenizer = SimpleBilingualTokenizer()
    tokenizer.build_vocab(all_texts, max_vocab=2500)
    vocab_size = len(tokenizer.token2id)
    print(f"Built bilingual vocabulary: {vocab_size} tokens")

    # Save tokenizer
    out_dir = os.path.join(base_dir, "ml", "output")
    os.makedirs(out_dir, exist_ok=True)
    tokenizer.save(os.path.join(out_dir, "vocab.json"))

    dataset = RAGDataset(triplets, tokenizer, max_len=160)
    train_size = int(0.85 * len(dataset))
    val_size = len(dataset) - train_size
    train_ds, val_ds = torch.utils.data.random_split(dataset, [train_size, val_size])

    train_loader = DataLoader(train_ds, batch_size=16, shuffle=True)
    val_loader = DataLoader(val_ds, batch_size=16, shuffle=False)

    model = KrishiMiniLM(vocab_size=vocab_size, d_model=128, nhead=4, num_layers=4, d_ff=512, max_len=160).to(device)
    param_count = sum(p.numel() for p in model.parameters())
    print(f"KrishiMiniLM Parameters: {param_count:,} ({param_count/1e6:.2f}M params)")

    criterion = nn.CrossEntropyLoss(ignore_index=-100)
    optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3, weight_decay=1e-4)
    epochs = 20

    print("Starting training on RTX 4060 GPU...")
    start_time = time.time()

    for epoch in range(1, epochs + 1):
        model.train()
        total_loss = 0
        for input_ids, labels in train_loader:
            input_ids, labels = input_ids.to(device), labels.to(device)
            optimizer.zero_grad()
            logits = model(input_ids)
            
            # Shift for autoregressive loss: logits[:-1] predicts labels[1:]
            shift_logits = logits[:, :-1, :].contiguous()
            shift_labels = labels[:, 1:].contiguous()
            loss = criterion(shift_logits.view(-1, vocab_size), shift_labels.view(-1))
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()
            total_loss += loss.item()

        avg_loss = total_loss / len(train_loader)
        
        # Validation
        model.eval()
        val_loss = 0
        with torch.no_grad():
            for input_ids, labels in val_loader:
                input_ids, labels = input_ids.to(device), labels.to(device)
                logits = model(input_ids)
                shift_logits = logits[:, :-1, :].contiguous()
                shift_labels = labels[:, 1:].contiguous()
                v_loss = criterion(shift_logits.view(-1, vocab_size), shift_labels.view(-1))
                val_loss += v_loss.item()

        avg_val_loss = val_loss / len(val_loader)
        perplexity = math.exp(min(avg_val_loss, 20.0))

        if epoch % 5 == 0 or epoch == epochs:
            print(f"Epoch [{epoch:02d}/{epochs:02d}] - Train Loss: {avg_loss:.4f} | Val Loss: {avg_val_loss:.4f} | PPL: {perplexity:.2f}")

    train_time = time.time() - start_time
    print(f"Training completed in {train_time:.2f}s!")

    # Save PyTorch Model
    pth_path = os.path.join(out_dir, "krishi_mini_llm.pth")
    torch.save(model.state_dict(), pth_path)
    print(f"Saved PyTorch weights: {pth_path}")

    # Export to ONNX
    model.eval()
    dummy_input = torch.randint(0, vocab_size, (1, 32), dtype=torch.long).to(device)
    onnx_path = os.path.join(out_dir, "krishi_mini_llm.onnx")
    
    # Export without dynamic axes issues for mobile runtime
    torch.onnx.export(
        model,
        dummy_input,
        onnx_path,
        input_names=["input_ids"],
        output_names=["logits"],
        dynamic_axes={"input_ids": {0: "batch_size", 1: "seq_len"}, "logits": {0: "batch_size", 1: "seq_len"}},
        opset_version=17
    )
    onnx_size_kb = os.path.getsize(onnx_path) / 1024
    print(f"Exported ONNX Model: {onnx_path} ({onnx_size_kb:.1f} KB)")

if __name__ == "__main__":
    train()
