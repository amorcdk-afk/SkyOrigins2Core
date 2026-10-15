# Spawn test: builds one 16x16 island (exactly chunk 64,64) in the test biome,
# plus a glass viewing platform ~35 blocks away. Run as a player.
# Island bounds: x 1024..1039, y 61..64, z 1024..1039.
fill 1024 61 1024 1039 63 1039 minecraft:dirt
fill 1024 64 1024 1039 64 1039 minecraft:grass_block
# 4x4x3 pool so guardians have water with water below
fill 1030 62 1030 1033 64 1033 minecraft:water
# fillbiome is capped at 32768 blocks, so only cover y 48..111 (16*64*16)
fillbiome 1024 48 1024 1039 111 1039 skyorigins2core:spawn_test
# Viewing platform: out of the 24-block no-spawn radius, well inside 128
fill 1028 64 1072 1034 64 1076 minecraft:glass
tp @s 1031 65 1074 180 20
function skyorigins2core:spawntest/reset
tellraw @s {"text":"[spawn test] Island built at 1024 64 1024. Run /function skyorigins2core:spawntest/start","color":"aqua"}
