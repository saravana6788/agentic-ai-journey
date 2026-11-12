import sys
def summarize(path):
    try:
        with open(path,encoding="utf-8") as f:
            text = f.read()
            words = text.split(" ")
            print(f"File: {path}")
            print(f"Words Count: {len(words)}")
            print(f"Character Count: {len(text)}")
    except:
        print("Invalid file path")

if __name__ == "__main__":
    summarize(sys.argv[1] if len(sys.argv)>1 else "notes.txt")