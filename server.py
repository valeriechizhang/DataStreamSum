#!/opt/python-3.4/linux/bin/python3

import sys
import random
from socket import *

def help():
  s = '''
  server.py - server program for integer stream

  USAGE:
    server.py -h
    server.py <#int> <min> <max>

  OPTIONS:
    -h   get this help page
    <#int> number of integers (default is 100000)
    <min> minimum delay (default is 5000)
    <max> maximum delay (default is 10000)

  EXAMPLE:
    server.py -h
    server.py 10 1000 2000

  CONTACT:
    Ming-Hwa Wang, Ph.D. 408/805-4175  m1wan@scu.edu
  '''
  print(s)
  raise SystemExit(1)

num, min, max = (100000, 5000, 10000)
if len(sys.argv) == 2 and sys.argv[1] == "-h":
  help()
elif len(sys.argv) == 4:
  num = int(sys.argv[1])
  min = int(sys.argv[2])
  max = int(sys.argv[3])
  if num <= 0 or min <= 0:
    help()
  if min > max:
    help()
else:
  help()

s = socket(AF_INET, SOCK_STREAM)
s.bind(('', 32767))
host, port = s.getsockname()
print("connect to port number %s" % port)
s.listen(10)
while True:
  client, addr = s.accept()
  print("Got a connection from %s" % str(addr))
  random.seed(32767)
  for i in range(num):
    j = random.randint(0,65535)
    print(j)
    x = str(j)
    x = x + "\n"
    client.send(x.encode('ascii'))
    for k in range(random.randint(min,max)):
      j = j + k
client.close()