# Runs once a second while the test is on. Counts every new mob in the island volume once.
scoreboard players add seconds so2c_spawns 1
execute as @e[type=minecraft:zombie,tag=!so2c_seen,x=1024,y=58,z=1024,dx=15,dy=12,dz=15] run scoreboard players add zombie so2c_spawns 1
execute as @e[type=minecraft:skeleton,tag=!so2c_seen,x=1024,y=58,z=1024,dx=15,dy=12,dz=15] run scoreboard players add skeleton so2c_spawns 1
execute as @e[type=minecraft:blaze,tag=!so2c_seen,x=1024,y=58,z=1024,dx=15,dy=12,dz=15] run scoreboard players add blaze so2c_spawns 1
execute as @e[type=minecraft:guardian,tag=!so2c_seen,x=1024,y=58,z=1024,dx=15,dy=12,dz=15] run scoreboard players add guardian so2c_spawns 1
execute as @e[type=minecraft:sheep,tag=!so2c_seen,x=1024,y=58,z=1024,dx=15,dy=12,dz=15] run scoreboard players add sheep so2c_spawns 1
execute as @e[type=!minecraft:player,type=!minecraft:item,type=!minecraft:experience_orb,type=!minecraft:arrow,tag=!so2c_seen,x=1024,y=58,z=1024,dx=15,dy=12,dz=15] unless entity @s[type=minecraft:zombie] unless entity @s[type=minecraft:skeleton] unless entity @s[type=minecraft:blaze] unless entity @s[type=minecraft:guardian] unless entity @s[type=minecraft:sheep] run scoreboard players add other so2c_spawns 1
tag @e[type=!minecraft:player,tag=!so2c_seen,x=1024,y=58,z=1024,dx=15,dy=12,dz=15] add so2c_seen
execute if score #autokill so2c_spawns matches 1 run kill @e[type=!minecraft:player,tag=so2c_seen,x=1024,y=58,z=1024,dx=15,dy=12,dz=15]
execute if score #autokill so2c_spawns matches 1 run kill @e[type=minecraft:item,x=1020,y=56,z=1020,dx=23,dy=16,dz=23]
execute if score #autokill so2c_spawns matches 1 run kill @e[type=minecraft:experience_orb,x=1020,y=56,z=1020,dx=23,dy=16,dz=23]
schedule function skyorigins2core:spawntest/loop 20t replace
