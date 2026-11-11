prompts = [
    {"id":1, "role":"system", "text":"You are a helpful assistant"},
    {"id":2, "role":"user", "text":"Summarize this article in 3 bullets."},
    {"id":3, "role":"user", "text":"Translate 'good morning' to Spanish."},
    {"id":4, "role":"assistant", "text":"Sure, I can help with that"},
    ]

for prompt in prompts:
    print(
        f"{prompt['id']}. [{prompt['role']}] {prompt['text']}"
    )