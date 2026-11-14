import json
import os
import sys

import requests


def load_dotenv(path=".env"):
    try:
        with open(path, encoding="utf-8") as f:
            lines = f.readlines()
    except FileNotFoundError:
        return
    for line in lines:
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def ask(prompt, model="gemini-3.5-flash-lite"):
    key = os.getenv("GEMINI_API_KEY")
    if not key:
        print("No GEMINI_API_KEY found. Create a .env file with GEMINI_API_KEY=your-key and try again.")
        return None
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
    payload = {"contents": [{"parts": [{"text": prompt}]}]}
    response = requests.post(url, params={"key": key}, json=payload, timeout=60)
    if response.status_code != 200:
        print(f"Gemini API error: {response.status_code}")
        print(response.text[:500])
        return None
    data = response.json()
    try:
        return data["candidates"][0]["content"]["parts"][0]["text"]
    except (KeyError, IndexError, TypeError):
        print("Unexpected reply shape:")
        print(json.dumps(data, indent=2)[:1000])
        return None


if __name__ == "__main__":
    load_dotenv()
    prompt = " ".join(sys.argv[1:]) or "Explain what an API is in one sentence, like I am a Java developer."
    print(f"Prompt: {prompt}")
    reply = ask(prompt)
    if reply:
        print("Reply:")
        print(reply)