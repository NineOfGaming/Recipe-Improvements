# Recipe QoL

**Recipe QoL** is a client-side Fabric mod focused on restoring missing recipe data when a server sends an empty recipe sync, while also adding recipe visibility controls and vanilla recipe-book improvements.

## Why use it?

Some servers send empty recipe sync/update packets or empty recipe-book replacements.  
When that happens, the client can end up with missing recipe book entries, incomplete displays, or broken recipe viewer data.

Recipe QoL detects that case and restores recipe data from server-selected known packs when possible, otherwise from resources already available on your client.  
It also adds optional recipe-book browsing, layout, and crafting QoL that remain useful even when fallback features are not active.

## Features

- Detects empty recipe syncs and empty recipe-book replacements and applies a client-side fallback automatically
- Rebuilds recipe-book entries and the client recipe container used by vanilla systems
- Prefers server-selected known packs when available, otherwise falls back to vanilla or selected client packs
- notifications when empty-sync fallback activates: toast, chat, log only, or off
- Server blacklist and whitelist support
- non-vanilla fallback data from server-selected known packs or selected client packs
- "Show all recipes" mode to show fallback recipes even if the server has not unlocked them
- "Show all server-known recipes" mode that only uses server-selected known packs
- fallback indicator on entries sourced from fallback data
- Ingredient lines in recipe-book and overlay recipe tooltips
- recipe variant ungrouping
- modified-recipe tabs on crafting, furnace, blast furnace, and smoker recipe books
- tab tooltips and in-book config button
- Mouse wheel recipe paging
- quick-craft shortcuts for crafting recipe-book entries
- auto-close behavior for the recipe book
- setting to hide and disable the vanilla recipe book UI
- setting to prevent the recipe-book screens from shifting the main GUI
- setting to disable recipe-book animations
- Verbose logging and ingredient-tooltip debug keys
- Config screen powered by YACL
- REI and JEI bridges to keep recipe viewers in sync with fallback and always-visible recipes

## Compatibility

- Client-side only
- Fabric
- Requires [Fabric API](https://modrinth.com/mod/fabric-api)
- Requires [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)
- [Roughly Enough Items (REI)](https://modrinth.com/mod/rei) and [Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei) are optional
- [Mod Menu](https://modrinth.com/mod/modmenu) is optional

## Configuration

All settings are available from the in-game config screen when Mod Menu is installed.

- Enable or disable fallback-based features entirely
- Choose notification mode
- Use a blacklist or whitelist for server matching
- Match servers by `host`, `host:port`, `singleplayer`, `lan`, or `realm`
- Toggle the REI bridge
- Toggle the JEI bridge
- Hide REI's incomplete recipe warning while fallback is active
- Show fallback indicators on fallback-backed recipe entries
- Hide and disable the vanilla recipe book
- Prevent the recipe book from shifting the main GUI
- Show ingredients in recipe-book tooltips
- Show recipe variants separately instead of grouped
- Show modified recipes in a dedicated tab
- Show recipe tab tooltips
- Show the config button inside the recipe book
- Scroll through recipe-book pages with the mouse wheel
- Auto-close the recipe book after inserting a recipe
- Close the recipe book state when leaving supported screens
- Disable recipe-book animations
- Enable quick-craft shortcuts
- Include or exclude non-vanilla recipes from fallback data
- Show fallback recipes even if the server has not unlocked them
- Show only server-known fallback recipes even if the server has not unlocked them
- Enable verbose logging and raw ingredient tooltip keys for debugging

## Important notes

- This mod only changes what the client can display. It does not grant recipes on the server and does not bypass server-side crafting logic.
- Recipe QoL does not know about unsynced server-side recipe changes. If the server sends a custom or modified recipe to the client, the mod leaves it alone. If it does not, the client can only show the version available from fallback data.
- Empty-sync fallback only activates when the server sends an empty recipe sync or empty recipe-book replacement and the mod is enabled for that server.
- Fallback data comes from server-selected known packs when available, otherwise from local client resources.
- "Show all recipes" also works on servers that already send recipe data. It only changes client-side visibility and does not unlock recipes on the server.
- "Show all server-known recipes" needs server-selected known packs. If none are available, it will not add extra recipes.
- If you use client-only packs or mods that add recipes not present on the server, disable non-vanilla fallback data for that server.
