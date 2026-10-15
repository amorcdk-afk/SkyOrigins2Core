"""Write an empty 3x3x3 structure template (gzipped NBT) for GameTests.

Usage: python tools/make_empty_structure.py src/main/resources/data/skyorigins2core/structures/empty_3x3.nbt
"""
import gzip
import struct
import sys

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10
DATA_VERSION_1_20_1 = 3465


def name(text):
    raw = text.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def tag(tag_type, key, payload):
    return bytes([tag_type]) + name(key) + payload


def int_list(values):
    return bytes([TAG_INT]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)


def compound_list(compounds):
    return bytes([TAG_COMPOUND]) + struct.pack(">i", len(compounds)) + b"".join(c + bytes([TAG_END]) for c in compounds)


air = tag(TAG_STRING, "Name", name("minecraft:air"))
root = (
    tag(TAG_INT, "DataVersion", struct.pack(">i", DATA_VERSION_1_20_1))
    + tag(TAG_LIST, "size", int_list([3, 3, 3]))
    + tag(TAG_LIST, "palette", compound_list([air]))
    + tag(TAG_LIST, "blocks", compound_list([]))
    + tag(TAG_LIST, "entities", compound_list([]))
)
with gzip.open(sys.argv[1], "wb") as f:
    f.write(bytes([TAG_COMPOUND]) + name("") + root + bytes([TAG_END]))
print("wrote", sys.argv[1])
