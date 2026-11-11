import json

data = { "name":"Saravana", "language":"Python","day":1}
print(json.dumps(data,indent=2)
)

print(r'C:\some\name')

words = ["hello","I am", "learning","python"]

for word in words:
    print(word,len(word))

for i in range(10):
    print(i,i**2)


def fib(n):
    a,b = 0,1
    while a<n:
        print(a, end=' ')
        a, b = b, a+b

    print()

fib(2000)