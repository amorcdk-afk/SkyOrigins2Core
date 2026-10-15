# Clears counters and any mobs already on the island.
scoreboard objectives remove so2c_spawns
scoreboard objectives add so2c_spawns dummy "Island spawns"
scoreboard objectives setdisplay sidebar so2c_spawns
scoreboard players set seconds so2c_spawns 0
scoreboard players set zombie so2c_spawns 0
scoreboard players set skeleton so2c_spawns 0
scoreboard players set blaze so2c_spawns 0
scoreboard players set guardian so2c_spawns 0
scoreboard players set sheep so2c_spawns 0
scoreboard players set other so2c_spawns 0
# 1 = kill mobs once counted, so the local mob cap never fills. Set to 0 to watch them pile up.
scoreboard players set #autokill so2c_spawns 1
kill @e[type=!player,x=1020,y=56,z=1020,dx=23,dy=16,dz=23]
