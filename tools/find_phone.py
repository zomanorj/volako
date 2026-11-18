#!/usr/bin/env python3
"""Cherche le port du debogage sans fil (il change a chaque redemarrage du tel).

Usage : python3 tools/find_phone.py 192.168.88.229   -> affiche IP:PORT ou rien
"""
import asyncio
import sys


async def probe(host, port, sem, found):
    async with sem:
        try:
            _, writer = await asyncio.wait_for(asyncio.open_connection(host, port), 0.5)
            writer.close()
            found.append(port)
        except Exception:
            pass


async def main(host):
    sem = asyncio.Semaphore(800)
    found = []
    # Android choisit le port dans cette plage
    await asyncio.gather(*(probe(host, p, sem, found) for p in range(30000, 50000)))
    for port in sorted(found):
        print(f"{host}:{port}")


if __name__ == "__main__":
    asyncio.run(main(sys.argv[1]))
