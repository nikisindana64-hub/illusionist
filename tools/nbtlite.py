"""Минимальный писатель NBT (без внешних библиотек) для файлов структур."""
import gzip, struct

class Byte(int): pass
class Long(int): pass

def _s(b, s):
    e = s.encode("utf-8"); b += struct.pack(">H", len(e)) + e

def _tag_id(v):
    if isinstance(v, bool): return 1
    if isinstance(v, Byte): return 1
    if isinstance(v, Long): return 4
    if isinstance(v, int): return 3
    if isinstance(v, str): return 8
    if isinstance(v, list): return 9
    if isinstance(v, dict): return 10
    raise TypeError(type(v))

def _payload(b, v):
    t = _tag_id(v)
    if t == 1: b += struct.pack(">b", int(v))
    elif t == 3: b += struct.pack(">i", v)
    elif t == 4: b += struct.pack(">q", v)
    elif t == 8: _s(b, v)
    elif t == 9:
        inner = _tag_id(v[0]) if v else 0
        b += struct.pack(">bi", inner, len(v))
        for x in v: _payload(b, x)
    elif t == 10:
        for k, x in v.items():
            b += struct.pack(">b", _tag_id(x)); _s(b, k); _payload(b, x)
        b += b"\x00"

def write_nbt(path, root):
    b = bytearray()
    b += struct.pack(">b", 10); _s(b, "")
    _payload(b, root)
    with gzip.GzipFile(path, "wb", mtime=0) as f:
        f.write(bytes(b))
